package co.edu.ufps.legal_cases.security.dto.auth.login;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    //Es que va a traer los datos del frontend al backend para autenticar al usuario
    @NotBlank(message = "El correo es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}