package co.edu.ufps.legal_cases.security.filter.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro JWT ejecutado una vez por petición.
// Lee el token desde la cookie de autenticación y, si es válido,
// carga el usuario y sus permisos activos en el contexto de Spring Security.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtService jwtService;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PerfilUsuarioResolverService perfilUsuarioResolverService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioSistemaRepository usuarioSistemaRepository,
            PerfilUsuarioResolverService perfilUsuarioResolverService) {
        this.jwtService = jwtService;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.perfilUsuarioResolverService = perfilUsuarioResolverService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = obtenerTokenDesdeCookie(request);

        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            autenticarDesdeToken(token, request);
        } catch (Exception ex) {
            // Si el token es inválido o el usuario ya no puede autenticarse,
            // se limpia el contexto y la cadena continúa sin autenticación.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarDesdeToken(
            String token,
            HttpServletRequest request) {

        String username = jwtService.obtenerUsername(token);

        UsuarioSistema usuario = usuarioSistemaRepository
                .findWithRolPermisosAndPerfilByUsername(username)
                .orElse(null);

        if (usuario == null || !usuarioPuedeAutenticarse(usuario)) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario.getUsername(),
                        null,
                        obtenerAuthorities(usuario));

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String obtenerTokenDesdeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private boolean usuarioPuedeAutenticarse(UsuarioSistema usuario) {
        return usuarioActivo(usuario)
                && rolActivo(usuario)
                && perfilActivo(usuario);
    }

    private boolean usuarioActivo(UsuarioSistema usuario) {
        return Boolean.TRUE.equals(usuario.getActivo());
    }

    private boolean rolActivo(UsuarioSistema usuario) {
        return usuario.getRol() != null
                && Boolean.TRUE.equals(usuario.getRol().getActivo());
    }

    private boolean perfilActivo(UsuarioSistema usuario) {
        return perfilUsuarioResolverService.tienePerfilActivo(usuario);
    }

    private List<SimpleGrantedAuthority> obtenerAuthorities(UsuarioSistema usuario) {
        return usuario.getRol().getPermisos()
                .stream()
                .filter(permiso -> Boolean.TRUE.equals(permiso.getActivo()))
                .map(Permiso::getNombre)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}