package co.edu.ufps.legal_cases.security.service.account.usuario;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

// Centraliza la conversión entre UsuarioSistema y UsuarioSistemaDTO.
// También resuelve el perfil real activo para exponerlo en la respuesta.
@Component
public class UsuarioSistemaMapper {

    private final PerfilUsuarioResolverService perfilUsuarioResolverService;

    public UsuarioSistemaMapper(PerfilUsuarioResolverService perfilUsuarioResolverService) {
        this.perfilUsuarioResolverService = perfilUsuarioResolverService;
    }

    public UsuarioSistemaDTO convertirADTO(UsuarioSistema usuario) {
        UsuarioSistemaDTO dto = new UsuarioSistemaDTO();

        dto.setId(usuario.getId());
        dto.setUsername(usuario.getUsername());
        dto.setActivo(usuario.getActivo());

        asignarRolYPermisos(dto, usuario);
        asignarPerfil(dto, usuario);

        return dto;
    }

    private void asignarRolYPermisos(UsuarioSistemaDTO dto, UsuarioSistema usuario) {
        if (usuario.getRol() == null) {
            return;
        }

        dto.setRolId(usuario.getRol().getId());
        dto.setRolNombre(usuario.getRol().getNombre());

        List<String> permisos = usuario.getRol().getPermisos()
                .stream()
                .filter(permiso -> Boolean.TRUE.equals(permiso.getActivo()))
                .sorted(Comparator.comparing(Permiso::getNombre))
                .map(Permiso::getNombre)
                .toList();

        dto.setPermisos(permisos);
    }

    // Identifica a qué perfil real pertenece el usuario del sistema.
    // Si no hay perfil activo, se expone SIN_PERFIL sin romper listados administrativos.
    private void asignarPerfil(UsuarioSistemaDTO dto, UsuarioSistema usuario) {
        try {
            PerfilUsuarioActual perfilActual =
                    perfilUsuarioResolverService.obtenerPerfilActivoObligatorio(usuario);

            dto.setPerfilId(perfilActual.getPerfilId());
            dto.setTipoPerfil(perfilActual.getTipoPerfil().name());

        } catch (BusinessException ex) {
            dto.setPerfilId(null);
            dto.setTipoPerfil("SIN_PERFIL");
        }
    }
}