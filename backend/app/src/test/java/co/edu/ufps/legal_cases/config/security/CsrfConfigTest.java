package co.edu.ufps.legal_cases.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import jakarta.servlet.http.Cookie;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CsrfConfigTest {

    @Test
    void shouldCreateSecureHttpOnlyCookieForProduction() {
        CsrfProperties properties = new CsrfProperties();
        properties.setSecure(true);
        properties.setSameSite("None");

        CsrfTokenRepository repository =
                new CsrfConfig().csrfTokenRepository(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);

        Cookie cookie = response.getCookie("XSRF-TOKEN");

        assertNotNull(token);
        assertNotNull(cookie);

        assertEquals("X-XSRF-TOKEN", token.getHeaderName());

        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("None", cookie.getAttribute("SameSite"));
    }

    @Test
    void shouldCreateLocalCookieWithLaxAndWithoutSecure() {
        CsrfProperties properties = new CsrfProperties();
        properties.setSecure(false);
        properties.setSameSite("Lax");

        CsrfTokenRepository repository =
                new CsrfConfig().csrfTokenRepository(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, response);

        Cookie cookie = response.getCookie("XSRF-TOKEN");

        assertNotNull(token);
        assertNotNull(cookie);

        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
    }

    @Test
    void shouldRejectSameSiteNoneWithoutSecure() {
        CsrfProperties properties = new CsrfProperties();
        properties.setSecure(false);
        properties.setSameSite("None");

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator = factory.getValidator();

            Set<ConstraintViolation<CsrfProperties>> violations =
                    validator.validate(properties);

            assertFalse(violations.isEmpty());

            assertTrue(violations.stream()
                    .anyMatch(violation ->
                            violation.getMessage()
                                    .contains(
                                            "SameSite=None requiere una cookie Secure")));
        }
    }
}