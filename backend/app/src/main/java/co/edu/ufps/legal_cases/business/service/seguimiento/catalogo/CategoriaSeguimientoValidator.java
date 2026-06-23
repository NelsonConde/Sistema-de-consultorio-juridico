package co.edu.ufps.legal_cases.business.service.seguimiento.catalogo;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.seguimiento.CategoriaSeguimientoDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.CategoriaSeguimientoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class CategoriaSeguimientoValidator {

    private final CategoriaSeguimientoRepository categoriaSeguimientoRepository;

    public CategoriaSeguimientoValidator(CategoriaSeguimientoRepository categoriaSeguimientoRepository) {
        this.categoriaSeguimientoRepository = categoriaSeguimientoRepository;
    }

    public void validarCreacion(CategoriaSeguimientoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, CategoriaSeguimientoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la categoría");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la categoría es obligatorio");
        }

        if (nombreNormalizado.length() > 50) {
            throw new BusinessException("El nombre de la categoría no puede superar 50 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (categoriaSeguimientoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una categoría de seguimiento con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (categoriaSeguimientoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una categoría de seguimiento con ese nombre");
        }
    }

    public void validarExistenCambios(CategoriaSeguimiento categoria, String nombreNuevo) {
        if (Objects.equals(categoria.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(CategoriaSeguimiento categoria, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(categoria.getActivo(), activo)) {
            throw new BusinessException("La categoría ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(CategoriaSeguimientoDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la categoría son obligatorios");
        }
    }
}