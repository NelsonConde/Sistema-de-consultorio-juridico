package co.edu.ufps.legal_cases.business.repository.seguimiento;

import java.time.LocalDate;

import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;

/**
 * Projection ligera para consumo de Agenda.
 * Solo incluye los campos necesarios para construir AgendaEventDTO.
 */
public interface SeguimientoAgendaProjection {

    Long getId();

    Long getConsultaId();

    String getDescripcion();

    EstadoSeguimiento getEstado();

    LocalDate getFechaEntrega();
}
