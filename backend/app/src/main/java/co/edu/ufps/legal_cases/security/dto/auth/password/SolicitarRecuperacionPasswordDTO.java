package co.edu.ufps.legal_cases.security.dto.auth.password;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Este DTO se usa para hacer la solicitud de recuperar contraseña
@Getter
@Setter
public class SolicitarRecuperacionPasswordDTO {

    @JsonAlias("email")
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String username;
}