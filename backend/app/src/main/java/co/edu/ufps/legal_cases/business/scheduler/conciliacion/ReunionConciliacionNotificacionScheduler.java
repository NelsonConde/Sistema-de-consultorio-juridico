package co.edu.ufps.legal_cases.business.scheduler.conciliacion;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.notificacion.ReunionConciliacionNotificacionService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReunionConciliacionNotificacionScheduler {

    private final ReunionConciliacionNotificacionService reunionConciliacionNotificacionService;

    // Procesa recordatorios y reintentos pendientes sin depender del flujo principal de programación.
    @Scheduled(cron = "${app.conciliacion.reunion.notificaciones.cron:0 0 * * * *}")
    public void procesarNotificacionesPendientes() {
        reunionConciliacionNotificacionService.procesarPendientes(LocalDateTime.now());
    }
}
