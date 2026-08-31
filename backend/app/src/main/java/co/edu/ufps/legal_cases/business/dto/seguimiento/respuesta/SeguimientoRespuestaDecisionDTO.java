package co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta;

import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// dto para aprobar o rechazar un seguimiento
@Getter
@Setter
public class SeguimientoRespuestaDecisionDTO {

    @NotNull(message = "El estado de la respuesta es obligatorio")
    private EstadoRespuestaSeguimiento estado;

    @Size(max = 500, message = "La observación de revisión no puede superar 500 caracteres")
    private String observacionRevision;

    // PENDIENTE solo se usa al crear la respuesta.
    // En revisión solo se permite aprobar o rechazar.
    @AssertTrue(message = "La decisión debe ser APROBADA o RECHAZADA")
    public boolean isDecisionValida() {
        if (estado == null) {
            return true;
        }

        return estado == EstadoRespuestaSeguimiento.APROBADA
                || estado == EstadoRespuestaSeguimiento.RECHAZADA;
    }
}