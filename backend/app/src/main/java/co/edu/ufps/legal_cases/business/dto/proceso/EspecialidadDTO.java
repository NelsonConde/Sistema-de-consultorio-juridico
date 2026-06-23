package co.edu.ufps.legal_cases.business.dto.proceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EspecialidadDTO {

    private Long id;

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    @NotNull(message = "El órgano de control es obligatorio")
    private Long organoControlId;

    private Boolean activo;
}