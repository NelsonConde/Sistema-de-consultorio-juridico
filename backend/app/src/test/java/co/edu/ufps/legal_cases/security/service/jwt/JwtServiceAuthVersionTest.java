package co.edu.ufps.legal_cases.security.service.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceAuthVersionTest {

    private static final String SECRET =
            "scrum-290-test-secret-key-with-at-least-32-bytes-long";

    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                SECRET,
                EXPIRATION_MS);
    }

    @Test
    void tokenVersionadoConservaUsernameYAuthVersion() {
        String token =
                jwtService.generarToken(
                        "usuario@prueba.test",
                        7L);

        assertTrue(jwtService.esTokenValido(token));

        assertEquals(
                "usuario@prueba.test",
                jwtService.obtenerUsername(token));

        assertEquals(
                7L,
                jwtService.obtenerAuthVersion(token));
    }

    @Test
    void tokenGeneradoConSobrecargaCompatibleUsaVersionCero() {
        String token =
                jwtService.generarToken(
                        "usuario@prueba.test");

        assertTrue(jwtService.esTokenValido(token));

        assertEquals(
                0L,
                jwtService.obtenerAuthVersion(token));
    }

    @Test
    void tokenLegacySinAuthVersionSeInterpretaComoVersionCero() {
        SecretKey key = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));

        Date ahora = new Date();
        Date expiracion =
                new Date(ahora.getTime() + EXPIRATION_MS);

        String legacyToken = Jwts.builder()
                .subject("legacy@prueba.test")
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(key)
                .compact();

        assertTrue(jwtService.esTokenValido(legacyToken));

        assertEquals(
                "legacy@prueba.test",
                jwtService.obtenerUsername(legacyToken));

        assertEquals(
                0L,
                jwtService.obtenerAuthVersion(legacyToken));
    }
}