package co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReunionConciliacionRecordatorioService {

    private static final long DIAS_ANTICIPACION_RECORDATORIO = 1L;

    private final ReunionConciliacionDestinatarioService reunionConciliacionDestinatarioService;
    private final ReunionConciliacionNotificacionEstadoService reunionConciliacionNotificacionEstadoService;

    @Transactional
    public void crearRecordatorios(ReunionConciliacion reunion, MotivoNotificacionReunionConciliacion motivo) {
        LocalDateTime fechaProgramada = calcularFechaProgramada(reunion);

        if (fechaProgramada == null) {
            return;
        }

        List<ReunionConciliacionDestinatarioDTO> destinatarios = reunionConciliacionDestinatarioService
                .obtenerDestinatariosPartes(reunion.getConciliacionId());

        for (ReunionConciliacionDestinatarioDTO destinatario : destinatarios) {
            reunionConciliacionNotificacionEstadoService.crear(
                    reunion.getConciliacionId(),
                    destinatario,
                    motivo,
                    MomentoNotificacionReunionConciliacion.RECORDATORIO,
                    fechaProgramada);
        }
    }

    private LocalDateTime calcularFechaProgramada(ReunionConciliacion reunion) {
        if (reunion == null || reunion.getFechaReunion() == null) {
            return null;
        }

        LocalDateTime fechaProgramada = reunion.getFechaReunion().minusDays(DIAS_ANTICIPACION_RECORDATORIO);

        // Si el recordatorio ya quedó en el pasado, se evita crear uno que saldría tarde.
        return fechaProgramada.isAfter(LocalDateTime.now()) ? fechaProgramada : null;
    }
}
