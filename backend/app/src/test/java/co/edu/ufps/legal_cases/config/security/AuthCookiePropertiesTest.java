package co.edu.ufps.legal_cases.config.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AuthCookiePropertiesTest {

    @Test
    void shouldAcceptSecureSameSiteNoneForProduction() {
        AuthCookieProperties properties =
                new AuthCookieProperties();

        properties.setSecure(true);
        properties.setSameSite("None");

        Set<ConstraintViolation<AuthCookieProperties>> violations =
                validate(properties);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptLaxWithoutSecureForLocalDevelopment() {
        AuthCookieProperties properties =
                new AuthCookieProperties();

        properties.setSecure(false);
        properties.setSameSite("Lax");

        Set<ConstraintViolation<AuthCookieProperties>> violations =
                validate(properties);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectSameSiteNoneWithoutSecure() {
        AuthCookieProperties properties =
                new AuthCookieProperties();

        properties.setSecure(false);
        properties.setSameSite("None");

        Set<ConstraintViolation<AuthCookieProperties>> violations =
                validate(properties);

        assertFalse(violations.isEmpty());

        assertTrue(
                violations.stream()
                        .anyMatch(violation ->
                                violation.getMessage()
                                        .contains(
                                                "SameSite=None requiere una cookie de autenticacion Secure")));
    }

    @Test
    void shouldRejectInvalidSameSiteValue() {
        AuthCookieProperties properties =
                new AuthCookieProperties();

        properties.setSecure(true);
        properties.setSameSite("Invalido");

        Set<ConstraintViolation<AuthCookieProperties>> violations =
                validate(properties);

        assertFalse(violations.isEmpty());
    }

    private Set<ConstraintViolation<AuthCookieProperties>> validate(
            AuthCookieProperties properties) {

        try (ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    factory.getValidator();

            return validator.validate(properties);
        }
    }
}