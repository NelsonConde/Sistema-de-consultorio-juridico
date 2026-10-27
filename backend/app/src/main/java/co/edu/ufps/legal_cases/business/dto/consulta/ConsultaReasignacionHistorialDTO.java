package co.edu.ufps.legal_cases.business.dto.consulta;

import java.time.Instant;

import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;

/**
 * Evento legible del historial de responsables de una consulta.
 *
 * <p>Solo conserva el identificador técnico del evento. Los identificadores
 * internos de la consulta, responsables y actor no forman parte de la
 * respuesta porque el consumidor necesita sus nombres, no claves de base de
 * datos.
 */
public record ConsultaReasignacionHistorialDTO(
        Long id,
        TipoResponsableConsulta tipoResponsable,
        String responsableAnteriorNombre,
        String responsableNuevoNombre,
        String motivo,
        String actorNombre,
        Instant ocurridoEn) {
}
