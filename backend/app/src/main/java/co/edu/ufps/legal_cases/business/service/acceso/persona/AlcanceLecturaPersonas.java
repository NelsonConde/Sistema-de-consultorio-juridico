package co.edu.ufps.legal_cases.business.service.acceso.persona;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

public record AlcanceLecturaPersonas(
        boolean esGlobal,
        TipoPerfilUsuario tipoPerfil,
        Long perfilId) {

    public static AlcanceLecturaPersonas global() {
        return new AlcanceLecturaPersonas(true, null, null);
    }

    public static AlcanceLecturaPersonas restringido(
            TipoPerfilUsuario tipoPerfil,
            Long perfilId) {
        return new AlcanceLecturaPersonas(false, tipoPerfil, perfilId);
    }
}
