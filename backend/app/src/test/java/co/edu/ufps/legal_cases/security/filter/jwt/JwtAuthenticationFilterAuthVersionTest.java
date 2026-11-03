package co.edu.ufps.legal_cases.security.filter.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

class JwtAuthenticationFilterAuthVersionTest {

    private JwtService jwtService;
    private UsuarioSistemaRepository usuarioSistemaRepository;
    private PerfilUsuarioResolverService perfilUsuarioResolverService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtService = mock(JwtService.class);
        usuarioSistemaRepository =
                mock(UsuarioSistemaRepository.class);
        perfilUsuarioResolverService =
                mock(PerfilUsuarioResolverService.class);

        filter = new JwtAuthenticationFilter(
                jwtService,
                usuarioSistemaRepository,
                perfilUsuarioResolverService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectTokenWhenAuthVersionIsRevoked()
            throws Exception {

        String token = "jwt-version-3";
        String username = "usuario@prueba.test";

        UsuarioSistema usuario = mock(UsuarioSistema.class);
        Rol rol = mock(Rol.class);

        when(jwtService.obtenerUsername(token))
                .thenReturn(username);

        when(usuarioSistemaRepository
                .findWithRolPermisosAndPerfilByUsername(username))
                .thenReturn(Optional.of(usuario));

        when(usuario.getActivo())
                .thenReturn(true);

        when(usuario.getRol())
                .thenReturn(rol);

        when(rol.getActivo())
                .thenReturn(true);

        when(perfilUsuarioResolverService
                .tienePerfilActivo(usuario))
                .thenReturn(true);

        when(jwtService.obtenerAuthVersion(token))
                .thenReturn(3L);

        when(usuario.getAuthVersion())
                .thenReturn(4L);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie("access_token", token));

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain =
                mock(FilterChain.class);

        filter.doFilterInternal(
                request,
                response,
                filterChain);

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication());

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void shouldAuthenticateTokenWhenAuthVersionMatches()
            throws Exception {

        String token = "jwt-version-4";
        String username = "usuario@prueba.test";

        UsuarioSistema usuario = mock(UsuarioSistema.class);
        Rol rol = mock(Rol.class);

        when(jwtService.obtenerUsername(token))
                .thenReturn(username);

        when(usuarioSistemaRepository
                .findWithRolPermisosAndPerfilByUsername(username))
                .thenReturn(Optional.of(usuario));

        when(usuario.getUsername())
                .thenReturn(username);

        when(usuario.getActivo())
                .thenReturn(true);

        when(usuario.getRol())
                .thenReturn(rol);

        when(rol.getActivo())
                .thenReturn(true);

        when(rol.getPermisos())
                .thenReturn(Set.of());

        when(perfilUsuarioResolverService
                .tienePerfilActivo(usuario))
                .thenReturn(true);

        when(jwtService.obtenerAuthVersion(token))
                .thenReturn(4L);

        when(usuario.getAuthVersion())
                .thenReturn(4L);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie("access_token", token));

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain =
                mock(FilterChain.class);

        filter.doFilterInternal(
                request,
                response,
                filterChain);

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertNotNull(authentication);

        assertEquals(
                username,
                authentication.getPrincipal());

        verify(filterChain)
                .doFilter(request, response);
    }
}