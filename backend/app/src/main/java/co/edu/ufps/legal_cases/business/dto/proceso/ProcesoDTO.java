package co.edu.ufps.legal_cases.business.dto.proceso;

import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoDTO {

    private Long id;

    // Puede ser nulo mientras el proceso permanezca PENDIENTE.
    // Para estados finales se valida en ProcesoValidator.
    private String numeroRadicado;

    @NotNull(message = "El departamento es obligatorio")
    private Long departamentoId;

    @NotNull(message = "La consulta es obligatoria")
    private Long consultaId;

    private Long especialidadId;

    private Long organoControlId;

    private EstadoProceso estado;

    private Boolean activo;
}