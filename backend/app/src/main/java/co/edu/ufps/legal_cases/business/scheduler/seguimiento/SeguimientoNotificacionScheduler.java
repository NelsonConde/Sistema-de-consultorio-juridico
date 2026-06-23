package co.edu.ufps.legal_cases.business.scheduler.seguimiento;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoNotificacionService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeguimientoNotificacionScheduler {

    private final SeguimientoNotificacionService seguimientoNotificacionService;

    // Revisa las notificaciones pendientes y envia las que ya llegaron a su fecha programada.
    @Scheduled(cron = "${app.seguimiento.notificaciones.cron}")
    public void procesarNotificacionesPendientes() {
        seguimientoNotificacionService.procesarPendientes(LocalDate.now());
    }
}