package co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// dto para crear o editar una respuesta a seguimiento
@Getter
@Setter
public class SeguimientoRespuestaRequestDTO {

    private Long id;

    @NotBlank(message = "La respuesta del seguimiento es obligatoria")
    @Size(max = 1000, message = "La respuesta del seguimiento no puede superar 1000 caracteres")
    private String contenido;
}