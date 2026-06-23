package co.edu.ufps.legal_cases.security.dto.account.cambio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Clase base para los DTOs de cambio de perfil.
// Contiene los datos comunes que comparten estudiante, asesor, monitor,
// administrativo y conciliador.
@Getter
@Setter
public class CambiarPerfilBaseDTO {

    // Rol que tendrá el usuario después del cambio.
    @NotNull(message = "El rol destino es obligatorio")
    private Long rolId;

    // Motivo del cambio. Se guarda en historial.
    @NotBlank(message = "El motivo del cambio es obligatorio")
    @Size(max = 255, message = "El motivo del cambio no puede superar 255 caracteres")
    private String motivo;

    // Campos comunes entre perfiles.
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    private String nombre;

    // Algunos perfiles lo exigen y otros lo permiten opcional.
    // Por eso la obligatoriedad se valida en el handler correspondiente.
    private Long tipoDocumentoId;

    // Algunos perfiles lo exigen y otros lo permiten opcional.
    // Por eso la obligatoriedad se valida en el handler correspondiente.
    @Size(max = 30, message = "El documento no puede superar 30 caracteres")
    private String documento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 100, message = "El usuario no puede superar 100 caracteres")
    private String usuario;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    private String codigo;

    // Algunos perfiles lo exigen y otros lo permiten opcional.
    // Por eso la obligatoriedad se valida en el handler correspondiente.
    private Long sedeId;
}