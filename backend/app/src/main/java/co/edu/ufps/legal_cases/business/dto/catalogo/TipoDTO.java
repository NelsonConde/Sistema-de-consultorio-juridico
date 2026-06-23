package co.edu.ufps.legal_cases.business.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TipoDTO {

    private Long id;

    @NotBlank(message = "El nombre del tipo es obligatorio")
    @Size(max = 80, message = "El nombre del tipo no puede superar los 80 caracteres")
    private String nombre;

    @NotNull(message = "El tema es obligatorio")
    private Long temaId;
    
    private Boolean activo;
}