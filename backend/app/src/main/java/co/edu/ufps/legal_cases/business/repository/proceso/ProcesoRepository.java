package co.edu.ufps.legal_cases.business.repository.proceso;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;

@Repository
public interface ProcesoRepository extends JpaRepository<Proceso, Long> {

    Optional<Proceso> findByIdAndActivoTrue(Long id);

    Optional<Proceso> findByIdAndActivoTrueAndConsulta_EstadoNot(
            Long id,
            EstadoConsulta estado);

    List<Proceso> findByActivoTrueOrderByIdDesc();

    List<Proceso> findByActivoTrueAndConsulta_EstadoNotOrderByIdDesc(
            EstadoConsulta estado);

    boolean existsByNumeroRadicado(String numeroRadicado);

    boolean existsByConsulta_IdAndActivoTrue(Long consultaId);

    boolean existsByNumeroRadicadoAndIdNot(String numeroRadicado, Long id);

    boolean existsByConsulta_IdAndActivoTrueAndEstado(Long consultaId, EstadoProceso estado);

    @Query(value = """
            SELECT p.id AS id,
                   p.version AS version,
                   p.numeroRadicado AS numeroRadicado,
                   departamento.id AS departamentoId,
                   departamento.nombre AS departamentoNombre,
                   consulta.id AS consultaId,
                   consulta.descripcion AS consulta,
                   organoControl.id AS organoControlId,
                   organoControl.nombre AS organoControlNombre,
                   especialidad.id AS especialidadId,
                   especialidad.nombre AS especialidadNombre,
                   p.estado AS estado,
                   p.activo AS activo,
                   p.fechaCreacion AS fechaCreacion
            FROM Proceso p
            JOIN p.departamento departamento
            JOIN p.consulta consulta
            JOIN consulta.persona persona
            LEFT JOIN p.organoControl organoControl
            LEFT JOIN p.especialidad especialidad
            LEFT JOIN consulta.estudiante estudiante
            LEFT JOIN estudiante.asesor asesorEstudiante
            WHERE p.activo = true
              AND consulta.estado <> :estadoArchivado
              AND (
                    CAST(:search AS String) IS NULL
                    OR LOWER(COALESCE(p.numeroRadicado, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(departamento.nombre)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(organoControl.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(especialidad.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
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
              )
              AND (:estado IS NULL OR p.estado = :estado)
              AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR p.fechaCreacion >= :fechaDesde)
              AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR p.fechaCreacion < :fechaHastaExclusiva)
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND consulta.estudiante.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND (
                            consulta.asesor.id = :perfilId
                            OR asesorEstudiante.id = :perfilId
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND consulta.monitor.id = :perfilId
                    )
              )
            """, countQuery = """
            SELECT COUNT(p.id)
            FROM Proceso p
            JOIN p.departamento departamento
            JOIN p.consulta consulta
            JOIN consulta.persona persona
            LEFT JOIN p.organoControl organoControl
            LEFT JOIN p.especialidad especialidad
            LEFT JOIN consulta.estudiante estudiante
            LEFT JOIN estudiante.asesor asesorEstudiante
            WHERE p.activo = true
              AND consulta.estado <> :estadoArchivado
              AND (
                    CAST(:search AS String) IS NULL
                    OR LOWER(COALESCE(p.numeroRadicado, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(departamento.nombre)
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(organoControl.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                    OR LOWER(COALESCE(especialidad.nombre, ''))
                       LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
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
              )
              AND (:estado IS NULL OR p.estado = :estado)
              AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR p.fechaCreacion >= :fechaDesde)
              AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR p.fechaCreacion < :fechaHastaExclusiva)
              AND (
                    :alcanceGlobal = true
                    OR (
                        CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                        AND consulta.estudiante.id = :perfilId
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'ASESOR'
                        AND (
                            consulta.asesor.id = :perfilId
                            OR asesorEstudiante.id = :perfilId
                        )
                    )
                    OR (
                        CAST(:tipoPerfil AS String) = 'MONITOR'
                        AND consulta.monitor.id = :perfilId
                    )
              )
            """)
    Page<ProcesoResumenProjection> buscarResumenPaginado(
            @Param("search") String search,
            @Param("estado") EstadoProceso estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHastaExclusiva") LocalDateTime fechaHastaExclusiva,
            @Param("alcanceGlobal") boolean alcanceGlobal,
            @Param("tipoPerfil") String tipoPerfil,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado,
            Pageable pageable);
    // Procesos agrupados por estado — todos los tiempos.
    // El estado es varchar por ahora; se normaliza como catalogo en vacaciones.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                GROUP BY p.estado
                ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstado();

    // Procesos activos creados dentro del periodo estadístico configurado.
    @Query(value = """
        SELECT p.estado, COUNT(p.id) AS total_procesos
        FROM "DB_consultorioJuridico".proceso p
        JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
        WHERE p.activo = true
        AND c.estado <> 'ARCHIVADO'
        AND p.fecha_creacion >= :fechaInicio
        AND p.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        GROUP BY p.estado
        ORDER BY total_procesos DESC
        """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Procesos activos creados dentro de un rango libre.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE p.activo = true
                AND c.estado <> 'ARCHIVADO'
                AND p.fecha_creacion >= CAST(:fechaInicio AS date)
                AND p.fecha_creacion < (CAST(:fechaFin AS date) + INTERVAL '1 day')
                GROUP BY p.estado
                ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);


    // Procesos por estado y periodo filtrados por asesor.
    @Query(value = """
        SELECT p.estado, COUNT(p.id) AS total_procesos
        FROM "DB_consultorioJuridico".proceso p
        JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
        WHERE p.activo = true
        AND c.estado <> 'ARCHIVADO'
        AND p.fecha_creacion >= :fechaInicio
        AND p.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND c.asesor_id = :asesorId
        GROUP BY p.estado
        ORDER BY total_procesos DESC
        """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorPeriodoYAsesor(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("asesorId") Long asesorId);

    // Procesos por estado y periodo filtrados por estudiante.
    @Query(value = """
        SELECT p.estado, COUNT(p.id) AS total_procesos
        FROM "DB_consultorioJuridico".proceso p
        JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
        WHERE p.activo = true
        AND c.estado <> 'ARCHIVADO'
        AND p.fecha_creacion >= :fechaInicio
        AND p.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND c.estudiante_id = :estudianteId
        GROUP BY p.estado
        ORDER BY total_procesos DESC
        """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorPeriodoYEstudiante(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("estudianteId") Long estudianteId);

    // Procesos por estado y periodo filtrados por monitor.
    @Query(value = """
        SELECT p.estado, COUNT(p.id) AS total_procesos
        FROM "DB_consultorioJuridico".proceso p
        JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
        WHERE p.activo = true
        AND c.estado <> 'ARCHIVADO'
        AND p.fecha_creacion >= :fechaInicio
        AND p.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND c.monitor_id = :monitorId
        GROUP BY p.estado
        ORDER BY total_procesos DESC
        """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorPeriodoYMonitor(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("monitorId") Long monitorId);


}
