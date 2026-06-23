package co.edu.ufps.legal_cases.business.repository.seguimiento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;

@Repository
public interface CategoriaSeguimientoRepository extends JpaRepository<CategoriaSeguimiento, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<CategoriaSeguimiento> findByActivoTrueOrderByNombreAsc();

    Optional<CategoriaSeguimiento> findByIdAndActivoTrue(Long id);
}