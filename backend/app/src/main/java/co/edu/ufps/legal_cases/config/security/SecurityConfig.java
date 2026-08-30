package co.edu.ufps.legal_cases.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import co.edu.ufps.legal_cases.security.filter.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity // Activa la seguridad en las peticiones HTTP.
@EnableMethodSecurity // Habilita @PreAuthorize en controllers y services.
@EnableConfigurationProperties(AuthCookieProperties.class) // Habilita la inyeccion de propiedades de configuracion en AuthCookieProperties.
@AllArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/solicitar-recuperacion",
            "/api/auth/restablecer-password"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
        "/api/auth/csrf",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final CsrfTokenRepository csrfTokenRepository;      // Interfaz para almacenar y recuperar tokens CSRF de la cookie.

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Usa el CorsConfigurationSource definido en config/cors.
                .cors(Customizer.withDefaults())

                // Protege operaciones que modifican estado mediante token CSRF.
                // El token esperado se almacena en cookie y el cliente lo envía en header.
                .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository))

                // Cada petición debe autenticarse con el token.
                // No se crea ni se conserva sesión HTTP en el servidor.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Se deshabilitan mecanismos de autenticación por defecto
                // que no usa esta API.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Respuestas JSON estándar para errores 401 y 403 generados por Spring Security.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))

                // Define endpoints públicos y protegidos.
                .authorizeHttpRequests(auth -> auth
                        // Permite preflight de CORS.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos de autenticación.
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()

                        // Endpoints públicos GET (Swagger / OpenAPI).
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()

                        // Endpoints de usuario autenticado.
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/auth/cambiar-password").authenticated()

                        // Todo lo demás requiere autenticación.
                        .anyRequest().authenticated())

                // El filtro JWT valida el token antes del filtro estándar de usuario/password.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}