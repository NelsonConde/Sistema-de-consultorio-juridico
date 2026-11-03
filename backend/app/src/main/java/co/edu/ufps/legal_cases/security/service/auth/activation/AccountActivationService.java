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
}