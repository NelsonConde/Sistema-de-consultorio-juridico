package co.edu.ufps.legal_cases.business.repository.conciliacion;

import java.util.Collection;
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

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;

@Repository
public interface ConciliacionRepository extends JpaRepository<Conciliacion, Long> {

        Optional<Conciliacion> findByIdAndActivoTrue(Long id);

        Optional<Conciliacion> findByIdAndActivoTrueAndConsulta_EstadoNot(
                        Long id,
                        EstadoConsulta estado);

        List<Conciliacion> findByActivoTrueOrderByIdDesc();

        List<Conciliacion> findByActivoTrueAndConsulta_EstadoNotOrderByIdDesc(
                        EstadoConsulta estado);

        List<Conciliacion> findByConsulta_IdAndActivoTrueOrderByIdDesc(Long consultaId);

        List<Conciliacion> findByConsulta_IdAndActivoTrueAndConsulta_EstadoNotOrderByIdDesc(
                        Long consultaId,
                        EstadoConsulta estado);

        boolean existsByConsulta_IdAndActivoTrue(Long consultaId);

        boolean existsByConsulta_IdAndActivoTrueAndEstado_CodigoIn(
                        Long consultaId,
                        Collection<String> codigosEstado);

        @Query(value = """
                SELECT c.id AS id,
                       c.version AS version,
                       consulta.id AS consultaId,
                       consulta.descripcion AS consulta,
                       estado.codigo AS estadoCodigo,
                       estado.nombre AS estadoNombre,
                       estudiante.id AS estudianteId,
                       estudiante.nombre AS estudianteNombre,
                       conciliador.id AS conciliadorId,
                       conciliador.nombre AS conciliadorNombre,
                       c.fechaCreacion AS fechaCreacion,
                       c.fechaConciliacion AS fechaConciliacion,
                       c.fechaFinalizacion AS fechaFinalizacion,
                       c.activo AS activo
                FROM Conciliacion c
                JOIN c.consulta consulta
                JOIN consulta.persona persona
                JOIN c.estado estado
                LEFT JOIN c.estudiante estudiante
                LEFT JOIN c.conciliador conciliador
                LEFT JOIN consulta.estudiante estudianteConsulta
                LEFT JOIN consulta.asesor asesor
                LEFT JOIN consulta.monitor monitor
                WHERE c.activo = true
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
                        OR LOWER(COALESCE(estudiante.nombre, ''))
                           LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(COALESCE(conciliador.nombre, ''))
                           LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                  )
                  AND (CAST(:estadoCodigo AS String) IS NULL OR estado.codigo = :estadoCodigo)
                  AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR c.fechaCreacion >= :fechaDesde)
                  AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR c.fechaCreacion < :fechaHastaExclusiva)
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
                SELECT COUNT(c.id)
                FROM Conciliacion c
                JOIN c.consulta consulta
                JOIN consulta.persona persona
                JOIN c.estado estado
                LEFT JOIN c.estudiante estudiante
                LEFT JOIN c.conciliador conciliador
                LEFT JOIN consulta.estudiante estudianteConsulta
                LEFT JOIN consulta.asesor asesor
                LEFT JOIN consulta.monitor monitor
                WHERE c.activo = true
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
                        OR LOWER(COALESCE(estudiante.nombre, ''))
                           LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(COALESCE(conciliador.nombre, ''))
                           LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                  )
                  AND (CAST(:estadoCodigo AS String) IS NULL OR estado.codigo = :estadoCodigo)
                  AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR c.fechaCreacion >= :fechaDesde)
                  AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR c.fechaCreacion < :fechaHastaExclusiva)
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
        Page<ConciliacionResumenProjection> buscarResumenPaginado(
                        @Param("search") String search,
                        @Param("estadoCodigo") String estadoCodigo,
                        @Param("fechaDesde") LocalDateTime fechaDesde,
                        @Param("fechaHastaExclusiva") LocalDateTime fechaHastaExclusiva,
                        @Param("alcanceGlobal") boolean alcanceGlobal,
                        @Param("tipoPerfil") String tipoPerfil,
                        @Param("perfilId") Long perfilId,
                        @Param("estadoArchivado") EstadoConsulta estadoArchivado,
                        Pageable pageable);

        long countByEstudiante_IdAndActivoTrueAndEstado_CodigoIn(
                        Long estudianteId,
                        Collection<String> codigosEstado);

        long countByConciliador_IdAndActivoTrueAndEstado_CodigoIn(
                        Long conciliadorId,
                        Collection<String> codigosEstado);

        // Total de conciliaciones dentro del periodo estadístico configurado.
        @Query(value = """
        SELECT COUNT(c.id) AS total
        FROM "DB_consultorioJuridico".conciliacion c
        WHERE c.fecha_creacion >= :fechaInicio
        AND c.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND c.activo = true
        """, nativeQuery = true)
        List<Object[]> contarConciliacionesPorPeriodo(
                @Param("fechaInicio") LocalDate fechaInicio,
                @Param("fechaFin") LocalDate fechaFin);

        // Conciliaciones agrupadas por estado dentro del periodo estadístico configurado.
        @Query(value = """
        SELECT ec.nombre, COUNT(c.id) AS total
        FROM "DB_consultorioJuridico".conciliacion c
        JOIN "DB_consultorioJuridico".estado_conciliacion ec ON ec.id = c.estado_id
        WHERE c.fecha_creacion >= :fechaInicio
        AND c.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND c.activo = true
        GROUP BY ec.nombre, ec.orden
        ORDER BY ec.orden
        """, nativeQuery = true)
        List<Object[]> contarConciliacionesPorEstadoPorPeriodo(
                @Param("fechaInicio") LocalDate fechaInicio,
                @Param("fechaFin") LocalDate fechaFin);

        @Query(value = """
                        SELECT COUNT(c.id) AS total
                        FROM "DB_consultorioJuridico".conciliacion c
                        WHERE c.fecha_creacion >= CAST(:fechaInicio AS date)
                        AND c.fecha_creacion <= CAST(:fechaFin AS date)
                        AND c.activo = true
                        """, nativeQuery = true)
        List<Object[]> contarConciliacionesPorRango(
                        @Param("fechaInicio") String fechaInicio,
                        @Param("fechaFin") String fechaFin);

        @Query(value = """
                        SELECT ec.nombre, COUNT(c.id) AS total
                        FROM "DB_consultorioJuridico".conciliacion c
                        JOIN "DB_consultorioJuridico".estado_conciliacion ec ON ec.id = c.estado_id
                        WHERE c.fecha_creacion >= CAST(:fechaInicio AS date)
                        AND c.fecha_creacion <= CAST(:fechaFin AS date)
                        AND c.activo = true
                        GROUP BY ec.nombre, ec.orden ORDER BY ec.orden
                        """, nativeQuery = true)
        List<Object[]> contarConciliacionesPorEstadoPorRango(
                        @Param("fechaInicio") String fechaInicio,
                        @Param("fechaFin") String fechaFin);

}
