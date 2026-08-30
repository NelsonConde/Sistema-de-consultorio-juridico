package co.edu.ufps.legal_cases.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class CsrfFilterSecurityTest {

    private CsrfFilter csrfFilter;

    @BeforeEach
    void setUp() {
        CsrfProperties properties = new CsrfProperties();
        properties.setSecure(false);
        properties.setSameSite("Lax");

        CsrfTokenRepository repository =
                new CsrfConfig().csrfTokenRepository(properties);

        csrfFilter = new CsrfFilter(repository);
    }

    @Test
    void shouldRejectPostWithoutCsrfToken()
            throws ServletException, IOException {

        CsrfSession csrfSession = createCsrfSession();

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/test");

        request.setCookies(csrfSession.cookie());

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        csrfFilter.doFilter(
                request,
                response,
                new MockFilterChain());

        assertEquals(
                HttpServletResponse.SC_FORBIDDEN,
                response.getStatus());
    }

    @Test
    void shouldRejectPostWithInvalidCsrfToken()
            throws ServletException, IOException {

        CsrfSession csrfSession = createCsrfSession();

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/test");

        request.setCookies(csrfSession.cookie());
        request.addHeader(
                csrfSession.headerName(),
                "token-invalido");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        csrfFilter.doFilter(
                request,
                response,
                new MockFilterChain());

        assertEquals(
                HttpServletResponse.SC_FORBIDDEN,
                response.getStatus());
    }

    @Test
    void shouldAllowPostWithValidCsrfToken()
            throws ServletException, IOException {

        CsrfSession csrfSession = createCsrfSession();

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/test");

        request.setCookies(csrfSession.cookie());
        request.addHeader(
                csrfSession.headerName(),
                csrfSession.token());

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse)
                        .setStatus(HttpServletResponse.SC_NO_CONTENT);

        csrfFilter.doFilter(
                request,
                response,
                filterChain);

        assertEquals(
                HttpServletResponse.SC_NO_CONTENT,
                response.getStatus());
    }

    private CsrfSession createCsrfSession()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/auth/csrf");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> tokenValue =
                new AtomicReference<>();

        AtomicReference<String> headerName =
                new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) -> {

            HttpServletRequest httpRequest =
                    (HttpServletRequest) servletRequest;

            CsrfToken csrfToken =
                    (CsrfToken) httpRequest.getAttribute(
                            CsrfToken.class.getName());

            assertNotNull(csrfToken);

            headerName.set(csrfToken.getHeaderName());

            // Fuerza la generación y persistencia del token diferido.
            tokenValue.set(csrfToken.getToken());
        };

        csrfFilter.doFilter(
                request,
                response,
                filterChain);

        Cookie cookie = response.getCookie("XSRF-TOKEN");

        assertNotNull(cookie);
        assertNotNull(tokenValue.get());
        assertNotNull(headerName.get());

        return new CsrfSession(
                cookie,
                headerName.get(),
                tokenValue.get());
    }

    private record CsrfSession(
            Cookie cookie,
            String headerName,
            String token) {
    }
}