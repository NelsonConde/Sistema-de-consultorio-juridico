package co.edu.ufps.legal_cases.business.dto.proceso;

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
public class OrganoControlDTO {

    private Long id;

    @NotBlank(message = "El nombre del organo de control es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    private Boolean activo;
}