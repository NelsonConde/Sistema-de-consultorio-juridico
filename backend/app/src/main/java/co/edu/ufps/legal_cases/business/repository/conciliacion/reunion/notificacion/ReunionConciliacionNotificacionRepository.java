package co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.ReunionConciliacionNotificacion;

@Repository
public interface ReunionConciliacionNotificacionRepository
        extends JpaRepository<ReunionConciliacionNotificacion, Long> {

    List<ReunionConciliacionNotificacion> findByConciliacion_IdOrderByFechaCreacionDesc(Long conciliacionId);

    List<ReunionConciliacionNotificacion> findByConciliacion_IdAndEnviadaFalseAndActivaTrue(Long conciliacionId);

    List<ReunionConciliacionNotificacion> findByFechaProgramadaLessThanEqualAndEnviadaFalseAndActivaTrue(
            LocalDateTime fechaProgramada);
}
