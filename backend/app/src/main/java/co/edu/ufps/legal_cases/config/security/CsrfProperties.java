package co.edu.ufps.legal_cases.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
// Para que spring boot traiga las propiedades de properties y las asigne a las variables con el mismo nombre de esta clase
@ConfigurationProperties(prefix = "app.security.csrf")
public class CsrfProperties {

    private boolean secure = true;

    @NotBlank
    @Pattern(
            regexp = "(?i)None|Lax|Strict",
            message = "SameSite debe ser None, Lax o Strict")
    private String sameSite = "None";

    // Validacion para que el backend no inicie sin configuracion correcta de SameSite y Secure
    @AssertTrue(message = "SameSite=None requiere una cookie Secure")
    public boolean isCookieConfigurationValid() {
        return !"None".equalsIgnoreCase(sameSite) || secure;
    }
}