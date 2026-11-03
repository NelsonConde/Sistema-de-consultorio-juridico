package co.edu.ufps.legal_cases.security.service.auth.activation;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.common.exception.AccountActivationException;
import co.edu.ufps.legal_cases.security.dto.auth.activation.ActivarCuentaRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.AccountActivationToken;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

@Service
public class AccountActivationService {

    private final AccountActivationTokenService tokenService;
    private final AccountActivationValidator validator;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountActivationService(
            AccountActivationTokenService tokenService,
            AccountActivationValidator validator,
            UsuarioSistemaRepository usuarioSistemaRepository,
            PasswordEncoder passwordEncoder) {

        this.tokenService = tokenService;
        this.validator = validator;
        this.usuarioSistemaRepository =
                usuarioSistemaRepository;
        this.passwordEncoder = passwordEncoder;
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
         * Esta lectura identifica el usuario. El estado definitivo
         * vuelve a comprobarse después de adquirir los locks.
         */
        AccountActivationToken tokenInicial =
                tokenService.obtenerToken(
                        dto.getToken());

        validator.validarToken(tokenInicial);

        Long usuarioId =
                tokenInicial
                        .getUsuarioSistema()
                        .getId();

        /*
         * Todas las operaciones concurrentes siguen el mismo orden:
         * primero UsuarioSistema y después AccountActivationToken.
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
         * Entre la lectura inicial y los locks, otra transacción
         * pudo consumir o revocar la invitación.
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
}