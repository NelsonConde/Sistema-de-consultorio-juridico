package co.edu.ufps.legal_cases.business.service.persona.condicion;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.CondicionDTO;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.repository.persona.CondicionRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class CondicionValidator {

    private final CondicionRepository condicionRepository;

    public CondicionValidator(CondicionRepository condicionRepository) {
        this.condicionRepository = condicionRepository;
    }

    public void validarCreacion(CondicionDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, CondicionDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la condición");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la condición es obligatorio");
        }

        if (nombreNormalizado.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (condicionRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una condición con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (condicionRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una condición con ese nombre");
        }
    }

    public void validarExistenCambios(Condicion condicion, String nombreNuevo) {
        if (Objects.equals(condicion.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Condicion condicion, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(condicion.getActivo(), activo)) {
            throw new BusinessException("La condición ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(CondicionDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la condición son obligatorios");
        }
    }
}