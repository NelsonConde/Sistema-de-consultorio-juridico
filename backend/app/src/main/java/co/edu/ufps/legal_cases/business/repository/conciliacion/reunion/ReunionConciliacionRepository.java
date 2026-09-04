package co.edu.ufps.legal_cases.business.repository.conciliacion.reunion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;

@Repository
public interface ReunionConciliacionRepository extends JpaRepository<ReunionConciliacion, Long> {

    Optional<ReunionConciliacion> findByConciliacion_Id(Long conciliacionId);

    boolean existsByConciliacion_Id(Long conciliacionId);

    @Query(value = """
            SELECT r.conciliacionId AS conciliacionId,
                   r.version AS version,
                   conciliacion.version AS conciliacionVersion,
                   consulta.id AS consultaId,
                   estado.codigo AS estadoCodigo,
                   estado.nombre AS estadoNombre,
                   sede.id AS sedeId,
                   sede.nombre AS sedeNombre,
                   r.fechaReunion AS fechaReunion,
                   r.observaciones AS observaciones,
                   estudiante.id AS estudianteId,
                   estudiante.nombre AS estudianteNombre,
                   conciliador.id AS conciliadorId,
                   conciliador.nombre AS conciliadorNombre,
                   r.fechaCreacion AS fechaCreacion,
                   r.fechaActualizacion AS fechaActualizacion
            FROM ReunionConciliacion r
            JOIN r.conciliacion conciliacion
            JOIN conciliacion.consulta consulta
            JOIN consulta.persona persona
            JOIN conciliacion.estado estado
            JOIN r.sede sede
            LEFT JOIN conciliacion.estudiante estudiante
            LEFT JOIN conciliacion.conciliador conciliador
            LEFT JOIN consulta.estudiante estudianteConsulta
            LEFT JOIN consulta.asesor asesor
            LEFT JOIN consulta.monitor monitor
            WHERE conciliacion.activo = true
              AND consulta.estado <> :estadoArchivado
              AND (
                    CAST(:search AS String) IS NULL
                    OR LOWER(consulta.descripcion)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(persona.nombres)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(persona.apellidos)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(CONCAT(CONCAT(persona.nombres, ' '), persona.apellidos))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(persona.numeroDocumento)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(estado.codigo)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(estado.nombre)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(sede.nombre)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(estudiante.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(conciliador.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (CAST(:estadoCodigo AS String) IS NULL OR estado.codigo = :estadoCodigo)
              AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR r.fechaReunion >= :fechaDesde)
              AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR r.fechaReunion < :fechaHastaExclusiva)
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND asesor.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND monitor.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'CONCILIADOR'
                        AND conciliador.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND (
                            estudiante.id = :perfilId
                            OR estudianteConsulta.id = :perfilId
                        )
                    )
              )
            """, countQuery = """
            SELECT COUNT(r.conciliacionId)
            FROM ReunionConciliacion r
            JOIN r.conciliacion conciliacion
            JOIN conciliacion.consulta consulta
            JOIN consulta.persona persona
            JOIN conciliacion.estado estado
            JOIN r.sede sede
            LEFT JOIN conciliacion.estudiante estudiante
            LEFT JOIN conciliacion.conciliador conciliador
            LEFT JOIN consulta.estudiante estudianteConsulta
            LEFT JOIN consulta.asesor asesor
            LEFT JOIN consulta.monitor monitor
            WHERE conciliacion.activo = true
              AND consulta.estado <> :estadoArchivado
              AND (
                    CAST(:search AS String) IS NULL
                    OR LOWER(consulta.descripcion)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(persona.nombres)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(persona.apellidos)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(CONCAT(CONCAT(persona.nombres, ' '), persona.apellidos))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(persona.numeroDocumento)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(estado.codigo)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(estado.nombre)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(sede.nombre)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(estudiante.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(conciliador.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
              )
              AND (CAST(:estadoCodigo AS String) IS NULL OR estado.codigo = :estadoCodigo)
              AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR r.fechaReunion >= :fechaDesde)
              AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR r.fechaReunion < :fechaHastaExclusiva)
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND asesor.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND monitor.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'CONCILIADOR'
                        AND conciliador.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND (
                            estudiante.id = :perfilId
                            OR estudianteConsulta.id = :perfilId
                        )
                    )
              )
            """)
    Page<ReunionConciliacionResumenProjection> buscarResumenPaginado(
            @Param("search") String search,
            @Param("estadoCodigo") String estadoCodigo,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHastaExclusiva") LocalDateTime fechaHastaExclusiva,
            @Param("alcanceGlobal") boolean alcanceGlobal,
            @Param("tipoPerfil") String tipoPerfil,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado,
            Pageable pageable);

    @Query("""
            SELECT r.conciliacionId AS conciliacionId,
                   consulta.id AS consultaId,
                   estado.codigo AS estadoCodigo,
                   r.fechaReunion AS fechaReunion
            FROM ReunionConciliacion r
            JOIN r.conciliacion conciliacion
            JOIN conciliacion.consulta consulta
            JOIN conciliacion.estado estado
            LEFT JOIN conciliacion.estudiante estudiante
            LEFT JOIN conciliacion.conciliador conciliador
            LEFT JOIN consulta.estudiante estudianteConsulta
            LEFT JOIN consulta.asesor asesor
            LEFT JOIN consulta.monitor monitor
            WHERE r.fechaReunion >= :desde
              AND r.fechaReunion < :hastaExclusiva
              AND conciliacion.activo = true
              AND consulta.estado <> :estadoArchivado
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND asesor.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND monitor.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'CONCILIADOR'
                        AND conciliador.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND (
                            estudiante.id = :perfilId
                            OR estudianteConsulta.id = :perfilId
                        )
                    )
              )
            ORDER BY r.fechaReunion ASC, r.conciliacionId ASC
            """)
    List<ReunionAgendaProjection> buscarParaAgenda(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusiva") LocalDateTime hastaExclusiva,
            @Param("alcanceGlobal") boolean alcanceGlobal,
            @Param("tipoPerfil") String tipoPerfil,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado);
}
