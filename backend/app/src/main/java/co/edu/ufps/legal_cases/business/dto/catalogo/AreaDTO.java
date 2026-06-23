package co.edu.ufps.legal_cases.business.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AreaDTO {

    private Long id;

    @NotBlank(message = "El nombre del área es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;
    
    private Boolean activo;

}