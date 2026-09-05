package co.edu.ufps.legal_cases.security.repository.account;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

public interface UsuarioSistemaResumenProjection {

    Long getId();

    String getUsername();

    Boolean getActivo();

    Long getRolId();

    String getRolNombre();

    TipoPerfilUsuario getTipoPerfil();
}
