package co.edu.ufps.legal_cases.business.repository.consulta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    boolean existsByAsesor_IdAndEstadoIn(Long asesorId, List<EstadoConsulta> estados);

    boolean existsByEstudiante_Asesor_IdAndEstadoIn(Long asesorId, List<EstadoConsulta> estados);

    boolean existsByEstudiante_IdAndEstadoIn(Long estudianteId, List<EstadoConsulta> estados);

    boolean existsByMonitor_IdAndEstadoIn(Long monitorId, List<EstadoConsulta> estados);

    // Hibernate no permite hacer JOIN FETCH de dos colecciones al mismo tiempo.
    // Por eso partes y contrapartes se cargan en consultas separadas.
    @Query("""
                        SELECT DISTINCT c
                        FROM Consulta c
                        LEFT JOIN FETCH c.partes
                        WHERE c.id = :id
                        """)
    Optional<Consulta> findByIdConPartes(@Param("id") Long id);

    @Query("""
                        SELECT DISTINCT c
                        FROM Consulta c
                        LEFT JOIN FETCH c.contrapartes
                        WHERE c.id = :id
                        """)
    Optional<Consulta> findByIdConContrapartes(@Param("id") Long id);

    @Query("""
                        SELECT c FROM Consulta c
                        JOIN c.persona p
                        WHERE c.estado <> co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta.ARCHIVADO
                          AND (:search IS NULL OR :search = ''
                           OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.nombres)           LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.apellidos)         LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.numeroDocumento)   LIKE LOWER(CONCAT('%', :search, '%')))
                        ORDER BY c.fecha DESC
                        """)
    List<Consulta> buscar(@Param("search") String search);

    // Búsqueda para administrador.
    // El administrador puede ver todas las consultas activas.
    @Query("""
                        SELECT c FROM Consulta c
                        JOIN c.persona p
                        WHERE c.estado <> co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta.ARCHIVADO
                          AND (:search IS NULL OR :search = ''
                           OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.nombres)           LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.apellidos)         LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.numeroDocumento)   LIKE LOWER(CONCAT('%', :search, '%')))
                        ORDER BY c.fecha DESC
                        """)
    List<Consulta> buscarParaAdministrador(@Param("search") String search);

    // Búsqueda para estudiante.
    // El estudiante solo ve las consultas asociadas a su perfil.
    @Query("""
                        SELECT c FROM Consulta c
                        JOIN c.persona p
                        WHERE c.estado <> co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta.ARCHIVADO
                          AND (:search IS NULL OR :search = ''
                           OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.nombres)           LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.apellidos)         LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.numeroDocumento)   LIKE LOWER(CONCAT('%', :search, '%')))
                          AND c.estudiante.id = :estudianteId
                        ORDER BY c.fecha DESC
                        """)
    List<Consulta> buscarParaEstudiante(
            @Param("search") String search,
            @Param("estudianteId") Long estudianteId);

    // Búsqueda para asesor.
    // El asesor ve consultas asignadas directamente a él
    // y consultas de estudiantes que pertenecen a su asesoría.
    @Query("""
                        SELECT c FROM Consulta c
                        JOIN c.persona p
                        WHERE c.estado <> co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta.ARCHIVADO
                          AND (:search IS NULL OR :search = ''
                           OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.nombres)           LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.apellidos)         LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.numeroDocumento)   LIKE LOWER(CONCAT('%', :search, '%')))
                          AND (
                                c.asesor.id = :asesorId
                                OR c.estudiante.asesor.id = :asesorId
                          )
                        ORDER BY c.fecha DESC
                        """)
    List<Consulta> buscarParaAsesor(
            @Param("search") String search,
            @Param("asesorId") Long asesorId);

    // Búsqueda para monitor.
    // El monitor solo ve consultas donde está asignado.
    @Query("""
                        SELECT c FROM Consulta c
                        JOIN c.persona p
                        WHERE c.estado <> co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta.ARCHIVADO
                          AND (:search IS NULL OR :search = ''
                           OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.nombres)           LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.apellidos)         LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.numeroDocumento)   LIKE LOWER(CONCAT('%', :search, '%')))
                          AND c.monitor.id = :monitorId
                        ORDER BY c.fecha DESC
                        """)
    List<Consulta> buscarParaMonitor(
            @Param("search") String search,
            @Param("monitorId") Long monitorId);

    // Búsqueda filtrada anterior.
    // Se conserva temporalmente para no romper llamadas existentes mientras se migra el service.
    @Query("""
                        SELECT c FROM Consulta c
                        JOIN c.persona p
                        WHERE c.estado <> co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta.ARCHIVADO
                          AND (:search IS NULL OR :search = ''
                           OR LOWER(c.descripcion)       LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.nombres)           LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.apellidos)         LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(p.numeroDocumento)   LIKE LOWER(CONCAT('%', :search, '%')))
                          AND (:estudianteId IS NULL OR c.estudiante.id = :estudianteId)
                          AND (:asesorId     IS NULL OR c.asesor.id     = :asesorId)
                          AND (:monitorId    IS NULL OR c.monitor.id    = :monitorId)
                        ORDER BY c.fecha DESC
                        """)
    List<Consulta> buscarFiltrado(
            @Param("search") String search,
            @Param("estudianteId") Long estudianteId,
            @Param("asesorId") Long asesorId,
            @Param("monitorId") Long monitorId);

    // Destinatario principal de la consulta.
    @Query("""
                        SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                            p.correo,
                            TRIM(CONCAT(CONCAT(COALESCE(p.nombres, ''), ' '), COALESCE(p.apellidos, '')))
                        )
                        FROM Consulta c
                        JOIN c.persona p
                        WHERE c.id = :consultaId
                        AND p.correo IS NOT NULL
                        AND TRIM(p.correo) <> ''
                        """)
    List<SeguimientoDestinatarioDTO> findDestinatarioPersonaPrincipalByConsultaId(
            @Param("consultaId") Long consultaId);

    // Partes adicionales de la consulta.
    @Query("""
                        SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                            p.correo,
                            TRIM(CONCAT(CONCAT(COALESCE(p.nombres, ''), ' '), COALESCE(p.apellidos, '')))
                        )
                        FROM Consulta c
                        JOIN c.partes p
                        WHERE c.id = :consultaId
                        AND p.correo IS NOT NULL
                        AND TRIM(p.correo) <> ''
                        """)
    List<SeguimientoDestinatarioDTO> findDestinatariosPartesByConsultaId(
            @Param("consultaId") Long consultaId);

    // Contrapartes de la consulta.
    @Query("""
                        SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                            p.correo,
                            TRIM(CONCAT(CONCAT(COALESCE(p.nombres, ''), ' '), COALESCE(p.apellidos, '')))
                        )
                        FROM Consulta c
                        JOIN c.contrapartes p
                        WHERE c.id = :consultaId
                        AND p.correo IS NOT NULL
                        AND TRIM(p.correo) <> ''
                        """)
    List<SeguimientoDestinatarioDTO> findDestinatariosContrapartesByConsultaId(
            @Param("consultaId") Long consultaId);

    // Estudiante asignado a la consulta.
    @Query("""
                        SELECT new co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion.SeguimientoDestinatarioDTO(
                            e.email,
                            e.nombre
                        )
                        FROM Consulta c
                        JOIN c.estudiante e
                        WHERE c.id = :consultaId
                        AND e.activo = true
                        """)
    Optional<SeguimientoDestinatarioDTO> findDestinatarioEstudianteByConsultaId(
            @Param("consultaId") Long consultaId);

    List<Consulta> findByEstado(EstadoConsulta estado);

    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE c.estado = 'CERRADO') AS finished_consultas,
            COUNT(*) FILTER (
                WHERE c.estado <> 'CERRADO'
                AND c.estado <> 'ARCHIVADO'
            ) AS unfinished_consultas
        FROM "DB_consultorioJuridico".consulta c
        WHERE c.fecha >= :fechaInicio
        AND c.fecha <= :fechaFin
        AND c.estado <> 'ARCHIVADO'
        """, nativeQuery = true)
    List<Object[]> contarFinalizadasYPendientesPorPeriodoRaw(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);


    // Consultas agrupadas por área jurídica — todos los tiempos.
    @Query(value = """
                SELECT a.id AS area_id, a.nombre AS area_nombre, COUNT(c.id) AS total_consultas
                FROM "DB_consultorioJuridico".consulta c
                JOIN "DB_consultorioJuridico".area a ON a.id = c.area_id
                GROUP BY a.id, a.nombre
                ORDER BY total_consultas DESC
                """, nativeQuery = true)
    List<Object[]> contarConsultasPorAreaTodos();

    @Query(value = """
        SELECT
            a.id AS area_id,
            a.nombre AS area_nombre,
            COUNT(c.id) AS total_consultas
        FROM "DB_consultorioJuridico".consulta c
        JOIN "DB_consultorioJuridico".area a
            ON a.id = c.area_id
        WHERE c.fecha >= :fechaInicio
        AND c.fecha <= :fechaFin
        AND c.estado <> 'ARCHIVADO'
        GROUP BY a.id, a.nombre
        ORDER BY total_consultas DESC
        """, nativeQuery = true)
    List<Object[]> contarConsultasPorAreaPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);


    @Query(value = """
        SELECT COUNT(DISTINCT persona_id) AS total_personas_atendidas
        FROM (
            SELECT c.persona_id
            FROM "DB_consultorioJuridico".consulta c
            WHERE c.fecha >= :fechaInicio
            AND c.fecha <= :fechaFin
            AND c.estado <> 'ARCHIVADO'

            UNION

            SELECT cp.persona_id
            FROM "DB_consultorioJuridico".consulta_parte cp
            JOIN "DB_consultorioJuridico".consulta c
                ON c.id = cp.consulta_id
            WHERE c.fecha >= :fechaInicio
            AND c.fecha <= :fechaFin
            AND c.estado <> 'ARCHIVADO'
        ) AS personas_unicas
        """, nativeQuery = true)
    List<Object[]> contarPersonasAtendidasPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);


    // Consultas finalizadas/pendientes por periodo filtradas por asesor.
    @Query(value = """
                SELECT COUNT(*) FILTER (WHERE c.estado = 'CERRADO') AS finished_consultas,
                       COUNT(*) FILTER (WHERE c.estado <> 'CERRADO' AND c.estado <> 'ARCHIVADO') AS unfinished_consultas
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= :fechaInicio
                AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                AND c.asesor_id = :asesorId
                """, nativeQuery = true)
    List<Object[]> contarFinalizadasYPendientesPorPeriodoYAsesor(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("asesorId") Long asesorId);

    // Consultas finalizadas/pendientes por periodo filtradas por estudiante.
    @Query(value = """
                SELECT COUNT(*) FILTER (WHERE c.estado = 'CERRADO') AS finished_consultas,
                       COUNT(*) FILTER (WHERE c.estado <> 'CERRADO' AND c.estado <> 'ARCHIVADO') AS unfinished_consultas
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= :fechaInicio
                AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                AND c.estudiante_id = :estudianteId
                """, nativeQuery = true)
    List<Object[]> contarFinalizadasYPendientesPorPeriodoYEstudiante(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("estudianteId") Long estudianteId);

    // Consultas finalizadas/pendientes por periodo filtradas por monitor.
    @Query(value = """
                SELECT COUNT(*) FILTER (WHERE c.estado = 'CERRADO') AS finished_consultas,
                       COUNT(*) FILTER (WHERE c.estado <> 'CERRADO' AND c.estado <> 'ARCHIVADO') AS unfinished_consultas
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= :fechaInicio
                AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                AND c.monitor_id = :monitorId
                """, nativeQuery = true)
    List<Object[]> contarFinalizadasYPendientesPorPeriodoYMonitor(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("monitorId") Long monitorId);

    // Personas atendidas por periodo filtradas por asesor.
    @Query(value = """
                WITH consultas_semestre AS (
                    SELECT c.id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    AND c.asesor_id = :asesorId
                )
                SELECT (SELECT COUNT(*) FROM consultas_semestre)
                    + (SELECT COUNT(*) FROM "DB_consultorioJuridico".consulta_parte cp
                       WHERE cp.consulta_id IN (SELECT id FROM consultas_semestre))
                AS total_personas_atendidas
                """, nativeQuery = true)
    List<Object[]> contarPersonasAtendidasPorPeriodoYAsesor(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("asesorId") Long asesorId);

    // Personas atendidas por periodo filtradas por estudiante.
    @Query(value = """
                WITH consultas_semestre AS (
                    SELECT c.id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.estudiante_id = :estudianteId
                    AND c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                SELECT (SELECT COUNT(*) FROM consultas_semestre)
                    + (SELECT COUNT(*) FROM "DB_consultorioJuridico".consulta_parte cp
                       WHERE cp.consulta_id IN (SELECT id FROM consultas_semestre))
                AS total_personas_atendidas
                """, nativeQuery = true)
    List<Object[]> contarPersonasAtendidasPorPeriodoYEstudiante(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("estudianteId") Long estudianteId);

    // Personas atendidas por periodo filtradas por monitor.
    @Query(value = """
                WITH consultas_semestre AS (
                    SELECT c.id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    AND c.monitor_id = :monitorId
                )
                SELECT (SELECT COUNT(*) FROM consultas_semestre)
                    + (SELECT COUNT(*) FROM "DB_consultorioJuridico".consulta_parte cp
                       WHERE cp.consulta_id IN (SELECT id FROM consultas_semestre))
                AS total_personas_atendidas
                """, nativeQuery = true)
    List<Object[]> contarPersonasAtendidasPorPeriodoYMonitor(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("monitorId") Long monitorId);


    // Consultas agrupadas por estado en el semestre.
    @Query(value = """
                SELECT c.estado, COUNT(c.id) AS total
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= :fechaInicio
                AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                GROUP BY c.estado ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarConsultasPorEstadoPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Consultas agrupadas por tipo de violencia en el semestre.
    @Query(value = """
                SELECT COALESCE(c.tipo_violencia, 'No aplica') AS tipo, COUNT(c.id) AS total
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= :fechaInicio
                AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                GROUP BY tipo ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarConsultasPorTipoViolenciaPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Personas atendidas por género en el semestre.
    @Query(value = """
                SELECT p.genero, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.genero ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorGeneroPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Personas atendidas por estrato en el semestre.
    @Query(value = """
                SELECT p.estrato, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.estrato ORDER BY p.estrato
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorEstratoPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Personas atendidas por zona en el semestre (incluye partes adicionales).
    @Query(value = """
                SELECT p.zona, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.zona ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorZonaPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Personas atendidas por grupo étnico en el semestre (incluye partes adicionales).
    @Query(value = """
                SELECT p.grupo_etnico, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.grupo_etnico ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorGrupoEtnicoPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Personas atendidas por municipio en el semestre (incluye partes adicionales).
    @Query(value = """
                SELECT m.nombre, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                JOIN "DB_consultorioJuridico".municipio m ON m.id = p.municipio_id
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY m.nombre ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorMunicipioPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    // Personas atendidas por condición en el semestre (incluye partes adicionales).
    @Query(value = """
                SELECT co.nombre, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                JOIN "DB_consultorioJuridico".condicion co ON co.id = p.condicion_actual_id
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= :fechaInicio
                    AND c.fecha <= :fechaFin
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY co.nombre ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorCondicionPorPeriodo(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);


    // =====================================================
    // QUERIES POR RANGO LIBRE DE FECHAS
    // =====================================================

    @Query(value = """
                SELECT COUNT(*) FILTER (WHERE c.estado = 'CERRADO') AS finished_consultas,
                       COUNT(*) FILTER (WHERE c.estado <> 'CERRADO' AND c.estado <> 'ARCHIVADO') AS unfinished_consultas
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= CAST(:fechaInicio AS date)
                AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                """, nativeQuery = true)
    List<Object[]> contarFinalizadasYPendientesPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT COUNT(DISTINCT persona_id) AS total_personas_atendidas
                FROM (
                    SELECT c.persona_id
                    FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id
                    FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                ) AS personas_unicas
                """, nativeQuery = true)
    List<Object[]> contarPersonasAtendidasPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT c.estado, COUNT(c.id) AS total
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= CAST(:fechaInicio AS date)
                AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                GROUP BY c.estado ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarConsultasPorEstadoPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT a.id AS area_id, a.nombre AS area_nombre, COUNT(c.id) AS total_consultas
                FROM "DB_consultorioJuridico".consulta c
                JOIN "DB_consultorioJuridico".area a ON a.id = c.area_id
                WHERE c.fecha >= CAST(:fechaInicio AS date)
                AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                GROUP BY a.id, a.nombre ORDER BY total_consultas DESC
                """, nativeQuery = true)
    List<Object[]> contarConsultasPorAreaPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT COALESCE(c.tipo_violencia, 'No aplica') AS tipo, COUNT(c.id) AS total
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.fecha >= CAST(:fechaInicio AS date)
                AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                GROUP BY tipo ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarConsultasPorTipoViolenciaPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT p.genero, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.genero ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorGeneroPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT p.estrato, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.estrato ORDER BY p.estrato
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorEstratoPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT p.zona, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.zona ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorZonaPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT p.grupo_etnico, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY p.grupo_etnico ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorGrupoEtnicoPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT m.nombre, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                JOIN "DB_consultorioJuridico".municipio m ON m.id = p.municipio_id
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY m.nombre ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorMunicipioPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);

    @Query(value = """
                SELECT co.nombre, COUNT(DISTINCT p.id) AS total
                FROM "DB_consultorioJuridico".persona p
                JOIN "DB_consultorioJuridico".condicion co ON co.id = p.condicion_actual_id
                WHERE p.id IN (
                    SELECT c.persona_id FROM "DB_consultorioJuridico".consulta c
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                    UNION
                    SELECT cp.persona_id FROM "DB_consultorioJuridico".consulta_parte cp
                    JOIN "DB_consultorioJuridico".consulta c ON c.id = cp.consulta_id
                    WHERE c.fecha >= CAST(:fechaInicio AS date)
                    AND c.fecha <= CAST(:fechaFin AS date)
                AND c.estado <> 'ARCHIVADO'
                )
                GROUP BY co.nombre ORDER BY total DESC
                """, nativeQuery = true)
    List<Object[]> contarPersonasPorCondicionPorRango(
            @Param("fechaInicio") String fechaInicio,
            @Param("fechaFin") String fechaFin);


}
