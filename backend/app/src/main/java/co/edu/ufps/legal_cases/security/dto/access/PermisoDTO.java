package co.edu.ufps.legal_cases.security.dto.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermisoDTO {

    private Long id;

    @NotBlank(message = "El nombre del permiso es obligatorio")
    @Size(max = 100, message = "El nombre del permiso no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    private Boolean activo;
}