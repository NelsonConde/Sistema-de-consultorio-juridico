package co.edu.ufps.legal_cases.business.repository.proceso;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;

@Repository
public interface OrganoControlRepository extends JpaRepository<OrganoControl, Long> {

    Optional<OrganoControl> findByIdAndActivoTrue(Long id);

    List<OrganoControl> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<OrganoControl> findAllByOrderByNombreAsc();
}