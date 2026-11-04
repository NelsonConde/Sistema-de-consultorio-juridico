package co.edu.ufps.legal_cases.security.service.auth.activation;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.common.email.EmailService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.auth.EmailTemplateService;

/**
 * Service to build and dispatch secure account activation invitation emails.
 * Complies with SCRUM-294 acceptance criteria:
 * - Does not include national ID/document or default secrets in template.
 * - Does not log raw invitation tokens or sensitive link URLs.
 * - Supports configurable frontend URL.
 * - Handles email dispatch failure safely leaving the account recoverable.
 * - Generic error handling avoiding leaking internal provider details.
 */
@Service
public class AccountActivationEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountActivationEmailService.class);

    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final String frontendActivationBaseUrl;
    private final long expirationHours;

    public AccountActivationEmailService(
            EmailService emailService,
            EmailTemplateService emailTemplateService,
            @Value("${app.frontend.activate-account-url:http://localhost:3000/activar-cuenta}") String frontendActivationBaseUrl,
            @Value("${app.activation.expiration-hours:48}") long expirationHours) {
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.frontendActivationBaseUrl = frontendActivationBaseUrl;
        this.expirationHours = expirationHours;
    }

    /**
     * Builds the full activation link from the token and dispatches the HTML email via EmailService.
     *
     * @param email Recipient email address
     * @param rawToken Secure raw activation token
     */
    public void enviarInvitacionActivacion(String email, String rawToken) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("El correo del destinatario es obligatorio para enviar la invitación");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("El token de activación es obligatorio");
        }

        String enlace = construirEnlaceActivacion(rawToken);
        String html = emailTemplateService.construirInvitacionActivacion(enlace, expirationHours);

        try {
            log.info("Despachando invitación de activación de cuenta");
            emailService.enviarHtml(
                    email.trim(),
                    null,
                    "Activación de cuenta",
                    html,
                    "activación de cuenta");
        } catch (BusinessException ex) {
            log.error("Error de negocio al despachar invitación de activación");
            throw ex;
        } catch (Exception ex) {
            log.error("Fallo al despachar invitación de activación. Tipo: {}", ex.getClass().getName());
            throw new BusinessException("No se pudo enviar el correo de activación de cuenta");
        }
    }

    public String construirEnlaceActivacion(String rawToken) {
        String baseUrl = frontendActivationBaseUrl.trim();
        String separator = baseUrl.contains("?") ? "&" : "?";
        String enlace = baseUrl + separator + "token=" + rawToken.trim();

        validarEnlaceSeguro(enlace);
        return enlace;
    }

    private void validarEnlaceSeguro(String enlace) {
        final URI uri;
        try {
            uri = URI.create(enlace);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Configuración de URL de activación inválida");
        }

        String host = uri.getHost();
        String scheme = uri.getScheme();
        boolean entornoLocal = host != null
                && (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1"));
        boolean conexionSegura = "https".equalsIgnoreCase(scheme);

        if (host == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null
                || (!conexionSegura && !(entornoLocal && "http".equalsIgnoreCase(scheme)))) {
            throw new BusinessException("Configuración de URL de activación inválida");
        }
    }

    public long getExpirationHours() {
        return expirationHours;
    }

    public String getFrontendActivationBaseUrl() {
        return frontendActivationBaseUrl;
    }
}
