package co.edu.ufps.legal_cases.security.dto.account;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioSistemaResumenDTO {

    private Long id;

    private String username;

    private Boolean activo;

    private Long rolId;

    private String rolNombre;

    private TipoPerfilUsuario tipoPerfil;
}
