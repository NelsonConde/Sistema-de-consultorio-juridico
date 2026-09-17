package co.edu.ufps.legal_cases.security.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Solo contiene los campos que el propio usuario puede editar.
// A propósito NO incluye nombre, sede, rol ni codigo: eso evita que un
// cliente malicioso intente actualizar campos restringidos enviándolos en
// el body, ya que el DTO de entrada simplemente no los conoce.
@Getter
@Setter
public class ActualizarContactoDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,30}$", message = "El teléfono no tiene un formato válido")
    private String telefono;
}