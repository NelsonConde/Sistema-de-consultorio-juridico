package co.edu.ufps.legal_cases.security.service.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

/**
 * Primitiva común para tokens opacos de seguridad.
 *
 * Genera el valor que puede entregarse al cliente y calcula únicamente
 * su SHA-256 para persistencia.
 */
@Service
public class SecureTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    public String generarTokenSeguro() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public String generarHashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(
                    "El token es obligatorio");
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 no está disponible",
                    ex);
        }
    }
}