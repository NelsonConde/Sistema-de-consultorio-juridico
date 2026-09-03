package co.edu.ufps.legal_cases.business.repository.conciliacion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

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