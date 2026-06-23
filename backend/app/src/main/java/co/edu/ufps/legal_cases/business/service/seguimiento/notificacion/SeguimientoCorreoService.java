package co.edu.ufps.legal_cases.business.service.seguimiento.notificacion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.MomentoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.TipoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.common.email.EmailService;
import lombok.RequiredArgsConstructor;

// Este servicio envia correos como notificacion de seguimientos
@Service
@RequiredArgsConstructor
public class SeguimientoCorreoService {

    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.app-name}")
    private String nombreSistema;

    public void enviar(
            List<SeguimientoDestinatarioDTO> destinatarios,
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion,
            String descripcion,
            String categoria,
            Long consultaId,
            LocalDate fechaEntrega) {

        if (destinatarios == null || destinatarios.isEmpty()) {
            return;
        }

        // Envia el correo a cada destinatario encontrado.
        for (SeguimientoDestinatarioDTO destinatario : destinatarios) {
            enviarADestinatario(
                    destinatario,
                    tipoNotificacion,
                    momentoNotificacion,
                    descripcion,
                    categoria,
                    consultaId,
                    fechaEntrega);
        }
    }

    private void enviarADestinatario(
            SeguimientoDestinatarioDTO destinatario,
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion,
            String descripcion,
            String categoria,
            Long consultaId,
            LocalDate fechaEntrega) {

        // El asunto cambia segun el tipo y si es inmediato o recordatorio.
        String asunto = construirAsunto(tipoNotificacion, momentoNotificacion);

        // Construye el html usando la plantilla de seguimiento.
        String html = construirHtml(
                destinatario.getNombre(),
                asunto,
                tipoNotificacion,
                momentoNotificacion,
                descripcion,
                categoria,
                consultaId,
                fechaEntrega);

        // El envio real lo hace el servicio comun de correo.
        emailService.enviarHtml(
                destinatario.getEmail(),
                destinatario.getNombre(),
                asunto,
                html,
                "seguimiento");
    }

    private String construirHtml(
            String nombreDestinatario,
            String tituloCorreo,
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion,
            String descripcion,
            String categoria,
            Long consultaId,
            LocalDate fechaEntrega) {

        Context context = new Context();

        context.setVariable("nombreSistema", nombreSistema);
        context.setVariable("nombreDestinatario", nombreDestinatario);
        context.setVariable("tituloCorreo", tituloCorreo);
        context.setVariable("mensajePrincipal", construirMensajePrincipal(tipoNotificacion, momentoNotificacion));
        context.setVariable("mensajeAdvertencia", construirMensajeAdvertencia(tipoNotificacion));
        context.setVariable("descripcion", descripcion);
        context.setVariable("categoria", categoria);
        context.setVariable("consultaId", consultaId);
        context.setVariable("fechaEntrega", fechaEntrega);

        // La ruta donde esta la plantilla de html
        return templateEngine.process("emails/notificacion-seguimiento", context);
    }

    private String construirAsunto(
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion) {

        if (MomentoNotificacionSeguimiento.RECORDATORIO.equals(momentoNotificacion)) {
            return switch (tipoNotificacion) {
                case PARTES -> "Recordatorio de seguimiento de su consulta";
                case ESTUDIANTE -> "Recordatorio de seguimiento pendiente";
                case ALERTA_DISCIPLINARIA -> "Recordatorio de alerta disciplinaria";
                case AUTOR -> "Recordatorio de seguimiento pendiente";
            };
        }

        return switch (tipoNotificacion) {
            case PARTES -> "Nuevo seguimiento de su consulta";
            case ESTUDIANTE -> "Nuevo seguimiento de la consulta";
            case ALERTA_DISCIPLINARIA -> "Alerta disciplinaria de seguimiento";
            case AUTOR -> "Nuevo seguimiento registrado";
        };
    }

    private String construirMensajePrincipal(
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion) {

        if (MomentoNotificacionSeguimiento.RECORDATORIO.equals(momentoNotificacion)) {
            return switch (tipoNotificacion) {
                case PARTES -> "Este es un recordatorio sobre un seguimiento pendiente relacionado con su consulta.";
                case ESTUDIANTE -> "Este es un recordatorio sobre un seguimiento que debes revisar en el sistema.";
                case ALERTA_DISCIPLINARIA -> "Este es un recordatorio sobre una alerta disciplinaria pendiente de revisión.";
                case AUTOR -> "Este es un recordatorio de un seguimiento que tienes pendiente.";
            };
        }

        return switch (tipoNotificacion) {
            case PARTES -> "Se ha registrado un seguimiento relacionado con su consulta.";
            case ESTUDIANTE -> "Se ha registrado un seguimiento que debes revisar en el sistema.";
            case ALERTA_DISCIPLINARIA -> "Se ha registrado un seguimiento marcado como alerta disciplinaria.";
            case AUTOR -> "Se ha registrado un seguimiento asociado a tu usuario.";
        };
    }

    private String construirMensajeAdvertencia(TipoNotificacionSeguimiento tipoNotificacion) {
        if (TipoNotificacionSeguimiento.ALERTA_DISCIPLINARIA.equals(tipoNotificacion)) {
            return "Esta alerta requiere revisión por parte del equipo administrativo.";
        }

        return null;
    }
}