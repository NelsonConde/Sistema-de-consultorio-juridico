package co.edu.ufps.legal_cases.business.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SedeDTO {

    private Long id;

    @NotBlank(message = "El nombre de la sede es obligatorio")
    @Size(max = 100, message = "El nombre de la sede no puede superar los 100 caracteres")
    private String nombre;

    private Boolean activo;
}