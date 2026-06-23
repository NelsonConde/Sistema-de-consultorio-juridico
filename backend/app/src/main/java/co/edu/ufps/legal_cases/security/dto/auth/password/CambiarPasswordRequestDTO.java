package co.edu.ufps.legal_cases.security.dto.auth.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarPasswordRequestDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
    private String passwordNueva;
}