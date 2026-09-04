package co.edu.ufps.legal_cases.business.repository.conciliacion.reunion;

import java.time.LocalDateTime;

public interface ReunionConciliacionResumenProjection {

    Long getConciliacionId();

    Long getVersion();

    Long getConciliacionVersion();

    Long getConsultaId();

    String getEstadoCodigo();

    String getEstadoNombre();

    Long getSedeId();

    String getSedeNombre();

    LocalDateTime getFechaReunion();

    String getObservaciones();

    Long getEstudianteId();

    String getEstudianteNombre();

    Long getConciliadorId();

    String getConciliadorNombre();

    LocalDateTime getFechaCreacion();

    LocalDateTime getFechaActualizacion();
}
