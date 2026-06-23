package co.edu.ufps.legal_cases.business.service.seguimiento.notificacion;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosCorreoSeguimientoDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.MomentoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.SeguimientoNotificacion;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.TipoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion.SeguimientoNotificacionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeguimientoRecordatorioService {

    private final SeguimientoNotificacionRepository seguimientoNotificacionRepository;
    private final SeguimientoNotificacionEstadoService seguimientoNotificacionEstadoService;

    @Transactional
    public void sincronizar(DatosCorreoSeguimientoDTO datos) {
        if (!tieneDatosParaRecordatorio(datos)) {
            cancelarRecordatorio(datos, TipoNotificacionSeguimiento.AUTOR);
            cancelarRecordatorio(datos, TipoNotificacionSeguimiento.PARTES);
            cancelarRecordatorio(datos, TipoNotificacionSeguimiento.ESTUDIANTE);
            cancelarRecordatorio(datos, TipoNotificacionSeguimiento.ALERTA_DISCIPLINARIA);
            return;
        }

        LocalDate fechaProgramada = datos.getFechaEntrega().minusDays(datos.getDiasNotificacion());

        // El autor siempre recibe recordatorio si hay fecha y días configurados.
        sincronizarRecordatorio(datos, TipoNotificacionSeguimiento.AUTOR, true, fechaProgramada);

        sincronizarRecordatorio(
                datos,
                TipoNotificacionSeguimiento.PARTES,
                Boolean.TRUE.equals(datos.getNotificarPartes()),
                fechaProgramada);

        sincronizarRecordatorio(
                datos,
                TipoNotificacionSeguimiento.ESTUDIANTE,
                Boolean.TRUE.equals(datos.getNotificarEstudiante()),
                fechaProgramada);

        sincronizarRecordatorio(
                datos,
                TipoNotificacionSeguimiento.ALERTA_DISCIPLINARIA,
                Boolean.TRUE.equals(datos.getAlertaDisciplinaria()),
                fechaProgramada);
    }

    private void sincronizarRecordatorio(
            DatosCorreoSeguimientoDTO datos,
            TipoNotificacionSeguimiento tipoNotificacion,
            boolean aplica,
            LocalDate fechaProgramada) {

        SeguimientoNotificacion notificacion = buscarRecordatorio(
                datos.getSeguimientoId(),
                tipoNotificacion);

        if (!aplica) {
            seguimientoNotificacionEstadoService.desactivarSiEstaPendiente(notificacion);
            return;
        }

        if (notificacion == null) {
            seguimientoNotificacionEstadoService.crear(
                    datos.getSeguimientoId(),
                    tipoNotificacion,
                    MomentoNotificacionSeguimiento.RECORDATORIO,
                    fechaProgramada);
            return;
        }

        // Si ya fue enviada, no se modifica porque representa un envío real del
        // historial.
        if (Boolean.TRUE.equals(notificacion.getEnviada())) {
            return;
        }

        seguimientoNotificacionEstadoService.reactivarPendiente(notificacion, fechaProgramada);
    }

    private void cancelarRecordatorio(
            DatosCorreoSeguimientoDTO datos,
            TipoNotificacionSeguimiento tipoNotificacion) {

        SeguimientoNotificacion notificacion = buscarRecordatorio(
                datos.getSeguimientoId(),
                tipoNotificacion);

        seguimientoNotificacionEstadoService.desactivarSiEstaPendiente(notificacion);
    }

    private SeguimientoNotificacion buscarRecordatorio(
            Long seguimientoId,
            TipoNotificacionSeguimiento tipoNotificacion) {

        return seguimientoNotificacionRepository
                .findBySeguimiento_IdAndTipoNotificacionAndMomentoNotificacion(
                        seguimientoId,
                        tipoNotificacion,
                        MomentoNotificacionSeguimiento.RECORDATORIO)
                .orElse(null);
    }

    private boolean tieneDatosParaRecordatorio(DatosCorreoSeguimientoDTO datos) {
        return datos.getFechaEntrega() != null
                && datos.getDiasNotificacion() != null;
    }
}