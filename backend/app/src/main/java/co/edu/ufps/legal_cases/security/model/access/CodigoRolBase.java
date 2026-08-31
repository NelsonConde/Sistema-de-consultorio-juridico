package co.edu.ufps.legal_cases.security.model.access;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

public enum CodigoRolBase {

    ADMINISTRADOR(TipoPerfilUsuario.ADMINISTRATIVO),
    ASESOR(TipoPerfilUsuario.ASESOR),
    ESTUDIANTE(TipoPerfilUsuario.ESTUDIANTE),
    MONITOR(TipoPerfilUsuario.MONITOR),
    CONCILIADOR(TipoPerfilUsuario.CONCILIADOR);

    private final TipoPerfilUsuario tipoPerfil;

    CodigoRolBase(TipoPerfilUsuario tipoPerfil) {
        this.tipoPerfil = tipoPerfil;
    }

    public TipoPerfilUsuario getTipoPerfil() {
        return tipoPerfil;
    }
}