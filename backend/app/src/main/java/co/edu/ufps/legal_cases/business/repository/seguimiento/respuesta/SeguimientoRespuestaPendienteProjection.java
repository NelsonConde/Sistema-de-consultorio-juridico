package co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;

public interface SeguimientoRespuestaPendienteProjection {

    Long getId();

    Long getVersion();

    Long getSeguimientoId();

    Long getConsultaId();

    Long getEstudianteId();

    String getEstudianteNombre();

    String getContenido();

    EstadoRespuestaSeguimiento getEstado();

    Boolean getFueraPlazo();

    String getObservacionRevision();

    Long getRevisadoPorId();

    String getRevisadoPorUsername();

    Boolean getActivo();

    LocalDateTime getFechaCreacion();

    LocalDateTime getFechaActualizacion();

    LocalDateTime getFechaDecision();
}
