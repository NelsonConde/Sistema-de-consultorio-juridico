package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Strategy para manejar operaciones sobre el perfil actual del usuario.
// Cada tipo de perfil decide cómo desactivarse sin que PerfilEstadoService use switch.
public interface PerfilEstadoHandler {

    TipoPerfilUsuario getTipoPerfil();

    void desactivarPerfilActual(Long usuarioSistemaId);
}