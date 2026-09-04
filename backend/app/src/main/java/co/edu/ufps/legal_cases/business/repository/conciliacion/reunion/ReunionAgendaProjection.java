package co.edu.ufps.legal_cases.business.repository.conciliacion.reunion;

import java.time.LocalDateTime;

public interface ReunionAgendaProjection {

    Long getConciliacionId();

    Long getConsultaId();

    String getEstadoCodigo();

    LocalDateTime getFechaReunion();
}
