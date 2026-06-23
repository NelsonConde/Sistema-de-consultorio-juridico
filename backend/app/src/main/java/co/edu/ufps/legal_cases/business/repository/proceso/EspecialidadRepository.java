package co.edu.ufps.legal_cases.business.repository.proceso;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.proceso.Especialidad;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    Optional<Especialidad> findByIdAndActivoTrue(Long id);

    List<Especialidad> findByActivoTrueOrderByNombreAsc();

    List<Especialidad> findByOrganoControlIdAndActivoTrueOrderByNombreAsc(Long organoControlId);

    boolean existsByNombreIgnoreCaseAndOrganoControlId(
            String nombre,
            Long organoControlId
    );

    boolean existsByNombreIgnoreCaseAndOrganoControlIdAndIdNot(
            String nombre,
            Long organoControlId,
            Long id
    );

    boolean existsByOrganoControlIdAndActivoTrue(Long organoControlId);
    
    List<Especialidad> findAllByOrderByNombreAsc();

    List<Especialidad> findByOrganoControlIdOrderByNombreAsc(Long organoControlId);
}