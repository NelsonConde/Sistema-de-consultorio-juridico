package co.edu.ufps.legal_cases.business.dto.catalogo;

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
public class NacionalidadDTO {

    private Long id;

    @NotBlank(message = "El nombre de la nacionalidad es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    private Boolean activo;
}