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


    // Procesos por estado filtrados por asesor.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE c.asesor_id = :asesorId
                GROUP BY p.estado ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoYAsesor(@Param("asesorId") Long asesorId);

    // Procesos por estado filtrados por estudiante.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE c.estudiante_id = :estudianteId
                GROUP BY p.estado ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoYEstudiante(@Param("estudianteId") Long estudianteId);

    // Procesos por estado filtrados por monitor.
    @Query(value = """
                SELECT p.estado, COUNT(p.id) AS total_procesos
                FROM "DB_consultorioJuridico".proceso p
                JOIN "DB_consultorioJuridico".consulta c ON c.id = p.consulta_id
                WHERE c.monitor_id = :monitorId
                GROUP BY p.estado ORDER BY total_procesos DESC
                """, nativeQuery = true)
    List<Object[]> contarProcesosPorEstadoYMonitor(@Param("monitorId") Long monitorId);


}