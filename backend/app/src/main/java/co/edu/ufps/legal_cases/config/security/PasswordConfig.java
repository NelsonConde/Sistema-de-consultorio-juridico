package co.edu.ufps.legal_cases.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Configura el encoder usado para guardar y validar contraseñas.
// BCrypt genera hashes seguros con salt interno y es el estándar recomendado en Spring Security.
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}