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
public class SeguimientoNotificacionInmediataService {

    private final SeguimientoNotificacionRepository seguimientoNotificacionRepository;
    private final SeguimientoEnvioNotificacionService seguimientoEnvioNotificacionService;
    private final SeguimientoNotificacionEstadoService seguimientoNotificacionEstadoService;

    @Transactional
    public void sincronizar(DatosCorreoSeguimientoDTO datos) {
        // Las inmediatas se recalculan desde las banderas actuales del seguimiento.
        sincronizarNotificacionInmediata(
                datos,
                TipoNotificacionSeguimiento.PARTES,
                Boolean.TRUE.equals(datos.getNotificarPartes()));

        sincronizarNotificacionInmediata(
                datos,
                TipoNotificacionSeguimiento.ESTUDIANTE,
                Boolean.TRUE.equals(datos.getNotificarEstudiante()));

        sincronizarNotificacionInmediata(
                datos,
                TipoNotificacionSeguimiento.ALERTA_DISCIPLINARIA,
                Boolean.TRUE.equals(datos.getAlertaDisciplinaria()));
    }

    private void sincronizarNotificacionInmediata(
            DatosCorreoSeguimientoDTO datos,
            TipoNotificacionSeguimiento tipoNotificacion,
            boolean aplica) {

        SeguimientoNotificacion notificacion = buscarNotificacion(
                datos.getSeguimientoId(),
                tipoNotificacion);

        if (!aplica) {
            seguimientoNotificacionEstadoService.desactivarSiEstaPendiente(notificacion);
            return;
        }

        if (notificacion == null) {
            notificacion = seguimientoNotificacionEstadoService.crear(
                    datos.getSeguimientoId(),
                    tipoNotificacion,
                    MomentoNotificacionSeguimiento.INMEDIATA,
                    LocalDate.now());
        } else {
            seguimientoNotificacionEstadoService.reactivarPendiente(notificacion, LocalDate.now());
        }

        // Si ya fue enviada antes, no se reenvía para evitar correos duplicados.
        if (!Boolean.TRUE.equals(notificacion.getEnviada())
                && Boolean.TRUE.equals(notificacion.getActivo())) {
            seguimientoEnvioNotificacionService.enviarNotificacionPendiente(notificacion);
        }
    }

    private SeguimientoNotificacion buscarNotificacion(
            Long seguimientoId,
            TipoNotificacionSeguimiento tipoNotificacion) {

        return seguimientoNotificacionRepository
                .findBySeguimiento_IdAndTipoNotificacionAndMomentoNotificacion(
                        seguimientoId,
                        tipoNotificacion,
                        MomentoNotificacionSeguimiento.INMEDIATA)
                .orElse(null);
    }
}