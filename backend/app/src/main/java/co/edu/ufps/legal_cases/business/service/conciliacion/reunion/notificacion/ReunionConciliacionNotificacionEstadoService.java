package co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion.ReunionConciliacionDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.ReunionConciliacionNotificacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReunionConciliacionNotificacionEstadoService {

    private final ConciliacionRepository conciliacionRepository;
    private final ReunionConciliacionNotificacionRepository reunionConciliacionNotificacionRepository;

    public ReunionConciliacionNotificacion crear(
            Long conciliacionId,
            ReunionConciliacionDestinatarioDTO destinatario,
            MotivoNotificacionReunionConciliacion motivo,
            MomentoNotificacionReunionConciliacion momentoNotificacion,
            LocalDateTime fechaProgramada) {

        Conciliacion conciliacion = conciliacionRepository.getReferenceById(conciliacionId);

        ReunionConciliacionNotificacion notificacion = new ReunionConciliacionNotificacion();
        notificacion.setConciliacion(conciliacion);
        notificacion.setTipoDestinatario(destinatario.getTipoDestinatario());
        notificacion.setMotivo(motivo);
        notificacion.setMomentoNotificacion(momentoNotificacion);
        notificacion.setDestinatarioEmail(destinatario.getEmail());
        notificacion.setDestinatarioNombre(destinatario.getNombre());
        notificacion.setFechaProgramada(fechaProgramada != null ? fechaProgramada : LocalDateTime.now());

        notificacion.setEnviada(false);
        notificacion.setActiva(true);
        notificacion.setIntentos(0);
        notificacion.setError(null);
        notificacion.setFechaCancelacion(null);

        return reunionConciliacionNotificacionRepository.save(notificacion);
    }

    public void desactivarSiEstaPendiente(ReunionConciliacionNotificacion notificacion) {
        if (notificacion == null) {
            return;
        }

        // Las enviadas quedan como historial real y no se modifican.
        if (Boolean.TRUE.equals(notificacion.getEnviada())) {
            return;
        }

        if (!Boolean.TRUE.equals(notificacion.getActiva())) {
            return;
        }

        notificacion.setActiva(false);
        notificacion.setFechaCancelacion(LocalDateTime.now());

        reunionConciliacionNotificacionRepository.save(notificacion);
    }
}
