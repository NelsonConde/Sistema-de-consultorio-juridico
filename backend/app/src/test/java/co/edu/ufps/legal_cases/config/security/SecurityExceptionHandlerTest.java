package co.edu.ufps.legal_cases.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.json.JsonMapper;

class SecurityExceptionHandlerTest {

    private SecurityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SecurityExceptionHandler(
                JsonMapper.builder().findAndAddModules().build());
    }

    @Test
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/protegido");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        handler.commence(
                request,
                response,
                new BadCredentialsException("Sin autenticación"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());
        String body = response.getContentAsString();

        assertTrue(body.contains("\"estado\":401"));
        assertTrue(body.contains("No autenticado"));
        assertTrue(body.contains("/api/protegido"));
    }

    @Test
    void shouldReturn403WhenAuthenticatedUserHasNoPermission()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/restringido");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        handler.handle(
                request,
                response,
                new AccessDeniedException("Sin permiso"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());

        String body = response.getContentAsString();

        assertTrue(body.contains("\"estado\":403"));
        assertTrue(body.contains("No autorizado"));
        assertTrue(body.contains("/api/restringido"));
    }
}