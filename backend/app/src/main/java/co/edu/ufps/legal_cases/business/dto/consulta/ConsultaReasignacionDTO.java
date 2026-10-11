package co.edu.ufps.legal_cases.business.dto.consulta;

import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultaReasignacionDTO {

    @NotNull(message = "El tipo de responsable es obligatorio")
    private TipoResponsableConsulta tipoResponsable;

    @NotNull(message = "El nuevo responsable es obligatorio")
    @Positive(message = "El identificador del nuevo responsable debe ser positivo")
    private Long nuevoResponsableId;

    @NotBlank(message = "El motivo de la reasignacion es obligatorio")
    private String motivo;

    @NotNull(message = "La version de la consulta es obligatoria")
    @PositiveOrZero(message = "La version de la consulta no puede ser negativa")
    private Long version;
}
