package co.edu.ufps.legal_cases.security.dto.access;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

@Getter
@Setter
public class RolDTO {

    private Long id;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre del rol no puede superar los 50 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    private Boolean activo;

    private TipoPerfilUsuario tipoPerfil;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CodigoRolBase codigoBase;

    // Se usa para crear o actualizar los permisos asociados al rol.
    // Esto porque el backend espera los id de permisos solamente
    private Set<Long> permisoIds;

    // Se usa para responder con la información completa de los permisos.
    // Esto porque el frontend si necesito toda la info de permisos para mostrarla
    // en la interfaz.
    private List<PermisoDTO> permisos;
}