package co.edu.ufps.legal_cases.config.cors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

// Propiedades configurables para CORS.
// Permite cambiar dominios, métodos y headers sin modificar código Java.
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    // Se usan patrones porque allowCredentials=true no debe combinarse con "*" como origen exacto.
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:3000",
            "https://*.vercel.app",
            "https://sistema-casos-juridicos.vercel.app"));

    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"));

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private Boolean allowCredentials = true;

    private Long maxAge = 3600L;
}