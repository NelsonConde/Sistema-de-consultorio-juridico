package co.edu.ufps.legal_cases.business.repository.consulta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import co.edu.ufps.legal_cases.business.model.consulta.ConsultaReasignacionHistorial;

/**
 * Repositorio deliberadamente limitado a inserción y lectura paginada del
 * historial. No expone operaciones ordinarias de actualización o borrado.
 */
public interface ConsultaReasignacionHistorialRepository
        extends Repository<ConsultaReasignacionHistorial, Long> {

    ConsultaReasignacionHistorial save(ConsultaReasignacionHistorial historial);

    @Query(
            value = """
                    SELECT historial.id AS "id",
                           historial.tipo_responsable AS "tipoResponsable",
                           CASE historial.tipo_responsable
                               WHEN 'ESTUDIANTE' THEN estudiante_anterior.nombre
                               WHEN 'ASESOR' THEN asesor_anterior.nombre
                               WHEN 'MONITOR' THEN monitor_anterior.nombre
                           END AS "responsableAnteriorNombre",
                           CASE historial.tipo_responsable
                               WHEN 'ESTUDIANTE' THEN estudiante_nuevo.nombre
                               WHEN 'ASESOR' THEN asesor_nuevo.nombre
                               WHEN 'MONITOR' THEN monitor_nuevo.nombre
                           END AS "responsableNuevoNombre",
                           historial.motivo AS "motivo",
                           COALESCE(
                               CASE actor.tipo_perfil_actual
                                   WHEN 'ESTUDIANTE' THEN actor_estudiante.nombre
                                   WHEN 'ASESOR' THEN actor_asesor.nombre
                                   WHEN 'MONITOR' THEN actor_monitor.nombre
                                   WHEN 'ADMINISTRATIVO' THEN actor_administrativo.nombre
                                   WHEN 'CONCILIADOR' THEN actor_conciliador.nombre
                               END,
                               'Usuario del sistema'
                           ) AS "actorNombre",
                           historial.ocurrido_en AS "ocurridoEn"
                    FROM "DB_consultorioJuridico".consulta_reasignacion_historial historial
                    LEFT JOIN "DB_consultorioJuridico".estudiante estudiante_anterior
                           ON historial.tipo_responsable = 'ESTUDIANTE'
                          AND estudiante_anterior.id = historial.responsable_anterior_id
                    LEFT JOIN "DB_consultorioJuridico".asesor asesor_anterior
                           ON historial.tipo_responsable = 'ASESOR'
                          AND asesor_anterior.id = historial.responsable_anterior_id
                    LEFT JOIN "DB_consultorioJuridico".monitor monitor_anterior
                           ON historial.tipo_responsable = 'MONITOR'
                          AND monitor_anterior.id = historial.responsable_anterior_id
                    LEFT JOIN "DB_consultorioJuridico".estudiante estudiante_nuevo
                           ON historial.tipo_responsable = 'ESTUDIANTE'
                          AND estudiante_nuevo.id = historial.responsable_nuevo_id
                    LEFT JOIN "DB_consultorioJuridico".asesor asesor_nuevo
                           ON historial.tipo_responsable = 'ASESOR'
                          AND asesor_nuevo.id = historial.responsable_nuevo_id
                    LEFT JOIN "DB_consultorioJuridico".monitor monitor_nuevo
                           ON historial.tipo_responsable = 'MONITOR'
                          AND monitor_nuevo.id = historial.responsable_nuevo_id
                    LEFT JOIN "DB_consultorioJuridico".usuario_sistema actor
                           ON actor.id = historial.actor_usuario_id
                    LEFT JOIN "DB_consultorioJuridico".estudiante actor_estudiante
                           ON actor_estudiante.usuario_sistema_id = actor.id
                    LEFT JOIN "DB_consultorioJuridico".asesor actor_asesor
                           ON actor_asesor.usuario_sistema_id = actor.id
                    LEFT JOIN "DB_consultorioJuridico".monitor actor_monitor
                           ON actor_monitor.usuario_sistema_id = actor.id
                    LEFT JOIN "DB_consultorioJuridico".administrativo actor_administrativo
                           ON actor_administrativo.usuario_sistema_id = actor.id
                    LEFT JOIN "DB_consultorioJuridico".conciliador actor_conciliador
                           ON actor_conciliador.usuario_sistema_id = actor.id
                    WHERE historial.consulta_id = :consultaId
                    ORDER BY historial.ocurrido_en DESC, historial.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM "DB_consultorioJuridico".consulta_reasignacion_historial historial
                    WHERE historial.consulta_id = :consultaId
                    """,
            nativeQuery = true)
    Page<ConsultaReasignacionHistorialResumenProjection> buscarResumenPorConsultaId(
            @Param("consultaId") Long consultaId,
            Pageable pageable);
}
