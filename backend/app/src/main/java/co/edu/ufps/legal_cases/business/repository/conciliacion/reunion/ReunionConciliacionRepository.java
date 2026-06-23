package co.edu.ufps.legal_cases.business.repository.conciliacion.reunion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;

@Repository
public interface ReunionConciliacionRepository extends JpaRepository<ReunionConciliacion, Long> {

    Optional<ReunionConciliacion> findByConciliacion_Id(Long conciliacionId);

    boolean existsByConciliacion_Id(Long conciliacionId);
}
