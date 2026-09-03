package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.persona.Persona;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumento(String numeroDocumento);

    Optional<Persona> findByIdAndActivoTrue(Long id);

    Optional<Persona> findByNumeroDocumentoAndActivoTrue(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);

    @Query(value = """
            SELECT p.id AS id,
                   p.nombres AS nombres,
                   p.apellidos AS apellidos,
                   p.tipoDocumento AS tipoDocumento,
                   p.numeroDocumento AS numeroDocumento,
                   tipoPersona.nombre AS tipoPersona,
                   p.activo AS activo
            FROM Persona p
            JOIN p.tipoPersona tipoPersona
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (
                    CAST(:search AS String) IS NULL
                    OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(CONCAT(CONCAT(p.nombres, ' '), p.apellidos))
                        LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
            """, countQuery = """
            SELECT COUNT(p.id)
            FROM Persona p
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (
                    CAST(:search AS String) IS NULL
                    OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(CONCAT(CONCAT(p.nombres, ' '), p.apellidos))
                        LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
            """)
    Page<PersonaResumenProjection> buscarResumen(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            Pageable pageable);
}