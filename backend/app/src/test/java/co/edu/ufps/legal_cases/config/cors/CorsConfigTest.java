package co.edu.ufps.legal_cases.config.cors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String UNAUTHORIZED_ORIGIN = "https://pagina-falsa.vercel.app";

    private CorsConfiguration corsConfiguration;

    @BeforeEach
    void setUp() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(ALLOWED_ORIGIN));

        CorsConfig corsConfig = new CorsConfig(properties);
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        MockHttpServletRequest request =
                new MockHttpServletRequest("OPTIONS", "/api/auth/login");

        corsConfiguration = source.getCorsConfiguration(request);

        assertNotNull(corsConfiguration);
    }

    @Test
    void shouldAllowConfiguredOrigin() {
        String result = corsConfiguration.checkOrigin(ALLOWED_ORIGIN);

        assertEquals(ALLOWED_ORIGIN, result);
    }

    @Test
    void shouldRejectUnconfiguredOrigin() {
        String result = corsConfiguration.checkOrigin(UNAUTHORIZED_ORIGIN);

        assertNull(result);
    }

    @Test
    void shouldAllowCredentials() {
        assertTrue(Boolean.TRUE.equals(corsConfiguration.getAllowCredentials()));
    }
}