package co.edu.ufps.legal_cases.business.repository.seguimiento;

import java.time.LocalDate;

import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;

public interface SeguimientoAgendaProjection {

    Long getId();

    String getDescripcion();

    LocalDate getFechaEntrega();

    Boolean getNotificarEstudiante();

    Boolean getAlertaDisciplinaria();

    EstadoSeguimiento getEstado();

    Long getConsultaId();

    String getCategoriaSeguimientoNombre();

    Long getAutorId();

    String getAutorUsername();
}
