package co.edu.ufps.legal_cases.business.dto.seguimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaSeguimientoDTO {

    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 50, message = "El nombre de la categoría no puede superar 50 caracteres")
    private String nombre;

    // Si viene null, el service lo asumirá como true en creación.
    private Boolean activo;
}