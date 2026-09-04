package co.edu.ufps.legal_cases.business.repository.persona;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
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
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND EXISTS (
                            SELECT 1 FROM Consulta c
                            WHERE c.estado <> :estadoArchivado
                              AND c.estudiante.id = :perfilId
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND EXISTS (
                            SELECT 1 FROM Consulta c
                            LEFT JOIN c.estudiante e
                            LEFT JOIN e.asesor a
                            WHERE c.estado <> :estadoArchivado
                              AND (c.asesor.id = :perfilId OR a.id = :perfilId)
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND EXISTS (
                            SELECT 1 FROM Consulta c
                            WHERE c.estado <> :estadoArchivado
                              AND c.monitor.id = :perfilId
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'CONCILIADOR'
                        AND EXISTS (
                            SELECT 1 FROM Conciliacion conc
                            JOIN conc.consulta c
                            WHERE conc.activo = true
                              AND conc.conciliador.id = :perfilId
                              AND c.estado <> :estadoArchivado
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
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
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND EXISTS (
                            SELECT 1 FROM Consulta c
                            WHERE c.estado <> :estadoArchivado
                              AND c.estudiante.id = :perfilId
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND EXISTS (
                            SELECT 1 FROM Consulta c
                            LEFT JOIN c.estudiante e
                            LEFT JOIN e.asesor a
                            WHERE c.estado <> :estadoArchivado
                              AND (c.asesor.id = :perfilId OR a.id = :perfilId)
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND EXISTS (
                            SELECT 1 FROM Consulta c
                            WHERE c.estado <> :estadoArchivado
                              AND c.monitor.id = :perfilId
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'CONCILIADOR'
                        AND EXISTS (
                            SELECT 1 FROM Conciliacion conc
                            JOIN conc.consulta c
                            WHERE conc.activo = true
                              AND conc.conciliador.id = :perfilId
                              AND c.estado <> :estadoArchivado
                              AND (
                                    c.persona.id = p.id
                                    OR EXISTS (SELECT 1 FROM Consulta cP JOIN cP.partes parte WHERE cP.id = c.id AND parte.id = p.id)
                                    OR EXISTS (SELECT 1 FROM Consulta cC JOIN cC.contrapartes contra WHERE cC.id = c.id AND contra.id = p.id)
                              )
                        )
                    )
              )
            """)
    Page<PersonaResumenProjection> buscarResumen(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("alcanceGlobal") boolean alcanceGlobal,
            @Param("tipoPerfil") String tipoPerfil,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado,
            Pageable pageable);
}