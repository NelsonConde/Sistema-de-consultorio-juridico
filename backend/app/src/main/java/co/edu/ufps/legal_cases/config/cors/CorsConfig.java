package co.edu.ufps.legal_cases.config.cors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Configuración global de CORS para la API.
// Los valores se leen desde app.cors.* para evitar dominios quemados en código.
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        validarConfiguracion();

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.getAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Aplica la política CORS a todos los endpoints de la API.
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private void validarConfiguracion() {
        if (corsProperties.getAllowedOriginPatterns() == null
                || corsProperties.getAllowedOriginPatterns().isEmpty()) {
            throw new IllegalStateException("Debe configurar al menos un origen permitido para CORS");
        }

        if (corsProperties.getAllowedMethods() == null
                || corsProperties.getAllowedMethods().isEmpty()) {
            throw new IllegalStateException("Debe configurar al menos un método permitido para CORS");
        }

        if (corsProperties.getAllowedHeaders() == null
                || corsProperties.getAllowedHeaders().isEmpty()) {
            throw new IllegalStateException("Debe configurar al menos un header permitido para CORS");
        }
    }
}