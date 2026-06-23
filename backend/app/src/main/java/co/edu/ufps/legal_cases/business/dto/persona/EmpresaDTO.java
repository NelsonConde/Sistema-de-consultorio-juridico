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
public class EmpresaDTO {

    private Long id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private Boolean activo;
}