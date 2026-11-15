package co.edu.ufps.legal_cases.security.repository.account;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;

public interface UsuarioSistemaResumenProjection {

    Long getId();

    String getUsername();

    Boolean getActivo();

    EstadoActivacionCuenta getEstadoActivacion();

    Long getRolId();

    String getRolNombre();

    TipoPerfilUsuario getTipoPerfil();
}
