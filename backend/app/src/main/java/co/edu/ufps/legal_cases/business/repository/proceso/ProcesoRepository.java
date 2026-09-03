package co.edu.ufps.legal_cases.business.repository.proceso;

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


}
