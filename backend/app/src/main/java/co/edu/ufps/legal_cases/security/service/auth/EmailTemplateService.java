package co.edu.ufps.legal_cases.security.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

// Construye el contenido del correo que se va a enviar
@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;    // Para procesar la plantilla thymeleaf que uso para enviar el correo de recuperacion
    // Lo obtengo de de la info que estableci en aplication.properties
    private final String nombreSistema; 
    private final long minutosExpiracion;

    public EmailTemplateService(
            TemplateEngine templateEngine,
            @Value("${app.mail.app-name}") String nombreSistema,
            @Value("${app.password-reset.expiration-minutes}") long minutosExpiracion) {
        this.templateEngine = templateEngine;
        this.nombreSistema = nombreSistema;
        this.minutosExpiracion = minutosExpiracion;
    }

    // Construye el contenido del correo de recuperacion
    // usando las veriables para incrustarlas en la plantilla thymeleaf
    public String construirRecuperacionPassword(String enlace) {
        Context context = new Context();
        context.setVariable("enlace", enlace);
        context.setVariable("nombreSistema", nombreSistema);
        context.setVariable("minutosExpiracion", minutosExpiracion);

        return templateEngine.process("emails/recuperacion-password", context);
    }
}