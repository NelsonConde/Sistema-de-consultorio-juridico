package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Strategy para resolver el perfil activo asociado a un UsuarioSistema.
// Cada tipo de perfil sabe en qué repositorio debe buscar.
public interface PerfilUsuarioActivoResolver {

    TipoPerfilUsuario getTipoPerfil();

    PerfilUsuarioActual resolver(Long usuarioSistemaId);
}