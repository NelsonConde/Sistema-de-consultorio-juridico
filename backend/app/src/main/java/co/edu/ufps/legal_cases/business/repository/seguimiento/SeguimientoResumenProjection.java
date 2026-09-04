package co.edu.ufps.legal_cases.business.repository.seguimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;

public interface SeguimientoResumenProjection {

    Long getId();

    Long getVersion();

    String getDescripcion();

    LocalDate getFechaEntrega();

    Integer getDiasNotificacion();

    Boolean getNotificarPartes();

    Boolean getNotificarEstudiante();

    Boolean getAlertaDisciplinaria();

    EstadoSeguimiento getEstado();

    Long getCategoriaSeguimientoId();

    String getCategoriaSeguimientoNombre();

    Long getConsultaId();

    Long getAutorId();

    String getAutorUsername();

    LocalDateTime getFechaCreacion();

    LocalDateTime getFechaActualizacion();
}
