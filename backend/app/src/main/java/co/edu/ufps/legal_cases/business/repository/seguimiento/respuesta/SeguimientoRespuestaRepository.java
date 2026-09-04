package co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta;

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
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;

@Repository
public interface SeguimientoRespuestaRepository extends JpaRepository<SeguimientoRespuesta, Long> {

        Optional<SeguimientoRespuesta> findByIdAndActivoTrue(Long id);

        Optional<SeguimientoRespuesta> findByIdAndSeguimiento_IdAndActivoTrue(Long respuestaId, Long seguimientoId);

        Optional<SeguimientoRespuesta> findByIdAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNot(
                        Long id,
                        EstadoConsulta estado);

        Optional<SeguimientoRespuesta> findBySeguimiento_IdAndEstudiante_IdAndActivoTrue(
                        Long seguimientoId,
                        Long estudianteId);

        // Trae el último intento activo del estudiante para ese seguimiento.
        // Se usa para decidir si puede crear una nueva respuesta.
        Optional<SeguimientoRespuesta> findFirstBySeguimiento_IdAndEstudiante_IdAndActivoTrueOrderByFechaCreacionDescIdDesc(
                        Long seguimientoId,
                        Long estudianteId);

        List<SeguimientoRespuesta> findBySeguimiento_IdAndActivoTrueOrderByFechaCreacionDesc(Long seguimientoId);

        List<SeguimientoRespuesta> findBySeguimiento_IdAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNotOrderByFechaCreacionDesc(
                        Long seguimientoId,
                        EstadoConsulta estado);

        List<SeguimientoRespuesta> findByEstadoAndActivoTrueOrderByFechaCreacionDesc(
                        EstadoRespuestaSeguimiento estado);

        List<SeguimientoRespuesta> findByEstadoAndActivoTrueAndSeguimiento_ActivoTrueAndSeguimiento_Consulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoRespuestaSeguimiento estado,
                        EstadoConsulta estadoConsulta);

        // Listado paginado de respuestas pendientes con alcance resuelto en JPQL.
        @Query(value = """
                        SELECT r.id AS id,
                               r.version AS version,
                               s.id AS seguimientoId,
                               c.id AS consultaId,
                               estudianteRespuesta.id AS estudianteId,
                               estudianteRespuesta.nombre AS estudianteNombre,
                               r.contenido AS contenido,
                               r.estado AS estado,
                               r.fueraPlazo AS fueraPlazo,
                               r.observacionRevision AS observacionRevision,
                               revisadoPor.id AS revisadoPorId,
                               revisadoPor.username AS revisadoPorUsername,
                               r.activo AS activo,
                               r.fechaCreacion AS fechaCreacion,
                               r.fechaActualizacion AS fechaActualizacion,
                               r.fechaDecision AS fechaDecision
                        FROM SeguimientoRespuesta r
                        JOIN r.seguimiento s
                        JOIN s.consulta c
                        JOIN r.estudiante estudianteRespuesta
                        LEFT JOIN r.revisadoPor revisadoPor
                        LEFT JOIN c.asesor asesorDirecto
                        LEFT JOIN c.estudiante estudianteConsulta
                        LEFT JOIN estudianteConsulta.asesor asesorEstudiante
                        LEFT JOIN c.monitor monitor
                        WHERE r.activo = true
                          AND r.estado = :estadoPendiente
                          AND s.activo = true
                          AND c.estado <> :estadoArchivado
                          AND (
                                CAST(:search AS String) IS NULL
                                OR LOWER(r.contenido)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(estudianteRespuesta.nombre)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                          )
                          AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR r.fechaCreacion >= :fechaDesde)
                          AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR r.fechaCreacion < :fechaHastaExclusiva)
                          AND (
                                :alcanceGlobal = true
                                OR (
                                    CAST(:tipoPerfil AS String) = 'ASESOR'
                                    AND (
                                        asesorDirecto.id = :perfilId
                                        OR asesorEstudiante.id = :perfilId
                                    )
                                )
                                OR (
                                    CAST(:tipoPerfil AS String) = 'MONITOR'
                                    AND monitor.id = :perfilId
                                )
                          )
                        """, countQuery = """
                        SELECT COUNT(r.id)
                        FROM SeguimientoRespuesta r
                        JOIN r.seguimiento s
                        JOIN s.consulta c
                        JOIN r.estudiante estudianteRespuesta
                        LEFT JOIN c.asesor asesorDirecto
                        LEFT JOIN c.estudiante estudianteConsulta
                        LEFT JOIN estudianteConsulta.asesor asesorEstudiante
                        LEFT JOIN c.monitor monitor
                        WHERE r.activo = true
                          AND r.estado = :estadoPendiente
                          AND s.activo = true
                          AND c.estado <> :estadoArchivado
                          AND (
                                CAST(:search AS String) IS NULL
                                OR LOWER(r.contenido)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(estudianteRespuesta.nombre)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                          )
                          AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR r.fechaCreacion >= :fechaDesde)
                          AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR r.fechaCreacion < :fechaHastaExclusiva)
                          AND (
                                :alcanceGlobal = true
                                OR (
                                    CAST(:tipoPerfil AS String) = 'ASESOR'
                                    AND (
                                        asesorDirecto.id = :perfilId
                                        OR asesorEstudiante.id = :perfilId
                                    )
                                )
                                OR (
                                    CAST(:tipoPerfil AS String) = 'MONITOR'
                                    AND monitor.id = :perfilId
                                )
                          )
                        """)
        Page<SeguimientoRespuestaPendienteProjection> buscarPendientesPaginado(
                        @Param("search") String search,
                        @Param("estadoPendiente") EstadoRespuestaSeguimiento estadoPendiente,
                        @Param("fechaDesde") LocalDateTime fechaDesde,
                        @Param("fechaHastaExclusiva") LocalDateTime fechaHastaExclusiva,
                        @Param("alcanceGlobal") boolean alcanceGlobal,
                        @Param("tipoPerfil") String tipoPerfil,
                        @Param("perfilId") Long perfilId,
                        @Param("estadoArchivado") EstadoConsulta estadoArchivado,
                        Pageable pageable);

        // Sirve para validar un seguimiento puntual
        boolean existsBySeguimiento_IdAndActivoTrueAndEstado(Long seguimientoId, EstadoRespuestaSeguimiento estado);

        // Sirve para validar todos los seguimientos de una consulta
        boolean existsBySeguimiento_Consulta_IdAndSeguimiento_ActivoTrueAndActivoTrueAndEstado(Long consultaId,
                        EstadoRespuestaSeguimiento estado);
}
