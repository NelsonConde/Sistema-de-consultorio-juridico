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
@ConfigurationProperties(prefix = "app.auth.cookie")
public class AuthCookieProperties {

    private boolean secure = true;

    @NotBlank
    @Pattern(
            regexp = "(?i)None|Lax|Strict",
            message = "SameSite debe ser None, Lax o Strict")
    private String sameSite = "None";

    @AssertTrue(message = "SameSite=None requiere una cookie de autenticacion Secure")
    public boolean isCookieConfigurationValid() {
        return !"None".equalsIgnoreCase(sameSite) || secure;
    }
}