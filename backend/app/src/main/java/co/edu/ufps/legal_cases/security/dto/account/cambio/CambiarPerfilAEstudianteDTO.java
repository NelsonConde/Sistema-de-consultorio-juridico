package co.edu.ufps.legal_cases.security.dto.account.cambio;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarPerfilAEstudianteDTO extends CambiarPerfilBaseDTO {

    // Todo estudiante debe estar asociado a un asesor activo.
    @NotNull(message = "El asesor es obligatorio")
    private Long asesorId;

    private Boolean conciliacion;
}