package co.edu.ufps.legal_cases.business.dto.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstudianteDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotNull(message = "El tipo de documento es obligatorio")
    private Long tipoDocumentoId;

    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
    private String documento;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefono;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no puede superar los 50 caracteres")
    private String usuario;

    @NotNull(message = "La sede es obligatoria")
    private Long sedeId;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede superar los 30 caracteres")
    private String codigo;

    @NotNull(message = "El asesor es obligatorio")
    private Long asesorId;

    private Boolean activo;

    private Boolean conciliacion;
}