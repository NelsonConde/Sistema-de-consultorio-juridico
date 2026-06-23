package co.edu.ufps.legal_cases.security.service.access.permiso;

import static co.edu.ufps.legal_cases.common.util.ComparacionUtils.equalsIgnoreCase;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.access.PermisoDTO;
import co.edu.ufps.legal_cases.security.model.access.Permiso;

// Valida reglas locales de Permiso.
// No consulta base de datos; las validaciones de existencia quedan en el service.
@Component
public class PermisoValidator {

    public void validarCreacion(PermisoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Permiso permiso, PermisoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), permiso.getId())) {
            throw new BusinessException("No se permite cambiar el id del permiso");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null) {
            throw new BusinessException("El nombre del permiso es obligatorio");
        }

        return nombreNormalizado;
    }

    public String normalizarDescripcion(String descripcion) {
        return normalizarTexto(descripcion);
    }

    public void validarCambioEstado(Permiso permiso, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(permiso.getActivo(), activo)) {
            throw new BusinessException("El permiso ya tiene ese estado");
        }
    }

    public void validarExistenCambios(
            Permiso permiso,
            String nombreNuevo,
            String descripcionNueva,
            Boolean activoNuevo) {

        boolean sinCambios = equalsIgnoreCase(permiso.getNombre(), nombreNuevo)
                && equalsIgnoreCase(permiso.getDescripcion(), descripcionNueva)
                && Objects.equals(permiso.getActivo(), activoNuevo);

        if (sinCambios) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    private void validarDtoObligatorio(PermisoDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del permiso son obligatorios");
        }
    }
}