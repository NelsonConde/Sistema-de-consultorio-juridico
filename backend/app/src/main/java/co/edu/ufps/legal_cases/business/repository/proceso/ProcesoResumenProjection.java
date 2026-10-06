package co.edu.ufps.legal_cases.business.repository.proceso;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;

public interface ProcesoResumenProjection {

    Long getId();

    Long getVersion();

    String getNumeroRadicado();

    Long getDepartamentoId();

    String getDepartamentoNombre();

    Long getConsultaId();

    String getConsulta();

    Long getOrganoControlId();

    String getOrganoControlNombre();

    Long getEspecialidadId();

    String getEspecialidadNombre();

    EstadoProceso getEstado();

    Boolean getActivo();

    LocalDateTime getFechaCreacion();
}
