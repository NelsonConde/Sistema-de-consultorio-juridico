package co.edu.ufps.legal_cases.business.service.seguimiento.notificacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.MomentoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.SeguimientoNotificacion;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.TipoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion.SeguimientoNotificacionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeguimientoNotificacionEstadoService {

    private final SeguimientoRepository seguimientoRepository;
    private final SeguimientoNotificacionRepository seguimientoNotificacionRepository;

    public SeguimientoNotificacion crear(
            Long seguimientoId,
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion,
            LocalDate fechaProgramada) {

        // Se usa referencia por id porque aquí solo necesitamos enlazar la notificación al seguimiento.
        Seguimiento seguimiento = seguimientoRepository.getReferenceById(seguimientoId);

        SeguimientoNotificacion notificacion = new SeguimientoNotificacion();
        notificacion.setSeguimiento(seguimiento);
        notificacion.setTipoNotificacion(tipoNotificacion);
        notificacion.setMomentoNotificacion(momentoNotificacion);
        notificacion.setFechaProgramada(fechaProgramada);

        // Estado inicial común para cualquier notificación nueva.
        notificacion.setEnviada(false);
        notificacion.setActivo(true);
        notificacion.setIntentos(0);
        notificacion.setError(null);
        notificacion.setFechaCancelacion(null);

        return seguimientoNotificacionRepository.save(notificacion);
    }

    public void reactivarPendiente(
            SeguimientoNotificacion notificacion,
            LocalDate fechaProgramada) {

        if (notificacion == null || Boolean.TRUE.equals(notificacion.getEnviada())) {
            return;
        }

        // Si la regla vuelve a aplicar, la pendiente se reactiva y se limpia la cancelación anterior.
        notificacion.setActivo(true);
        notificacion.setFechaProgramada(fechaProgramada);
        notificacion.setFechaCancelacion(null);
        notificacion.setError(null);

        seguimientoNotificacionRepository.save(notificacion);
    }

    public void desactivarSiEstaPendiente(SeguimientoNotificacion notificacion) {
        if (notificacion == null) {
            return;
        }

        // Las enviadas ya hacen parte del historial real y no se deben tocar.
        if (Boolean.TRUE.equals(notificacion.getEnviada())) {
            return;
        }

        if (!Boolean.TRUE.equals(notificacion.getActivo())) {
            return;
        }

        // Se cancela sin borrar para conservar trazabilidad del seguimiento.
        notificacion.setActivo(false);
        notificacion.setFechaCancelacion(LocalDateTime.now());

        seguimientoNotificacionRepository.save(notificacion);
    }
}