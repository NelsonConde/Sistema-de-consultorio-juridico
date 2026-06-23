package co.edu.ufps.legal_cases.security.service.access.rol;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.access.RolDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;

// Valida reglas locales de Rol.
// Las consultas a base de datos quedan en RolService.
@Component
public class RolValidator {

    public void validarCreacion(RolDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Rol rol, RolDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), rol.getId())) {
            throw new BusinessException("No se permite cambiar el id del rol");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null) {
            throw new BusinessException("El nombre del rol es obligatorio");
        }

        return nombreNormalizado;
    }

    public String normalizarDescripcion(String descripcion) {
        return normalizarTexto(descripcion);
    }

    public void validarCambioEstado(Rol rol, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado del rol es obligatorio");
        }

        if (Objects.equals(rol.getActivo(), activo)) {
            throw new BusinessException("El rol ya tiene ese estado");
        }
    }

    public void validarPermisoIdObligatorio(Long permisoId) {
        if (permisoId == null) {
            throw new BusinessException("El permiso es obligatorio");
        }
    }

    public void validarListaPermisos(Set<Long> permisoIds) {
        if (permisoIds == null || permisoIds.isEmpty()) {
            return;
        }

        if (permisoIds.contains(null)) {
            throw new BusinessException("La lista de permisos contiene valores nulos");
        }
    }

    public void validarPermisoNoAsignado(Rol rol, Permiso permiso) {
        boolean yaAsignado = rol.getPermisos()
                .stream()
                .anyMatch(p -> Objects.equals(p.getId(), permiso.getId()));

        if (yaAsignado) {
            throw new BusinessException("El permiso ya está asignado al rol");
        }
    }

    public void validarPermisoAsignado(boolean eliminado) {
        if (!eliminado) {
            throw new BusinessException("El permiso no está asignado al rol");
        }
    }

    public void validarExistenCambios(
            Rol rol,
            String nombreNuevo,
            String descripcionNueva,
            Boolean activoNuevo,
            Set<Permiso> permisosNuevos) {

        boolean sinCambios = equalsIgnoreCase(rol.getNombre(), nombreNuevo)
                && equalsIgnoreCase(rol.getDescripcion(), descripcionNueva)
                && Objects.equals(rol.getActivo(), activoNuevo)
                && mismosPermisos(rol.getPermisos(), permisosNuevos);

        if (sinCambios) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    private boolean mismosPermisos(Set<Permiso> actuales, Set<Permiso> nuevos) {
        Set<Long> idsActuales = actuales.stream()
                .map(Permiso::getId)
                .collect(Collectors.toSet());

        Set<Long> idsNuevos = nuevos.stream()
                .map(Permiso::getId)
                .collect(Collectors.toSet());

        return Objects.equals(idsActuales, idsNuevos);
    }

    private void validarDtoObligatorio(RolDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del rol son obligatorios");
        }
    }
}