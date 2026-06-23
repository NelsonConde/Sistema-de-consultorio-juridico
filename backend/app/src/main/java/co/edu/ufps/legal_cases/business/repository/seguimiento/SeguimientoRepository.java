package co.edu.ufps.legal_cases.business.repository.seguimiento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

        // Lista seguimientos activos marcados como alerta disciplinaria.
        List<Seguimiento> findByAlertaDisciplinariaTrueAndActivoTrueOrderByFechaCreacionDesc();

        // Lista alertas disciplinarias activas,
        // excluyendo consultas archivadas.
        List<Seguimiento> findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta estado);

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

        // Total de seguimientos en el semestre.
        @Query(value = """
                        SELECT COUNT(s.id) AS total
                        FROM "DB_consultorioJuridico".seguimiento s
                        WHERE s.fecha_creacion >=
                            CASE WHEN :semester = 1 THEN make_date(:year, 1, 1) ELSE make_date(:year, 7, 1) END
                        AND s.fecha_creacion <
                            CASE WHEN :semester = 1 THEN make_date(:year, 7, 1) ELSE make_date(:year + 1, 1, 1) END
                        AND s.activo = true
                        """, nativeQuery = true)
        List<Object[]> contarSeguimientosPorSemestre(
                        @Param("year") int year, @Param("semester") int semester);

        // Seguimientos agrupados por estado en el semestre.
        @Query(value = """
                        SELECT s.estado, COUNT(s.id) AS total
                        FROM "DB_consultorioJuridico".seguimiento s
                        WHERE s.fecha_creacion >=
                            CASE WHEN :semester = 1 THEN make_date(:year, 1, 1) ELSE make_date(:year, 7, 1) END
                        AND s.fecha_creacion <
                            CASE WHEN :semester = 1 THEN make_date(:year, 7, 1) ELSE make_date(:year + 1, 1, 1) END
                        AND s.activo = true
                        GROUP BY s.estado ORDER BY total DESC
                        """, nativeQuery = true)
        List<Object[]> contarSeguimientosPorEstadoPorSemestre(
                        @Param("year") int year, @Param("semester") int semester);

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