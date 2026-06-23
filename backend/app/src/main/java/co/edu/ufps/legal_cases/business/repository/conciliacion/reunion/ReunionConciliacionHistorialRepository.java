package co.edu.ufps.legal_cases.business.repository.conciliacion.reunion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacionHistorial;

@Repository
public interface ReunionConciliacionHistorialRepository extends JpaRepository<ReunionConciliacionHistorial, Long> {

    List<ReunionConciliacionHistorial> findByConciliacion_IdOrderByFechaEventoDesc(Long conciliacionId);
}
