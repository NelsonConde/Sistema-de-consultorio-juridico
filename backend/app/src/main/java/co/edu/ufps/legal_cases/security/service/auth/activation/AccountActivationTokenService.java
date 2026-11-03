package co.edu.ufps.legal_cases.security.service.auth.activation;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.AccountActivationException;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.AccountActivationToken;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.repository.auth.AccountActivationTokenRepository;
import co.edu.ufps.legal_cases.security.service.auth.token.SecureTokenService;

@Service
@Transactional
public class AccountActivationTokenService {

    private final AccountActivationTokenRepository tokenRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final SecureTokenService secureTokenService;
    private final long expirationHours;

    public AccountActivationTokenService(
            AccountActivationTokenRepository tokenRepository,
            UsuarioSistemaRepository usuarioSistemaRepository,
            SecureTokenService secureTokenService,
            @Value("${app.activation.expiration-hours:48}")
            long expirationHours) {

        this.tokenRepository = tokenRepository;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.secureTokenService = secureTokenService;
        this.expirationHours = expirationHours;
    }

    public TokenActivacion crearTokenParaUsuario(
            UsuarioSistema usuario) {

        validarUsuarioPersistido(usuario);
        validarConfiguracionExpiracion();

        UsuarioSistema usuarioBloqueado =
                usuarioSistemaRepository
                        .findByIdForUpdate(usuario.getId())
                        .orElseThrow(() -> new BusinessException(
                                "No se pudo generar la invitación de activación"));

        validarCuentaPendiente(usuarioBloqueado);

        revocarTokensActivosInterno(usuarioBloqueado);

        String token =
                secureTokenService.generarTokenSeguro();

        String tokenHash =
                secureTokenService.generarHashToken(token);

        LocalDateTime ahora =
                LocalDateTime.now();

        LocalDateTime expiracion =
                ahora.plusHours(expirationHours);

        AccountActivationToken entidad =
                new AccountActivationToken();

        entidad.setTokenHash(tokenHash);
        entidad.setUsuarioSistema(usuarioBloqueado);
        entidad.setFechaCreacion(ahora);
        entidad.setFechaExpiracion(expiracion);
        entidad.setUsado(false);
        entidad.setFechaUso(null);
        entidad.setRevocado(false);
        entidad.setFechaRevocacion(null);

        tokenRepository.save(entidad);

        return new TokenActivacion(
                token,
                expiracion);
    }

    /*
     * Esta consulta devuelve únicamente un valor escalar.
     * No carga UsuarioSistema ni AccountActivationToken en el
     * persistence context antes de adquirir los locks.
     */
    @Transactional(readOnly = true)
    public Long obtenerUsuarioId(
            String token) {

        String hash =
                secureTokenService.generarHashToken(token);

        return tokenRepository
                .findUsuarioIdByTokenHash(hash)
                .orElseThrow(
                        AccountActivationException::invalido);
    }

    public AccountActivationToken obtenerTokenBloqueado(
            String token) {

        String hash =
                secureTokenService.generarHashToken(token);

        return tokenRepository
                .findByTokenHashForUpdate(hash)
                .orElseThrow(
                        AccountActivationException::invalido);
    }

    public void marcarComoUsado(
            AccountActivationToken token) {

        if (token == null || token.getId() == null) {
            throw AccountActivationException.invalido();
        }

        token.setUsado(true);
        token.setFechaUso(LocalDateTime.now());

        tokenRepository.save(token);
    }

    public void revocarTokensActivos(
            UsuarioSistema usuario) {

        validarUsuarioPersistido(usuario);

        UsuarioSistema usuarioBloqueado =
                usuarioSistemaRepository
                        .findByIdForUpdate(usuario.getId())
                        .orElseThrow(() -> new BusinessException(
                                "No se pudo revocar la invitación de activación"));

        revocarTokensActivosInterno(usuarioBloqueado);
    }

    private void revocarTokensActivosInterno(
            UsuarioSistema usuario) {

        List<AccountActivationToken> activos =
                tokenRepository
                        .findByUsuarioSistemaAndUsadoFalseAndRevocadoFalse(
                                usuario);

        if (activos.isEmpty()) {
            return;
        }

        LocalDateTime ahora =
                LocalDateTime.now();

        activos.forEach(token -> {
            token.setRevocado(true);
            token.setFechaRevocacion(ahora);
        });

        tokenRepository.saveAll(activos);

        /*
         * La revocación debe llegar a PostgreSQL antes de insertar
         * la nueva invitación para respetar el índice único parcial.
         */
        tokenRepository.flush();
    }

    private void validarCuentaPendiente(
            UsuarioSistema usuario) {

        if (usuario.getEstadoActivacion()
                != EstadoActivacionCuenta.PENDIENTE
                || usuario.getPasswordHash() != null) {

            throw new BusinessException(
                    "La cuenta no está pendiente de activación");
        }
    }

    private void validarUsuarioPersistido(
            UsuarioSistema usuario) {

        if (usuario == null || usuario.getId() == null) {
            throw new BusinessException(
                    "El usuario es obligatorio para gestionar la invitación de activación");
        }
    }

    private void validarConfiguracionExpiracion() {
        if (expirationHours <= 0) {
            throw new IllegalStateException(
                    "La expiración del token de activación debe ser mayor que cero");
        }
    }

    public record TokenActivacion(
            String token,
            LocalDateTime fechaExpiracion) {
    }
}