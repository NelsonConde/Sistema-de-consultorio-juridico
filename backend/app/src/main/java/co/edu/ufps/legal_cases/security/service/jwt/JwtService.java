package co.edu.ufps.legal_cases.security.service.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;

    public JwtService(
            // Spring va y busca en application.properties el valor y lo inyecta
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String generarToken(String username) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClave())
                .compact();
    }

    public boolean esTokenValido(String token) {
        try {
            obtenerUsername(token);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    // Para identificar de quien es el token
    public String obtenerUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(obtenerClave()) // Le dice qué clave usar para validar la firma
                    .build() // Construye el lector ya configurado
                    .parseSignedClaims(token) // Lee y valida el token
                    .getPayload() // Obtiene los datos internos del token
                    .getSubject(); // Saca el username guardado en el token
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException("Token inválido o expirado");
        }
    }

    private SecretKey obtenerClave() {
        // Convierte la clave secreta en bytes y luego en una llave para firmar el token
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}