package co.edu.ufps.legal_cases.business.service.catalogo.area;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.AreaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class AreaValidator {

    private final AreaRepository areaRepository;

    public AreaValidator(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    public void validarCreacion(AreaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, AreaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del área");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del área es obligatorio");
        }

        if (nombreNormalizado.length() > 50) {
            throw new BusinessException("El nombre no puede superar los 50 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (areaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un área con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un área con ese nombre");
        }
    }

    public void validarExistenCambios(Area area, String nombreNuevo) {
        if (Objects.equals(area.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Area area, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(area.getActivo(), activo)) {
            throw new BusinessException("El área ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(AreaDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del área son obligatorios");
        }
    }
}