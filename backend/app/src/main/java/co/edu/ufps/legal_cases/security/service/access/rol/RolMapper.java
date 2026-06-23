package co.edu.ufps.legal_cases.security.service.access.rol;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.dto.access.PermisoDTO;
import co.edu.ufps.legal_cases.security.dto.access.RolDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.service.access.permiso.PermisoMapper;

// Centraliza la conversión entre Rol y RolDTO.
// Mantiene fuera del service los detalles de exposición de permisos.
@Component
public class RolMapper {

    private final PermisoMapper permisoMapper;

    public RolMapper(PermisoMapper permisoMapper) {
        this.permisoMapper = permisoMapper;
    }

    public RolDTO convertirADTO(Rol rol) {
        RolDTO dto = new RolDTO();

        dto.setId(rol.getId());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        dto.setActivo(rol.getActivo());

        Set<Long> permisoIds = rol.getPermisos()
                .stream()
                .map(Permiso::getId)
                .collect(Collectors.toSet());

        List<PermisoDTO> permisos = rol.getPermisos()
                .stream()
                .sorted(Comparator.comparing(Permiso::getNombre))
                .map(permisoMapper::convertirADTO)
                .toList();

        dto.setPermisoIds(permisoIds);
        dto.setPermisos(permisos);

        return dto;
    }

    public void aplicarDatos(
            Rol rol,
            String nombre,
            String descripcion,
            Boolean activo,
            Set<Permiso> permisos) {

        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setActivo(activo);

        if (permisos != null) {
            rol.setPermisos(permisos);
        }
    }
}