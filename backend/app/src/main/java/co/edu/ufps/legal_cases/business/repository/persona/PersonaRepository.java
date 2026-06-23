package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.persona.Persona;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumento(String numeroDocumento);

    Optional<Persona> findByIdAndActivoTrue(Long id);

    Optional<Persona> findByNumeroDocumentoAndActivoTrue(String numeroDocumento);

    List<Persona> findByActivoTrueOrderByNombresAscApellidosAsc();

    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
}