package co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.DatosCorreoReunionConciliacionDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.ReunionConciliacionNotificacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.TipoDestinatarioReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionRepository;
import lombok.RequiredArgsConstructor;

// Envía notificaciones de reunión y registra el resultado sin bloquear la programación.
@Service
@RequiredArgsConstructor
public class ReunionConciliacionEnvioNotificacionService {

    private static final int LONGITUD_MAXIMA_ERROR = 500;

    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ReunionConciliacionNotificacionRepository reunionConciliacionNotificacionRepository;
    private final ReunionConciliacionDestinatarioService reunionConciliacionDestinatarioService;
    private final ReunionConciliacionNotificacionEstadoService reunionConciliacionNotificacionEstadoService;
    private final ReunionConciliacionCorreoService reunionConciliacionCorreoService;

    @Transactional
    public void enviarNotificacionPendiente(ReunionConciliacionNotificacion notificacion) {
        if (notificacion == null || !Boolean.TRUE.equals(notificacion.getActiva())) {
            return;
        }

        if (notificacion.getDestinatarioEmail() == null || notificacion.getDestinatarioEmail().isBlank()) {
            marcarComoEnviada(notificacion);
            return;
        }

        DatosCorreoReunionConciliacionDTO datos = construirDatosCorreo(notificacion, null);

        try {
            reunionConciliacionCorreoService.enviar(
                    notificacion.getDestinatarioEmail(),
                    notificacion.getDestinatarioNombre(),
                    datos);

            marcarComoEnviada(notificacion);

        } catch (Exception ex) {
            boolean primerError = marcarConError(notificacion, ex);

            if (primerError && !TipoDestinatarioReunionConciliacion.ADMINISTRATIVO.equals(notificacion.getTipoDestinatario())) {
                crearYEnviarAlertasAdministrativas(notificacion, ex);
            }
        }
    }

    public void crearYEnviarAlertasAdministrativas(
            Long conciliacionId,
            String detalleError,
            MotivoNotificacionReunionConciliacion motivoBase) {

        List<ReunionConciliacionDestinatarioDTO> administrativos = reunionConciliacionDestinatarioService
                .obtenerDestinatariosAdministrativos();

        if (administrativos.isEmpty()) {
            return;
        }

        for (ReunionConciliacionDestinatarioDTO administrativo : administrativos) {
            ReunionConciliacionNotificacion alerta = reunionConciliacionNotificacionEstadoService.crear(
                    conciliacionId,
                    administrativo,
                    MotivoNotificacionReunionConciliacion.ERROR_ENVIO,
                    MomentoNotificacionReunionConciliacion.INMEDIATA,
                    LocalDateTime.now());

            enviarAlertaAdministrativa(alerta, detalleError);
        }
    }

    private DatosCorreoReunionConciliacionDTO construirDatosCorreo(
            ReunionConciliacionNotificacion notificacion,
            String detalleError) {

        ReunionConciliacion reunion = reunionConciliacionRepository
                .findByConciliacion_Id(notificacion.getConciliacion().getId())
                .orElse(null);

        Long consultaId = notificacion.getConciliacion().getConsulta() != null
                ? notificacion.getConciliacion().getConsulta().getId()
                : null;

        return new DatosCorreoReunionConciliacionDTO(
                notificacion.getConciliacion().getId(),
                consultaId,
                reunion != null ? reunion.getFechaReunion() : null,
                reunion != null && reunion.getSede() != null ? reunion.getSede().getNombre() : null,
                reunion != null ? reunion.getObservaciones() : null,
                notificacion.getMotivo(),
                notificacion.getMomentoNotificacion(),
                detalleError);
    }

    private void crearYEnviarAlertasAdministrativas(
            ReunionConciliacionNotificacion notificacionFallida,
            Exception ex) {

        crearYEnviarAlertasAdministrativas(
                notificacionFallida.getConciliacion().getId(),
                construirDetalleError(notificacionFallida, ex),
                notificacionFallida.getMotivo());
    }

    private void enviarAlertaAdministrativa(
            ReunionConciliacionNotificacion alerta,
            String detalleError) {

        DatosCorreoReunionConciliacionDTO datos = construirDatosCorreo(alerta, detalleError);

        try {
            reunionConciliacionCorreoService.enviar(
                    alerta.getDestinatarioEmail(),
                    alerta.getDestinatarioNombre(),
                    datos);

            marcarComoEnviada(alerta);

        } catch (Exception ex) {
            marcarConError(alerta, ex);
        }
    }

    private void marcarComoEnviada(ReunionConciliacionNotificacion notificacion) {
        notificacion.setEnviada(true);
        notificacion.setActiva(true);
        notificacion.setFechaEnvio(LocalDateTime.now());
        notificacion.setError(null);

        reunionConciliacionNotificacionRepository.save(notificacion);
    }

    private boolean marcarConError(ReunionConciliacionNotificacion notificacion, Exception ex) {
        Integer intentos = notificacion.getIntentos() != null ? notificacion.getIntentos() : 0;
        boolean primerError = intentos == 0;

        notificacion.setIntentos(intentos + 1);
        notificacion.setError(recortarError(ex.getMessage()));

        reunionConciliacionNotificacionRepository.save(notificacion);

        return primerError;
    }

    private String construirDetalleError(
            ReunionConciliacionNotificacion notificacion,
            Exception ex) {

        String destinatario = notificacion.getDestinatarioEmail() != null
                ? notificacion.getDestinatarioEmail()
                : "destinatario sin correo";

        return "No se pudo enviar la notificación de reunión de conciliación a "
                + destinatario
                + ". Motivo: "
                + recortarError(ex.getMessage());
    }

    private String recortarError(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Error desconocido al enviar correo";
        }

        if (mensaje.length() <= LONGITUD_MAXIMA_ERROR) {
            return mensaje;
        }

        return mensaje.substring(0, LONGITUD_MAXIMA_ERROR);
    }
}
