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
@NoArgsConstructor
@AllArgsConstructor
public class TemaDTO {

    private Long id;

    @NotBlank(message = "El nombre del tema es obligatorio")
    @Size(max = 80, message = "El nombre del tema no puede superar los 80 caracteres")
    private String nombre;

    @NotNull(message = "El área es obligatoria")
    private Long areaId;

    private Boolean activo;

}
