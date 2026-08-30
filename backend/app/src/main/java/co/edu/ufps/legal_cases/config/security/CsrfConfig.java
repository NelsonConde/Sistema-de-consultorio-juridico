package co.edu.ufps.legal_cases.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@EnableConfigurationProperties(CsrfProperties.class)
public class CsrfConfig {

    @Bean
    public CsrfTokenRepository csrfTokenRepository(CsrfProperties properties) {
        CookieCsrfTokenRepository repository =
                new CookieCsrfTokenRepository();

        repository.setCookiePath("/");

        repository.setCookieCustomizer(cookie -> cookie
                .httpOnly(true) // HTTP only porque el front no recivira el valor por la cookie sino por un endpoint especial
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite()));

        return repository;
    }
}