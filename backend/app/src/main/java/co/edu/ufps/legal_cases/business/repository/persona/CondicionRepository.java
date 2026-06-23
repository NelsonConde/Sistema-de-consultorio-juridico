package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.persona.Condicion;

@Repository
public interface CondicionRepository extends JpaRepository<Condicion, Long> {

    Optional<Condicion> findByIdAndActivoTrue(Long id);

    List<Condicion> findByActivoTrueOrderByNombreAsc();

    List<Condicion> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}