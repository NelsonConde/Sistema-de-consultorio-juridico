package co.edu.ufps.legal_cases.business.repository.seguimiento.notificacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.MomentoNotificacionSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.SeguimientoNotificacion;
import co.edu.ufps.legal_cases.business.model.seguimiento.notificacion.TipoNotificacionSeguimiento;

@Repository
public interface SeguimientoNotificacionRepository extends JpaRepository<SeguimientoNotificacion, Long> {

    Optional<SeguimientoNotificacion> findBySeguimiento_IdAndTipoNotificacionAndMomentoNotificacion(
            Long seguimientoId,
            TipoNotificacionSeguimiento tipoNotificacion,
            MomentoNotificacionSeguimiento momentoNotificacion);

    List<SeguimientoNotificacion> findBySeguimiento_Id(Long seguimientoId);

    List<SeguimientoNotificacion> findByFechaProgramadaLessThanEqualAndEnviadaFalseAndActivoTrue(LocalDate fecha);

    List<SeguimientoNotificacion> findBySeguimiento_IdAndEnviadaFalseAndActivoTrue(Long seguimientoId);

    boolean existsBySeguimiento_Consulta_IdAndSeguimiento_ActivoTrueAndActivoTrueAndEnviadaFalse(
            Long consultaId);
}