package co.edu.ufps.legal_cases.common.email;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import tools.jackson.databind.json.JsonMapper;

// Implementación de EmailService usando Brevo como proveedor externo.
// La lógica de negocio no debe depender directamente de esta clase.
@Service
public class BrevoEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

    private static final URI BREVO_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    private final String brevoApiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailService(
            JsonMapper jsonMapper,
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${app.mail.from-email}") String fromEmail,
            @Value("${app.mail.from-name}") String fromName) {

        this.jsonMapper = jsonMapper;
        this.brevoApiKey = brevoApiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // Envía un correo HTML.
    // El contenido del correo se construye en el módulo que lo necesita.
    @Override
    public void enviarHtml(
            String destinatario,
            String nombreDestinatario,
            String asunto,
            String html,
            String tipoCorreo) {

        validarConfiguracion();

        try {
            log.info("Enviando correo de {} a {}", tipoCorreo, mascararCorreo(destinatario));

            Map<String, Object> payload = Map.of(
                    "sender", Map.of(
                            "name", fromName,
                            "email", fromEmail),
                    "to", List.of(construirDestinatario(destinatario, nombreDestinatario)),
                    "subject", asunto,
                    "htmlContent", html);

            HttpRequest request = construirRequest(payload);

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            validarRespuesta(response, tipoCorreo);

            log.info("Correo de {} enviado correctamente a {}", tipoCorreo, mascararCorreo(destinatario));

        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Envío de correo de {} interrumpido", tipoCorreo, ex);
            throw new BusinessException("No se pudo enviar el correo de " + tipoCorreo);
        } catch (Exception ex) {
            log.error("Error enviando correo de {}. Mensaje: {}", tipoCorreo, ex.getMessage(), ex);
            throw new BusinessException("No se pudo enviar el correo de " + tipoCorreo);
        }
    }

    // Construye la petición HTTP que se envía a Brevo.
    private HttpRequest construirRequest(Map<String, Object> payload) throws Exception {
        String json = jsonMapper.writeValueAsString(payload);

        return HttpRequest.newBuilder()
                .uri(BREVO_EMAIL_URI)
                .timeout(Duration.ofSeconds(20))
                .header("api-key", brevoApiKey)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
    }

    // Construye el destinatario para el payload esperado por Brevo.
    private Map<String, String> construirDestinatario(String email, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Map.of("email", email);
        }

        return Map.of(
                "email", email,
                "name", nombre);
    }

    // Valida la respuesta del proveedor externo.
    // Si Brevo responde con error, se registra el detalle en logs y se devuelve error de negocio genérico.
    private void validarRespuesta(HttpResponse<String> response, String tipoCorreo) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }

        log.error(
                "Brevo no pudo enviar el correo de {}. Status: {}. Body: {}",
                tipoCorreo,
                response.statusCode(),
                response.body());

        throw new BusinessException("No se pudo enviar el correo de " + tipoCorreo);
    }

    // Verifica que las variables de configuración necesarias para correo existan.
    private void validarConfiguracion() {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.error("No está configurada la variable BREVO_API_KEY");
            throw new BusinessException("No se pudo enviar el correo");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            log.error("No está configurada la variable MAIL_FROM_EMAIL");
            throw new BusinessException("No se pudo enviar el correo");
        }

        if (fromName == null || fromName.isBlank()) {
            log.error("No está configurada la variable MAIL_FROM_NAME");
            throw new BusinessException("No se pudo enviar el correo");
        }
    }

    // Oculta parcialmente el correo en logs.
    // Ejemplo: nelsoncondegpt@gmail.com -> n***t@gmail.com
    private String mascararCorreo(String correo) {
        if (correo == null || correo.isBlank() || !correo.contains("@")) {
            return "correo-no-valido";
        }

        String[] partes = correo.split("@", 2);
        String nombre = partes[0];
        String dominio = partes[1];

        if (nombre.length() <= 2) {
            return "***@" + dominio;
        }

        return nombre.charAt(0) + "***" + nombre.charAt(nombre.length() - 1) + "@" + dominio;
    }
}