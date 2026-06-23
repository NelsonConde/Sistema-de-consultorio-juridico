package co.edu.ufps.legal_cases.security.dto.account.cambio;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarPerfilAAsesorDTO extends CambiarPerfilBaseDTO {

    // Todo asesor debe quedar asociado a un área.
    @NotNull(message = "El área es obligatoria")
    private Long areaId;
}