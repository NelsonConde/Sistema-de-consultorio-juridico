package co.edu.ufps.legal_cases.security.service.auth.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import co.edu.ufps.legal_cases.common.email.EmailService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.auth.EmailTemplateService;

class AccountActivationEmailServiceTest {

    private EmailService emailService;
    private EmailTemplateService emailTemplateService;
    private AccountActivationEmailService activationEmailService;

    private final String baseUrl = "http://localhost:3000/activar-cuenta";
    private final long expirationHours = 48;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        emailTemplateService = mock(EmailTemplateService.class);
        activationEmailService = new AccountActivationEmailService(
                emailService,
                emailTemplateService,
                baseUrl,
                expirationHours);
    }

    @Test
    @DisplayName("Should build valid activation link with query parameter")
    void shouldBuildValidActivationLink() {
        String token = "abc-123-random-token";
        String link = activationEmailService.construirEnlaceActivacion(token);

        assertEquals("http://localhost:3000/activar-cuenta?token=abc-123-random-token", link);
    }

    @Test
    @DisplayName("Should dispatch email when email and token are valid")
    void shouldDispatchEmailWhenValid() {
        String email = "estudiante@ufps.edu.co";
        String rawToken = "secure-random-activation-token";
        String expectedHtml = "<html><body>Activar cuenta</body></html>";

        when(emailTemplateService.construirInvitacionActivacion(anyString(), eq(expirationHours)))
                .thenReturn(expectedHtml);

        activationEmailService.enviarInvitacionActivacion(email, rawToken);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailTemplateService).construirInvitacionActivacion(linkCaptor.capture(), eq(expirationHours));
        assertTrue(linkCaptor.getValue().contains("token=" + rawToken));

        verify(emailService).enviarHtml(
                eq(email),
                isNull(),
                eq("Activación de cuenta"),
                eq(expectedHtml),
                eq("activación de cuenta"));
    }

    @Test
    @DisplayName("Should throw BusinessException when email is null or blank")
    void shouldThrowExceptionWhenEmailIsBlank() {
        assertThrows(BusinessException.class, () ->
                activationEmailService.enviarInvitacionActivacion(null, "token123"));
        assertThrows(BusinessException.class, () ->
                activationEmailService.enviarInvitacionActivacion("   ", "token123"));
    }

    @Test
    @DisplayName("Should throw BusinessException when token is null or blank")
    void shouldThrowExceptionWhenTokenIsBlank() {
        assertThrows(BusinessException.class, () ->
                activationEmailService.enviarInvitacionActivacion("user@ufps.edu.co", null));
        assertThrows(BusinessException.class, () ->
                activationEmailService.enviarInvitacionActivacion("user@ufps.edu.co", "  "));
    }

    @Test
    @DisplayName("Should propagate BusinessException if email service fails so caller can handle recovery")
    void shouldPropagateBusinessExceptionOnEmailFailure() {
        String email = "test@ufps.edu.co";
        String token = "tok123";

        when(emailTemplateService.construirInvitacionActivacion(anyString(), eq(expirationHours)))
                .thenReturn("<html>html content</html>");

        doThrow(new BusinessException("Error de proveedor"))
                .when(emailService).enviarHtml(anyString(), isNull(), anyString(), anyString(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                activationEmailService.enviarInvitacionActivacion(email, token));

        assertEquals("Error de proveedor", ex.getMessage());
    }

    @Test
    @DisplayName("Should wrap unexpected exceptions in generic BusinessException")
    void shouldWrapUnexpectedExceptions() {
        String email = "test@ufps.edu.co";
        String token = "tok123";

        when(emailTemplateService.construirInvitacionActivacion(anyString(), eq(expirationHours)))
                .thenReturn("<html>html content</html>");

        doThrow(new RuntimeException("Connection timeout"))
                .when(emailService).enviarHtml(anyString(), isNull(), anyString(), anyString(), anyString());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                activationEmailService.enviarInvitacionActivacion(email, token));

        assertTrue(ex.getMessage().contains("No se pudo enviar el correo de activación"));
        assertFalse(ex.getMessage().contains("Connection timeout"));
    }
}
