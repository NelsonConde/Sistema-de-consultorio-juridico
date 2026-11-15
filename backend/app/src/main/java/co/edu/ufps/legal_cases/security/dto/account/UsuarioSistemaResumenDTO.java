package co.edu.ufps.legal_cases.security.dto.account;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioSistemaResumenDTO {

    private Long id;

    private String username;

    private Boolean activo;

    private EstadoActivacionCuenta estadoActivacion;

    private Long rolId;

    private String rolNombre;

    private TipoPerfilUsuario tipoPerfil;
}
