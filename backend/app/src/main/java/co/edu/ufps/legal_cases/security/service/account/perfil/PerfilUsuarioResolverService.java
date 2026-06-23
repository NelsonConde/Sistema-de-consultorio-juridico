package co.edu.ufps.legal_cases.security.service.account.perfil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.resolver.PerfilUsuarioActivoResolverRegistry;

// Resuelve el perfil activo del usuario autenticado.
// La búsqueda específica por tipo de perfil se delega a estrategias.
@Service
@Transactional(readOnly = true, noRollbackFor = BusinessException.class)
public class PerfilUsuarioResolverService {

    private final PerfilUsuarioActivoResolverRegistry perfilUsuarioActivoResolverRegistry;

    public PerfilUsuarioResolverService(
            PerfilUsuarioActivoResolverRegistry perfilUsuarioActivoResolverRegistry) {
        this.perfilUsuarioActivoResolverRegistry = perfilUsuarioActivoResolverRegistry;
    }

    public PerfilUsuarioActual obtenerPerfilActivoObligatorio(UsuarioSistema usuario) {
        validarUsuario(usuario);

        TipoPerfilUsuario tipoPerfil = usuario.getTipoPerfilActual();

        if (tipoPerfil == null) {
            throw new BusinessException("El usuario del sistema no tiene tipo de perfil actual definido");
        }

        return perfilUsuarioActivoResolverRegistry
                .obtenerResolver(tipoPerfil)
                .resolver(usuario.getId());
    }

    public boolean tienePerfilActivo(UsuarioSistema usuario) {
        try {
            obtenerPerfilActivoObligatorio(usuario);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void validarUsuario(UsuarioSistema usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new BusinessException("El usuario del sistema es obligatorio para resolver el perfil");
        }
    }
}