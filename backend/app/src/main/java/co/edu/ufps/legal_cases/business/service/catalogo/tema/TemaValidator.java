package co.edu.ufps.legal_cases.business.service.catalogo.tema;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.TemaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.repository.catalogo.TemaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class TemaValidator {

    private final TemaRepository temaRepository;

    public TemaValidator(TemaRepository temaRepository) {
        this.temaRepository = temaRepository;
    }

    public void validarCreacion(TemaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, TemaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del tema");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del tema es obligatorio");
        }

        if (nombreNormalizado.length() > 80) {
            throw new BusinessException("El nombre del tema no puede superar los 80 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre, Long areaId) {
        if (temaRepository.existsByNombreIgnoreCaseAndAreaId(nombre, areaId)) {
            throw new BusinessException("Ya existe un tema con ese nombre en el área seleccionada");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long areaId, Long id) {
        if (temaRepository.existsByNombreIgnoreCaseAndAreaIdAndIdNot(nombre, areaId, id)) {
            throw new BusinessException("Ya existe un tema con ese nombre en el área seleccionada");
        }
    }

    public void validarExistenCambios(Tema tema, String nombreNuevo, Area areaNueva) {
        boolean mismoNombre = Objects.equals(tema.getNombre(), nombreNuevo);
        boolean mismaArea = tema.getArea() != null
                && Objects.equals(tema.getArea().getId(), areaNueva.getId());

        if (mismoNombre && mismaArea) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Tema tema, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(tema.getActivo(), activo)) {
            throw new BusinessException("El tema ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(TemaDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del tema son obligatorios");
        }

        if (dto.getAreaId() == null) {
            throw new BusinessException("El área es obligatoria");
        }
    }
}