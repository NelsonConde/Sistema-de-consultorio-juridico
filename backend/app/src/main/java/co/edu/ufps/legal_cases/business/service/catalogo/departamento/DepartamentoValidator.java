package co.edu.ufps.legal_cases.business.service.catalogo.departamento;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.DepartamentoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.repository.catalogo.DepartamentoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class DepartamentoValidator {

    private final DepartamentoRepository departamentoRepository;

    public DepartamentoValidator(DepartamentoRepository departamentoRepository) {
        this.departamentoRepository = departamentoRepository;
    }

    public void validarCreacion(DepartamentoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, DepartamentoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del departamento");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del departamento es obligatorio");
        }

        if (nombreNormalizado.length() > 80) {
            throw new BusinessException("El nombre no puede superar los 80 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (departamentoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un departamento con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (departamentoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un departamento con ese nombre");
        }
    }

    public void validarExistenCambios(Departamento departamento, String nombreNuevo) {
        if (Objects.equals(departamento.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Departamento departamento, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(departamento.getActivo(), activo)) {
            throw new BusinessException("El departamento ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(DepartamentoDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del departamento son obligatorios");
        }
    }
}