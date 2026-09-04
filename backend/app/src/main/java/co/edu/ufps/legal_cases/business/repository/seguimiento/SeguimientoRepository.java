package co.edu.ufps.legal_cases.business.repository.seguimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosCorreoSeguimientoDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosNotificacionSeguimientoDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;

@Repository
public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {

        Optional<Seguimiento> findByIdAndActivoTrue(Long id);

        Optional<Seguimiento> findByIdAndActivoTrueAndConsulta_EstadoNot(
                        Long id,
                        EstadoConsulta estado);

        // Lista todos los seguimientos activos de una consulta.
        // Lo usarían asesor, monitor o administrativos según permisos.
        List<Seguimiento> findByConsulta_IdAndActivoTrueOrderByFechaCreacionDesc(Long consultaId);

        // Lista todos los seguimientos activos de una consulta,
        // excluyendo consultas archivadas para pantallas operativas.
        List<Seguimiento> findByConsulta_IdAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        Long consultaId,
                        EstadoConsulta estado);

        // Lista únicamente los seguimientos activos visibles para el estudiante.
        // En tu regla actual, notificarEstudiante = true significa:
        // se notifica al estudiante y también se le puede mostrar.
        List<Seguimiento> findByConsulta_IdAndNotificarEstudianteTrueAndActivoTrueOrderByFechaCreacionDesc(
                        Long consultaId);

        // Lista seguimientos visibles para el estudiante,
        // excluyendo consultas archivadas para evitar contaminación visual.
        List<Seguimiento> findByConsulta_IdAndNotificarEstudianteTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        Long consultaId,
                        EstadoConsulta estado);

        // Lista seguimientos activos creados por un usuario del sistema.
        List<Seguimiento> findByAutor_IdAndActivoTrueOrderByFechaCreacionDesc(Long autorId);

        // Lista seguimientos activos creados por un usuario,
        // excluyendo consultas archivadas.
        List<Seguimiento> findByAutor_IdAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        Long autorId,
                        EstadoConsulta estado);

        // Se usa para evitar eliminar categorías que ya tienen seguimientos asociados.
        boolean existsByCategoriaSeguimiento_Id(Long categoriaSeguimientoId);

        // Sirve para validar o consultar si una consulta tiene seguimientos.
        boolean existsByConsulta_Id(Long consultaId);

        // Sirve para validar si la consulta ya tiene actividad operativa de
        // seguimiento.
        boolean existsByConsulta_IdAndActivoTrue(Long consultaId);

        @Query(value = """
                        SELECT s.id AS id,
                               s.version AS version,
                               s.descripcion AS descripcion,
                               s.fechaEntrega AS fechaEntrega,
                               s.diasNotificacion AS diasNotificacion,
                               s.notificarPartes AS notificarPartes,
                               s.notificarEstudiante AS notificarEstudiante,
                               s.alertaDisciplinaria AS alertaDisciplinaria,
                               s.estado AS estado,
                               categoria.id AS categoriaSeguimientoId,
                               categoria.nombre AS categoriaSeguimientoNombre,
                               c.id AS consultaId,
                               autor.id AS autorId,
                               autor.username AS autorUsername,
                               s.fechaCreacion AS fechaCreacion,
                               s.fechaActualizacion AS fechaActualizacion
                        FROM Seguimiento s
                        JOIN s.consulta c
                        JOIN s.categoriaSeguimiento categoria
                        JOIN s.autor autor
                        LEFT JOIN c.asesor asesorDirecto
                        LEFT JOIN c.estudiante estudiante
                        LEFT JOIN estudiante.asesor asesorEstudiante
                        LEFT JOIN c.monitor monitor
                        WHERE s.activo = true
                          AND c.estado <> :estadoArchivado
                          AND (
                                CAST(:search AS String) IS NULL
                                OR LOWER(s.descripcion)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(categoria.nombre)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(autor.username)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(CAST(s.estado AS String))
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(c.descripcion)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                          )
                          AND (:estado IS NULL OR s.estado = :estado)
                          AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR s.fechaCreacion >= :fechaDesde)
                          AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR s.fechaCreacion < :fechaHastaExclusiva)
                          AND (:consultaId IS NULL OR c.id = :consultaId)
                          AND (:autorId IS NULL OR autor.id = :autorId)
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
                                OR (
                                    CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                                    AND estudiante.id = :perfilId
                                    AND s.notificarEstudiante = true
                                )
                          )
                        """, countQuery = """
                        SELECT COUNT(s.id)
                        FROM Seguimiento s
                        JOIN s.consulta c
                        JOIN s.categoriaSeguimiento categoria
                        JOIN s.autor autor
                        LEFT JOIN c.asesor asesorDirecto
                        LEFT JOIN c.estudiante estudiante
                        LEFT JOIN estudiante.asesor asesorEstudiante
                        LEFT JOIN c.monitor monitor
                        WHERE s.activo = true
                          AND c.estado <> :estadoArchivado
                          AND (
                                CAST(:search AS String) IS NULL
                                OR LOWER(s.descripcion)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(categoria.nombre)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(autor.username)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(CAST(s.estado AS String))
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(c.descripcion)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                          )
                          AND (:estado IS NULL OR s.estado = :estado)
                          AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR s.fechaCreacion >= :fechaDesde)
                          AND (CAST(:fechaHastaExclusiva AS LocalDateTime) IS NULL OR s.fechaCreacion < :fechaHastaExclusiva)
                          AND (:consultaId IS NULL OR c.id = :consultaId)
                          AND (:autorId IS NULL OR autor.id = :autorId)
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
                                OR (
                                    CAST(:tipoPerfil AS String) = 'ESTUDIANTE'
                                    AND estudiante.id = :perfilId
                                    AND s.notificarEstudiante = true
                                )
                          )
                        """)
        Page<SeguimientoResumenProjection> buscarResumenPaginado(
                        @Param("search") String search,
                        @Param("estado") EstadoSeguimiento estado,
                        @Param("fechaDesde") LocalDateTime fechaDesde,
                        @Param("fechaHastaExclusiva") LocalDateTime fechaHastaExclusiva,
                        @Param("consultaId") Long consultaId,
                        @Param("autorId") Long autorId,
                        @Param("alcanceGlobal") boolean alcanceGlobal,
                        @Param("tipoPerfil") String tipoPerfil,
                        @Param("perfilId") Long perfilId,
                        @Param("estadoArchivado") EstadoConsulta estadoArchivado,
                        Pageable pageable);

        // Lista seguimientos activos marcados como alerta disciplinaria.
        List<Seguimiento> findByAlertaDisciplinariaTrueAndActivoTrueOrderByFechaCreacionDesc();

        // Lista alertas disciplinarias activas, excluyendo consultas archivadas.
        @EntityGraph(attributePaths = {
                "categoriaSeguimiento",
                "consulta",
                "autor"
        })
        List<Seguimiento> findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                EstadoConsulta estado);
        
        // Filtra el alcance del asesor directamente en base de datos para no exponer
        // alertas de consultas ajenas. Las relaciones del EntityGraph son las necesarias
        // para construir el DTO sin depender de cargas LAZY posteriores.
        @EntityGraph(attributePaths = {
                "categoriaSeguimiento",
                "consulta",
                "autor"
        })
        @Query("""
                        SELECT s
                        FROM Seguimiento s
                        JOIN s.consulta c
                        LEFT JOIN c.estudiante e
                        LEFT JOIN e.asesor asesorEstudiante
                        WHERE s.alertaDisciplinaria = true
                        AND s.activo = true
                        AND c.estado <> :estadoArchivado
                        AND (
                                c.asesor.id = :asesorId
                                OR asesorEstudiante.id = :asesorId
                        )
                        ORDER BY s.fechaCreacion DESC, s.id DESC
                        """)
        List<Seguimiento> findAlertasDisciplinariasByAsesorScope(
                        @Param("asesorId") Long asesorId,
                        @Param("estadoArchivado") EstadoConsulta estadoArchivado);

        // Filtra el alcance del monitor directamente en base de datos para no exponer
        // alertas de consultas asignadas a otros monitores.
        @EntityGraph(attributePaths = {
                "categoriaSeguimiento",
                "consulta",
                "autor"
        })
        @Query("""
                SELECT s
                FROM Seguimiento s
                JOIN s.consulta c
                WHERE s.alertaDisciplinaria = true
                AND s.activo = true
                AND c.estado <> :estadoArchivado
                AND c.monitor.id = :monitorId
                ORDER BY s.fechaCreacion DESC, s.id DESC
                """)
        List<Seguimiento> findAlertasDisciplinariasByMonitorScope(
                @Param("monitorId") Long monitorId,
                @Param("estadoArchivado") EstadoConsulta estadoArchivado);

        // Lista seguimientos activos por fecha de entrega.
        List<Seguimiento> findByFechaEntregaAndActivoTrueOrderByFechaCreacionDesc(LocalDate fechaEntrega);

        // Lista seguimientos activos por fecha de entrega,
        // excluyendo consultas archivadas.
        List<Seguimiento> findByFechaEntregaAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        LocalDate fechaEntrega,
                        EstadoConsulta estado);

        @Query("""
                        SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosNotificacionSeguimientoDTO(
                            s.id,
                            s.consulta.id,
                            s.autor.id,
                            s.autor.username
                        )
                        FROM Seguimiento s
                        WHERE s.id = :seguimientoId
                        AND s.activo = true
                        """)
        Optional<DatosNotificacionSeguimientoDTO> findDatosNotificacionById(
                        @Param("seguimientoId") Long seguimientoId);

        // Para enviar datos al servicio de correo sin exponer toda la entidad de
        // seguimiento.
        @Query("""
                        SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.DatosCorreoSeguimientoDTO(
                            s.id,
                            s.descripcion,
                            s.categoriaSeguimiento.nombre,
                            s.consulta.id,
                            s.fechaEntrega,
                            s.diasNotificacion,
                            s.notificarPartes,
                            s.notificarEstudiante,
                            s.alertaDisciplinaria
                        )
                        FROM Seguimiento s
                        WHERE s.id = :seguimientoId
                        AND s.activo = true
                        """)
        Optional<DatosCorreoSeguimientoDTO> findDatosCorreoById(@Param("seguimientoId") Long seguimientoId);

        boolean existsByConsulta_IdAndActivoTrueAndEstado(Long consultaId, EstadoSeguimiento estado);

        // Total de seguimientos dentro del periodo estadístico configurado.
        @Query(value = """
        SELECT COUNT(s.id) AS total
        FROM "DB_consultorioJuridico".seguimiento s
        WHERE s.fecha_creacion >= :fechaInicio
        AND s.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND s.activo = true
        """, nativeQuery = true)
        List<Object[]> contarSeguimientosPorPeriodo(
                @Param("fechaInicio") LocalDate fechaInicio,
                @Param("fechaFin") LocalDate fechaFin);

        // Seguimientos agrupados por estado dentro del periodo estadístico configurado.
        @Query(value = """
        SELECT s.estado, COUNT(s.id) AS total
        FROM "DB_consultorioJuridico".seguimiento s
        WHERE s.fecha_creacion >= :fechaInicio
        AND s.fecha_creacion < (:fechaFin + INTERVAL '1 day')
        AND s.activo = true
        GROUP BY s.estado
        ORDER BY total DESC
        """, nativeQuery = true)
        List<Object[]> contarSeguimientosPorEstadoPorPeriodo(
                @Param("fechaInicio") LocalDate fechaInicio,
                @Param("fechaFin") LocalDate fechaFin);

        @Query(value = """
                        SELECT COUNT(s.id) AS total
                        FROM "DB_consultorioJuridico".seguimiento s
                        WHERE s.fecha_creacion >= CAST(:fechaInicio AS date)
                        AND s.fecha_creacion <= CAST(:fechaFin AS date)
                        AND s.activo = true
                        """, nativeQuery = true)
        List<Object[]> contarSeguimientosPorRango(
                        @Param("fechaInicio") String fechaInicio,
                        @Param("fechaFin") String fechaFin);

        @Query(value = """
                        SELECT s.estado, COUNT(s.id) AS total
                        FROM "DB_consultorioJuridico".seguimiento s
                        WHERE s.fecha_creacion >= CAST(:fechaInicio AS date)
                        AND s.fecha_creacion <= CAST(:fechaFin AS date)
                        AND s.activo = true
                        GROUP BY s.estado ORDER BY total DESC
                        """, nativeQuery = true)
        List<Object[]> contarSeguimientosPorEstadoPorRango(
                        @Param("fechaInicio") String fechaInicio,
                        @Param("fechaFin") String fechaFin);

}
