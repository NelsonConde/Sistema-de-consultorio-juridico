package co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.ReunionConciliacionNotificacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReunionConciliacionNotificacionService {

    private final ReunionConciliacionNotificacionRepository reunionConciliacionNotificacionRepository;
    private final ReunionConciliacionDestinatarioService reunionConciliacionDestinatarioService;
    private final ReunionConciliacionNotificacionEstadoService reunionConciliacionNotificacionEstadoService;
    private final ReunionConciliacionEnvioNotificacionService reunionConciliacionEnvioNotificacionService;
    private final ReunionConciliacionRecordatorioService reunionConciliacionRecordatorioService;

    @Transactional
    public void registrarProgramacion(ReunionConciliacion reunion) {
        crearNotificaciones(reunion, MotivoNotificacionReunionConciliacion.PROGRAMACION, false);
    }

    @Transactional
    public void registrarReprogramacion(ReunionConciliacion reunion) {
        crearNotificaciones(reunion, MotivoNotificacionReunionConciliacion.REPROGRAMACION, true);
    }

    @Transactional
    public void procesarPendientes(LocalDateTime fecha) {
        LocalDateTime fechaProceso = fecha != null ? fecha : LocalDateTime.now();

        List<ReunionConciliacionNotificacion> pendientes = reunionConciliacionNotificacionRepository
                .findByFechaProgramadaLessThanEqualAndEnviadaFalseAndActivaTrue(fechaProceso);

        pendientes.forEach(reunionConciliacionEnvioNotificacionService::enviarNotificacionPendiente);
    }

    @Transactional
    public void cancelarPendientesPorConciliacion(Long conciliacionId) {
        if (conciliacionId == null) {
            return;
        }

        List<ReunionConciliacionNotificacion> pendientes = reunionConciliacionNotificacionRepository
                .findByConciliacion_IdAndEnviadaFalseAndActivaTrue(conciliacionId);

        pendientes.forEach(reunionConciliacionNotificacionEstadoService::desactivarSiEstaPendiente);
    }

    private void crearNotificaciones(
            ReunionConciliacion reunion,
            MotivoNotificacionReunionConciliacion motivo,
            boolean cancelarPendientesPrevias) {

        if (reunion == null || reunion.getConciliacionId() == null) {
            return;
        }

        if (cancelarPendientesPrevias) {
            cancelarPendientesPorConciliacion(reunion.getConciliacionId());
        }

        List<ReunionConciliacionNotificacion> inmediatas = crearNotificacionesInmediatas(reunion, motivo);

        if (inmediatas.isEmpty()) {
            reunionConciliacionEnvioNotificacionService.crearYEnviarAlertasAdministrativas(
                    reunion.getConciliacionId(),
                    "No se encontraron destinatarios con correo para notificar la reunión de conciliación.",
                    motivo);
            return;
        }

        reunionConciliacionRecordatorioService.crearRecordatorios(reunion, motivo);
        inmediatas.forEach(reunionConciliacionEnvioNotificacionService::enviarNotificacionPendiente);
    }

    private List<ReunionConciliacionNotificacion> crearNotificacionesInmediatas(
            ReunionConciliacion reunion,
            MotivoNotificacionReunionConciliacion motivo) {

        List<ReunionConciliacionDestinatarioDTO> destinatarios = reunionConciliacionDestinatarioService
                .obtenerDestinatariosPartes(reunion.getConciliacionId());

        return destinatarios.stream()
                .map(destinatario -> reunionConciliacionNotificacionEstadoService.crear(
                        reunion.getConciliacionId(),
                        destinatario,
                        motivo,
                        MomentoNotificacionReunionConciliacion.INMEDIATA,
                        LocalDateTime.now()))
                .toList();
    }
}