package co.edu.ufps.legal_cases.business.repository.conciliacion;

import java.time.LocalDateTime;

public interface ConciliacionResumenProjection {

    Long getId();

    Long getVersion();

    Long getConsultaId();

    String getConsulta();

    String getEstadoCodigo();

    String getEstadoNombre();

    Long getEstudianteId();

    String getEstudianteNombre();

    Long getConciliadorId();

    String getConciliadorNombre();

    LocalDateTime getFechaCreacion();

    LocalDateTime getFechaConciliacion();

    LocalDateTime getFechaFinalizacion();

    Boolean getActivo();
}
