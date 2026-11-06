package co.edu.ufps.legal_cases.security.service.auth.password;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.PasswordResetToken;
import co.edu.ufps.legal_cases.security.repository.auth.PasswordResetTokenRepository;
import co.edu.ufps.legal_cases.security.service.auth.token.SecureTokenService;

// Maneja operaciones técnicas del token de recuperación:
// generación segura, hash, expiración, invalidación y construcción del enlace.
@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureTokenService secureTokenService;
    private final String resetPasswordUrl;
    private final long expirationMinutes;

    public PasswordResetTokenService(
            PasswordResetTokenRepository passwordResetTokenRepository,
            SecureTokenService secureTokenService,
            @Value("${app.frontend.reset-password-url}") String resetPasswordUrl,
            @Value("${app.password-reset.expiration-minutes}") long expirationMinutes) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.secureTokenService = secureTokenService;
        this.resetPasswordUrl = resetPasswordUrl;
        this.expirationMinutes = expirationMinutes;
    }

    // Crea un nuevo token de recuperación para el usuario.
    // No guarda el token real, solo su hash SHA-256.
    public TokenRecuperacion crearTokenParaUsuario(UsuarioSistema usuario) {
        invalidarTokensAnteriores(usuario);

        String token = secureTokenService.generarTokenSeguro();
        String tokenHash = secureTokenService.generarHashToken(token);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(tokenHash);
        resetToken.setUsuarioSistema(usuario);
        resetToken.setFechaCreacion(LocalDateTime.now());

        // La expiración viene desde application.properties.
        resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(expirationMinutes));
        resetToken.setUsado(false);

        passwordResetTokenRepository.save(resetToken);

        return new TokenRecuperacion(token, construirEnlace(token));
    }

    // Busca un token activo a partir del token real enviado por el frontend.
    public PasswordResetToken obtenerTokenActivo(String token) {
        String tokenHash = secureTokenService.generarHashToken(token);

        return passwordResetTokenRepository
                .findByTokenHashAndUsadoFalse(tokenHash)
                .orElseThrow(() -> new BusinessException("Token inválido o expirado"));
    }

    // Si el token ya expiró, se marca como usado para que no vuelva a intentarse usar.
    public void validarNoExpirado(PasswordResetToken resetToken) {
        if (!resetToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return;
        }

        marcarComoUsado(resetToken);

        throw new BusinessException("Token inválido o expirado");
    }

    // Marca el token como usado para que no pueda reutilizarse.
    public void marcarComoUsado(PasswordResetToken resetToken) {
        resetToken.setUsado(true);
        resetToken.setFechaUso(LocalDateTime.now());

        passwordResetTokenRepository.save(resetToken);
    }

    // Marca como usados los tokens anteriores del usuario.
    private void invalidarTokensAnteriores(UsuarioSistema usuario) {
        List<PasswordResetToken> tokensActivos = passwordResetTokenRepository
                .findByUsuarioSistemaAndUsadoFalse(usuario);

        LocalDateTime ahora = LocalDateTime.now();

        tokensActivos.forEach(token -> {
            token.setUsado(true);
            token.setFechaUso(ahora);
        });

        passwordResetTokenRepository.saveAll(tokensActivos);
    }

    // Construye el enlace que se enviará al correo del usuario.
    private String construirEnlace(String token) {
        String tokenCodificado = URLEncoder.encode(token, StandardCharsets.UTF_8);

        return resetPasswordUrl + "?token=" + tokenCodificado;
    }

    // Resultado interno al crear token.
    // Incluye el token real para enviarlo al frontend por enlace, y el enlace ya construido.
    public record TokenRecuperacion(String token, String enlace) {
    }
}