package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;

@Repository
public interface OcupacionRepository extends JpaRepository<Ocupacion, Long> {

    Optional<Ocupacion> findByIdAndActivoTrue(Long id);

    List<Ocupacion> findByActivoTrueOrderByNombreAsc();

    List<Ocupacion> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}