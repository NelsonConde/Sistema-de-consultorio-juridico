package co.edu.ufps.legal_cases.business.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TipoDocumentoDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 10, message = "El nombre no puede tener más de 10 caracteres")
    private String nombre;

    private Boolean activo;
}