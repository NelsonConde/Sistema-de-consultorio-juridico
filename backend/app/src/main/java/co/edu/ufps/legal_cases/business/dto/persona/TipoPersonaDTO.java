package co.edu.ufps.legal_cases.business.dto.persona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoPersonaDTO {

    private Long id;

    @NotBlank(message = "El nombre del tipo de persona es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    private Boolean activo;
}