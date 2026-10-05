package co.edu.ufps.legal_cases.business.repository.proceso;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    List<Proceso> findByConsulta_IdAndActivoTrueOrderByIdDesc(Long consultaId);

    boolean existsByNumeroRadicadoAndIdNot(String numeroRadicado, Long id);

    boolean existsByConsulta_IdAndActivoTrueAndEstado(Long consultaId, EstadoProceso estado);
    // Procesos agrupados por estado — todos los tiempos.
    // El estado es varchar por ahora; se normaliza como catalogo en vacaciones.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                GROUP BY p.estado
                ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstado();

    // Procesos activos asociados a consultas del semestre estadístico.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE p.activo = true
                AND c.estado <> 'ARCHIVADO'
                AND c.fecha >=
                    CASE WHEN :semester = 1 THEN make_date(:year, 1, 1) ELSE make_date(:year, 7, 1) END
                AND c.fecha <
                    CASE WHEN :semester = 1 THEN make_date(:year, 7, 1) ELSE make_date(:year + 1, 1, 1) END
                GROUP BY p.estado
                ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorSemestre(
            @Param("year") int year,
            @Param("semester") int semester);

    // Procesos activos asociados a consultas dentro de un rango libre.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE p.activo = true
                AND c.estado <> 'ARCHIVADO'
                AND c.fecha >= CAST(:fechaInicio AS date)
                AND c.fecha <= CAST(:fechaFin AS date)
                GROUP BY p.estado
                ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);


    // Procesos por estado y semestre filtrados por asesor.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE p.activo = true
                AND c.estado <> 'ARCHIVADO'
                AND c.fecha >=
                    CASE WHEN :semester = 1 THEN make_date(:year, 1, 1) ELSE make_date(:year, 7, 1) END
                AND c.fecha <
                    CASE WHEN :semester = 1 THEN make_date(:year, 7, 1) ELSE make_date(:year + 1, 1, 1) END
                AND c.asesor_id = :asesorId
                GROUP BY p.estado ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorSemestreYAsesor(
            @Param("year") int year,
            @Param("semester") int semester,
            @Param("asesorId") Long asesorId);

    // Procesos por estado y semestre filtrados por estudiante.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE p.activo = true
                AND c.estado <> 'ARCHIVADO'
                AND c.fecha >=
                    CASE WHEN :semester = 1 THEN make_date(:year, 1, 1) ELSE make_date(:year, 7, 1) END
                AND c.fecha <
                    CASE WHEN :semester = 1 THEN make_date(:year, 7, 1) ELSE make_date(:year + 1, 1, 1) END
                AND c.estudiante_id = :estudianteId
                GROUP BY p.estado ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorSemestreYEstudiante(
            @Param("year") int year,
            @Param("semester") int semester,
            @Param("estudianteId") Long estudianteId);

    // Procesos por estado y semestre filtrados por monitor.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE p.activo = true
                AND c.estado <> 'ARCHIVADO'
                AND c.fecha >=
                    CASE WHEN :semester = 1 THEN make_date(:year, 1, 1) ELSE make_date(:year, 7, 1) END
                AND c.fecha <
                    CASE WHEN :semester = 1 THEN make_date(:year, 7, 1) ELSE make_date(:year + 1, 1, 1) END
                AND c.monitor_id = :monitorId
                GROUP BY p.estado ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoPorSemestreYMonitor(
            @Param("year") int year,
            @Param("semester") int semester,
            @Param("monitorId") Long monitorId);




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
}
