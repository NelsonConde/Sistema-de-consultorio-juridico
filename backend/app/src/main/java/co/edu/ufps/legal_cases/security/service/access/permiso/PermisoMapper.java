package co.edu.ufps.legal_cases.security.service.access.permiso;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.dto.access.PermisoDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;

// Centraliza la conversión entre Permiso y PermisoDTO.
// El service no debe conocer detalles de mapeo.
@Component
public class PermisoMapper {

    public PermisoDTO convertirADTO(Permiso permiso) {
        PermisoDTO dto = new PermisoDTO();

        dto.setId(permiso.getId());
        dto.setNombre(permiso.getNombre());
        dto.setDescripcion(permiso.getDescripcion());
        dto.setActivo(permiso.getActivo());

        return dto;
    }

    public void aplicarDatos(
            Permiso permiso,
            String nombre,
            String descripcion,
            Boolean activo) {

        permiso.setNombre(nombre);
        permiso.setDescripcion(descripcion);
        permiso.setActivo(activo);
    }
}