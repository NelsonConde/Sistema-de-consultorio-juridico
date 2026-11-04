package co.edu.ufps.legal_cases.security.service.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        emailTemplateService = new EmailTemplateService(
                engine,
                "Consultorio Jurídico UFPS",
                15);
    }

    @Test
    @DisplayName("Should build password reset email HTML")
    void shouldBuildPasswordResetEmail() {
        String link = "https://consultorio.ufps.edu.co/restablecer-password?token=reset-123";
        String html = emailTemplateService.construirRecuperacionPassword(link);

        assertNotNull(html);
        assertTrue(html.contains("Consultorio Jurídico UFPS"));
        assertTrue(html.contains(link));
        assertTrue(html.contains("15"));
    }

    @Test
    @DisplayName("Should build activation invitation email HTML with link and expiration hours")
    void shouldBuildActivationInvitationEmail() {
        String link = "https://consultorio.ufps.edu.co/activar-cuenta?token=act-456";
        long expirationHours = 48;
        String html = emailTemplateService.construirInvitacionActivacion(link, expirationHours);

        assertNotNull(html);
        assertTrue(html.contains("Consultorio Jurídico UFPS"));
        assertTrue(html.contains(link));
        assertTrue(html.contains("48"));
        assertTrue(html.contains("Activar cuenta"));

        // Verify security criteria: no national document/ID or default password mentioned
        assertFalse(html.toLowerCase().contains("documento"));
        assertFalse(html.toLowerCase().contains("cedula"));
        assertFalse(html.toLowerCase().contains("cédula"));
        assertFalse(html.toLowerCase().contains("passwordinicial"));
        assertFalse(html.toLowerCase().contains("contraseña por defecto"));
    }
}
