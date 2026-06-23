package co.edu.ufps.legal_cases.business.service.seguimiento;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosCorreoSeguimientoDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.SeguimientoNotificacion;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion.SeguimientoNotificacionRepository;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoEnvioNotificacionService;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoNotificacionEstadoService;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoNotificacionInmediataService;
import co.edu.ufps.legal_cases.business.service.seguimiento.notificacion.SeguimientoRecordatorioService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeguimientoNotificacionService {

    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoNotificacionRepository seguimientoNotificacionRepository;
    private final SeguimientoNotificacionInmediataService seguimientoNotificacionInmediataService;
    private final SeguimientoRecordatorioService seguimientoRecordatorioService;
    private final SeguimientoEnvioNotificacionService seguimientoEnvioNotificacionService;
    private final SeguimientoNotificacionEstadoService seguimientoNotificacionEstadoService;

    @Transactional
    public void sincronizarNotificaciones(Long seguimientoId) {
        // Se consulta un DTO liviano porque para notificar no necesitamos cargar toda la entidad.
        DatosCorreoSeguimientoDTO datos = obtenerDatosCorreo(seguimientoId);

        seguimientoNotificacionInmediataService.sincronizar(datos);
        seguimientoRecordatorioService.sincronizar(datos);
    }

    @Transactional
    public void cancelarNotificacionesPendientes(Long seguimientoId) {
        if (seguimientoId == null) {
            return;
        }

        List<SeguimientoNotificacion> pendientes = seguimientoNotificacionRepository
                .findBySeguimiento_IdAndEnviadaFalseAndActivoTrue(seguimientoId);

        pendientes.forEach(seguimientoNotificacionEstadoService::desactivarSiEstaPendiente);
    }

    @Transactional
    public void procesarPendientes(LocalDate fecha) {
        // Si el scheduler no manda fecha, se procesan las vencidas hasta hoy.
        LocalDate fechaProceso = fecha != null ? fecha : LocalDate.now();

        List<SeguimientoNotificacion> notificaciones = seguimientoNotificacionRepository
                .findByFechaProgramadaLessThanEqualAndEnviadaFalseAndActivoTrue(fechaProceso);

        notificaciones.forEach(seguimientoEnvioNotificacionService::enviarNotificacionPendiente);
    }

    private DatosCorreoSeguimientoDTO obtenerDatosCorreo(Long seguimientoId) {
        if (seguimientoId == null) {
            throw new BusinessException("El id del seguimiento es obligatorio para notificar");
        }

        return seguimientoRepository.findDatosCorreoById(seguimientoId)
                .orElseThrow(() -> new BusinessException("Seguimiento no encontrado con id: " + seguimientoId));
    }
}