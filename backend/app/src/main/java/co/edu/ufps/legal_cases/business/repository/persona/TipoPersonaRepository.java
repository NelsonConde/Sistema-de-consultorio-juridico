package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;

@Repository
public interface TipoPersonaRepository extends JpaRepository<TipoPersona, Long> {

    Optional<TipoPersona> findByIdAndActivoTrue(Long id);

    List<TipoPersona> findByActivoTrueOrderByNombreAsc();

    List<TipoPersona> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}