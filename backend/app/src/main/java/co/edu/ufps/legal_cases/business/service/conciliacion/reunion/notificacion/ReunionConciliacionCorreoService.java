package co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.DatosCorreoReunionConciliacionDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.common.email.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReunionConciliacionCorreoService {

    private static final DateTimeFormatter FORMATO_FECHA_REUNION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.app-name}")
    private String nombreSistema;

    public void enviar(
            String destinatarioEmail,
            String destinatarioNombre,
            DatosCorreoReunionConciliacionDTO datos) {

        String asunto = construirAsunto(datos.getMotivo(), datos.getMomentoNotificacion());
        String html = construirHtml(destinatarioNombre, asunto, datos);

        emailService.enviarHtml(
                destinatarioEmail,
                destinatarioNombre,
                asunto,
                html,
                "reunion-conciliacion");
    }

    private String construirHtml(
            String nombreDestinatario,
            String tituloCorreo,
            DatosCorreoReunionConciliacionDTO datos) {

        Context context = new Context();

        context.setVariable("nombreSistema", nombreSistema);
        context.setVariable("nombreDestinatario", nombreDestinatario);
        context.setVariable("tituloCorreo", tituloCorreo);
        context.setVariable("mensajePrincipal", construirMensajePrincipal(datos.getMotivo(), datos.getMomentoNotificacion()));
        context.setVariable("mensajeAdvertencia", construirMensajeAdvertencia(datos.getMotivo()));
        context.setVariable("conciliacionId", datos.getConciliacionId());
        context.setVariable("consultaId", datos.getConsultaId());
        context.setVariable("sedeNombre", datos.getSedeNombre());
        context.setVariable("fechaReunion", formatearFechaReunion(datos));
        context.setVariable("observaciones", datos.getObservaciones());
        context.setVariable("detalleError", datos.getDetalleError());

        return templateEngine.process("emails/notificacion-reunion-conciliacion", context);
    }

    private String construirAsunto(
            MotivoNotificacionReunionConciliacion motivo,
            MomentoNotificacionReunionConciliacion momentoNotificacion) {

        if (MomentoNotificacionReunionConciliacion.RECORDATORIO.equals(momentoNotificacion)) {
            return "Recordatorio de reunión de conciliación";
        }

        if (MotivoNotificacionReunionConciliacion.REPROGRAMACION.equals(motivo)) {
            return "Reunión de conciliación reprogramada";
        }

        if (MotivoNotificacionReunionConciliacion.ERROR_ENVIO.equals(motivo)) {
            return "Alerta de notificación de reunión de conciliación";
        }

        return "Reunión de conciliación programada";
    }

    private String construirMensajePrincipal(
            MotivoNotificacionReunionConciliacion motivo,
            MomentoNotificacionReunionConciliacion momentoNotificacion) {

        if (MomentoNotificacionReunionConciliacion.RECORDATORIO.equals(momentoNotificacion)) {
            return "Este es un recordatorio de la reunión de conciliación programada.";
        }

        if (MotivoNotificacionReunionConciliacion.REPROGRAMACION.equals(motivo)) {
            return "La reunión de conciliación fue reprogramada.";
        }

        if (MotivoNotificacionReunionConciliacion.ERROR_ENVIO.equals(motivo)) {
            return "Se presentó un error al notificar una reunión de conciliación a uno o más destinatarios.";
        }

        return "Se ha programado una reunión de conciliación.";
    }

    private String construirMensajeAdvertencia(MotivoNotificacionReunionConciliacion motivo) {
        if (MotivoNotificacionReunionConciliacion.ERROR_ENVIO.equals(motivo)) {
            return "Esta alerta requiere revisión por parte del equipo administrativo.";
        }

        return null;
    }

    private String formatearFechaReunion(DatosCorreoReunionConciliacionDTO datos) {
        if (datos.getFechaReunion() == null) {
            return null;
        }

        return datos.getFechaReunion().format(FORMATO_FECHA_REUNION);
    }
}
