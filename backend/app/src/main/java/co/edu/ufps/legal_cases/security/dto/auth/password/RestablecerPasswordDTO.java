package co.edu.ufps.legal_cases.security.dto.auth.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

//Este DTO se usa para recibir el token y la nueva contraeña para reestablecer la contraseña
@Getter
@Setter
public class RestablecerPasswordDTO {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
    private String passwordNueva;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmarPassword;
}