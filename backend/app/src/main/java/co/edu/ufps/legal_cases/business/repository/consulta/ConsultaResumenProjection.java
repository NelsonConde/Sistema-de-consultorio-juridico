package co.edu.ufps.legal_cases.business.repository.consulta;

import java.time.LocalDate;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;

public interface ConsultaResumenProjection {

    Long getId();

    Long getVersion();

    String getConsulta();

    LocalDate getFecha();

    String getNombre();

    String getApellido();

    String getCedula();

    EstadoConsulta getEstado();
}
