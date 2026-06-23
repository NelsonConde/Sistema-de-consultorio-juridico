package co.edu.ufps.legal_cases.business.service.persona.tipopersona;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.TipoPersonaDTO;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.persona.TipoPersonaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class TipoPersonaValidator {

    private final TipoPersonaRepository tipoPersonaRepository;

    public TipoPersonaValidator(TipoPersonaRepository tipoPersonaRepository) {
        this.tipoPersonaRepository = tipoPersonaRepository;
    }

    public void validarCreacion(TipoPersonaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, TipoPersonaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del tipo de persona");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del tipo de persona es obligatorio");
        }

        if (nombreNormalizado.length() > 80) {
            throw new BusinessException("El nombre no puede superar los 80 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (tipoPersonaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un tipo de persona con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (tipoPersonaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un tipo de persona con ese nombre");
        }
    }

    public void validarExistenCambios(TipoPersona tipoPersona, String nombreNuevo) {
        if (Objects.equals(tipoPersona.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(TipoPersona tipoPersona, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(tipoPersona.getActivo(), activo)) {
            throw new BusinessException("El tipo de persona ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(TipoPersonaDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del tipo de persona son obligatorios");
        }
    }
}