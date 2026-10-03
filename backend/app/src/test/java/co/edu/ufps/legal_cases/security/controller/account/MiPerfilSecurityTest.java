package co.edu.ufps.legal_cases.security.controller.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.audit.service.log.AuditSecurityService;
import co.edu.ufps.legal_cases.config.security.SecurityExceptionHandler;
import co.edu.ufps.legal_cases.security.filter.jwt.JwtAuthenticationFilter;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

class MiPerfilSecurityTest {

    private AuditSecurityService auditSecurityService;
    private SecurityExceptionHandler securityExceptionHandler;
    private JwtService jwtService;
    private UsuarioSistemaRepository usuarioSistemaRepository;
    private PerfilUsuarioResolverService perfilUsuarioResolverService;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        auditSecurityService = mock(AuditSecurityService.class);
        securityExceptionHandler = new SecurityExceptionHandler(
                JsonMapper.builder().build(),
                auditSecurityService);

        jwtService = mock(JwtService.class);
        usuarioSistemaRepository = mock(UsuarioSistemaRepository.class);
        perfilUsuarioResolverService = mock(PerfilUsuarioResolverService.class);

        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtService,
                usuarioSistemaRepository,
                perfilUsuarioResolverService);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Petición sin sesión a endpoint protegido retorna 401 y registra auditoría de denegación")
    void peticionSinSesionRetorna401YAudita() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mi-perfil");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityExceptionHandler.commence(
                request,
                response,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Sin autenticación"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("No autenticado"));
        assertTrue(response.getContentAsString().contains("Debe iniciar sesión para acceder a este recurso"));

        verify(auditSecurityService).recordDenied(
                eq(request),
                eq("AUTHENTICATION_REQUIRED"),
                eq("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Petición con token JWT expirado limpia el contexto de seguridad y no autentica al usuario")
    void peticionConTokenExpiradoLimpiaContexto() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/mi-perfil/contacto");
        request.setCookies(new Cookie("access_token", "token-expirado"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        Header header = mock(Header.class);
        Claims claims = mock(Claims.class);
        ExpiredJwtException expiredEx = new ExpiredJwtException(header, claims, "JWT expired");

        when(jwtService.obtenerUsername("token-expirado")).thenThrow(expiredEx);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // El filtro debió limpiar el contexto de seguridad al detectar token expirado
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Al continuar la cadena hacia el exception handler
        securityExceptionHandler.commence(
                request,
                response,
                new org.springframework.security.authentication.InsufficientAuthenticationException("Token expirado"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("No autenticado"));

        verify(auditSecurityService).recordDenied(
                eq(request),
                eq("AUTHENTICATION_REQUIRED"),
                eq("UNAUTHENTICATED"));
    }
}
