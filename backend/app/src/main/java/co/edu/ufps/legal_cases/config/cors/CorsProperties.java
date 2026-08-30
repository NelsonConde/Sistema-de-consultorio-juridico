package co.edu.ufps.legal_cases.config.cors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

// Propiedades configurables para CORS.
// Permite cambiar dominios, métodos y headers sin modificar código Java.
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    // No deja que el backend arranque sin estas configuraciones (valida)
    @NotEmpty(message = "Debe configurar al menos un origen permitido para CORS")
    private List<String> allowedOrigins = new ArrayList<>();

    @NotEmpty(message = "Debe configurar al menos un método permitido para CORS")
    private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"));

    @NotEmpty(message = "Debe configurar al menos un header permitido para CORS")
    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    @NotNull
    private Boolean allowCredentials = true;

    @NotNull
    @PositiveOrZero
    private Long maxAge = 3600L;
}