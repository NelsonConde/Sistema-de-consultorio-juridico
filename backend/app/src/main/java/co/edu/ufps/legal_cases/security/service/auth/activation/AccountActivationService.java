package co.edu.ufps.legal_cases.security.service.auth.activation;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.common.exception.AccountActivationException;
import co.edu.ufps.legal_cases.security.dto.auth.AuthMessageResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.activation.ActivarCuentaRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.AccountActivationToken;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

@Service
public class AccountActivationService {

    private static final Logger log = LoggerFactory.getLogger(AccountActivationService.class);

    private static final String MENSAJE_GENERICO_REENVIO =
            "Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación";

    private static final String MENSAJE_GENERICO_REVOCACION =
            "Si la cuenta existe y tenía invitaciones activas, han sido revocadas";

    private static final Duration COOLDOWN_REENVIO = Duration.ofSeconds(60);

    private final AccountActivationTokenService tokenService;
    private final AccountActivationValidator validator;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationEmailService emailService;

    public AccountActivationService(
            AccountActivationTokenService tokenService,
            AccountActivationValidator validator,
            UsuarioSistemaRepository usuarioSistemaRepository,
            PasswordEncoder passwordEncoder,
            AccountActivationEmailService emailService) {

        this.tokenService = tokenService;
        this.validator = validator;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    @Auditable(
            action = "ACTIVAR_CUENTA",
            entityName = "UsuarioSistema",
            entityId = "#result",
            trackedFields = {"estadoActivacion"})
    public Long activarCuenta(
            ActivarCuentaRequestDTO dto) {

        validator.validarSolicitud(dto);

        /*
         * Solo resolvemos el identificador antes del lock.
         * No se carga ninguna entidad en el persistence context,
         * evitando reutilizar estado obsoleto durante concurrencia.
         */
        Long usuarioId =
                tokenService.obtenerUsuarioId(
                        dto.getToken());

        /*
         * El orden global de locks es:
         * UsuarioSistema -> AccountActivationToken.
         *
         * Una segunda activación concurrente esperará aquí hasta
         * que la primera transacción confirme sus cambios.
         */
        UsuarioSistema usuario =
                usuarioSistemaRepository
                        .findByIdForUpdate(usuarioId)
                        .orElseThrow(
                                AccountActivationException::invalido);

        AccountActivationToken tokenBloqueado =
                tokenService.obtenerTokenBloqueado(
                        dto.getToken());

        if (tokenBloqueado.getUsuarioSistema() == null
                || !usuarioId.equals(
                tokenBloqueado
                        .getUsuarioSistema()
                        .getId())) {

            throw AccountActivationException.invalido();
        }

        /*
         * Estas validaciones ocurren después de los locks.
         * Por tanto observan el estado confirmado por cualquier
         * transacción concurrente anterior.
         */
        validator.validarToken(tokenBloqueado);
        validator.validarCuentaPendiente(usuario);

        usuario.setPasswordHash(
                passwordEncoder.encode(
                        dto.getPasswordNueva()));

        usuario.setEstadoActivacion(
                EstadoActivacionCuenta.ACTIVA);

        /*
         * Defensa adicional ante cualquier credencial emitida
         * accidentalmente antes de completar la activación.
         */
        usuario.setAuthVersion(
                usuario.getAuthVersion() + 1L);

        usuarioSistemaRepository.save(usuario);

        tokenService.marcarComoUsado(
                tokenBloqueado);

        return usuario.getId();
    }

    /**
     * Resends an activation invitation for a pending account.
     * Complies with SCRUM-294 acceptance criteria:
     * - Invalidate previous active tokens for this user.
     * - Rate-limiting / controlled retry with cooldown.
     * - Generic non-enumerating response message.
     * - Safe error handling: failure to send email leaves the account recoverable.
     * - Auditing without logging raw tokens.
     */
    @Transactional
    @Auditable(
            action = "REENVIAR_INVITACION_ACTIVACION",
            entityName = "UsuarioSistema",
            entityId = "#usuarioId")
    public AuthMessageResponseDTO reenviarInvitacion(Long usuarioId) {
        if (usuarioId == null) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REENVIO);
        }

        UsuarioSistema usuario = usuarioSistemaRepository
                .findByIdForUpdate(usuarioId)
                .orElse(null);

        if (usuario == null || usuario.getEstadoActivacion() != EstadoActivacionCuenta.PENDIENTE || !Boolean.TRUE.equals(usuario.getActivo())) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REENVIO);
        }

        /*
         * Se comprueba tras adquirir el lock del usuario. El timestamp persistido
         * en PostgreSQL permite aplicar el cooldown también entre réplicas.
         */
        LocalDateTime fechaLimite = LocalDateTime.now().minus(COOLDOWN_REENVIO);
        if (tokenService.existeTokenReciente(usuario, fechaLimite)) {
            log.info("Reenvío de invitación bloqueado por cooldown para usuarioId={}", usuarioId);
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REENVIO);
        }

        AccountActivationTokenService.TokenActivacion tokenActivacion =
                tokenService.crearTokenParaUsuario(usuario);

        try {
            emailService.enviarInvitacionActivacion(usuario.getUsername(), tokenActivacion.token());
            log.info("Invitación de activación despachada correctamente para usuarioId={}", usuarioId);
        } catch (Exception ex) {
            log.error("Fallo controlado al enviar correo de invitación para usuarioId={}. Cuenta permanece pendiente y recuperable.", usuarioId);
        }

        return new AuthMessageResponseDTO(MENSAJE_GENERICO_REENVIO);
    }

    /**
     * Revokes active activation invitations for an account.
     * Complies with SCRUM-294 acceptance criteria:
     * - Revoke any existing active token.
     * - Generic non-enumerating response message.
     * - Audit without logging tokens.
     */
    @Transactional
    @Auditable(
            action = "REVOCAR_INVITACION_ACTIVACION",
            entityName = "UsuarioSistema",
            entityId = "#usuarioId")
    public AuthMessageResponseDTO revocarInvitacion(Long usuarioId) {
        if (usuarioId == null) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REVOCACION);
        }

        UsuarioSistema usuario = usuarioSistemaRepository
                .findByIdForUpdate(usuarioId)
                .orElse(null);

        if (usuario != null && usuario.getEstadoActivacion() == EstadoActivacionCuenta.PENDIENTE) {
            tokenService.revocarTokensActivos(usuario);
            log.info("Invitaciones de activación revocadas correctamente para usuarioId={}", usuarioId);
        }

        return new AuthMessageResponseDTO(MENSAJE_GENERICO_REVOCACION);
    }

    @Transactional
    public AuthMessageResponseDTO reenviarInvitacionPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REENVIO);
        }

        String username = normalizarEmail(email);
        UsuarioSistema usuario = usuarioSistemaRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (usuario == null) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REENVIO);
        }

        return reenviarInvitacion(usuario.getId());
    }

    @Transactional
    public AuthMessageResponseDTO revocarInvitacionPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REVOCACION);
        }

        String username = normalizarEmail(email);
        UsuarioSistema usuario = usuarioSistemaRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (usuario == null) {
            return new AuthMessageResponseDTO(MENSAJE_GENERICO_REVOCACION);
        }

        return revocarInvitacion(usuario.getId());
    }
}
