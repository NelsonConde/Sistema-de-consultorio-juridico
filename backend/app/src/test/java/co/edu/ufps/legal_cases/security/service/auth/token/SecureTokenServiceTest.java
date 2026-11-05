package co.edu.ufps.legal_cases.security.service.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

class SecureTokenServiceTest {

    private SecureTokenService service;

    @BeforeEach
    void setUp() {
        service = new SecureTokenService();
    }

    @Test
    void generaTokensAleatoriosDiferentes() {
        String primero =
                service.generarTokenSeguro();

        String segundo =
                service.generarTokenSeguro();

        assertNotEquals(primero, segundo);
    }

    @Test
    void hashEsDeterministaYNoExponeToken() {
        String token =
                "synthetic-secure-token";

        String hash1 =
                service.generarHashToken(token);

        String hash2 =
                service.generarHashToken(token);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
        assertNotEquals(token, hash1);
    }

    @Test
    void rechazaTokenVacio() {
        assertThrows(
                BusinessException.class,
                () -> service.generarHashToken(" "));
    }
}