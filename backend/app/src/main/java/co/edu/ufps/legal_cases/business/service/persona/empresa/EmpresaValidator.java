package co.edu.ufps.legal_cases.business.service.persona.empresa;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.EmpresaDTO;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.repository.persona.EmpresaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class EmpresaValidator {

    private final EmpresaRepository empresaRepository;

    public EmpresaValidator(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public void validarCreacion(EmpresaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, EmpresaDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la empresa");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la empresa es obligatorio");
        }

        if (nombreNormalizado.length() > 150) {
            throw new BusinessException("El nombre no puede superar los 150 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (empresaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una empresa con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (empresaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una empresa con ese nombre");
        }
    }

    public void validarExistenCambios(Empresa empresa, String nombreNuevo) {
        if (Objects.equals(empresa.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Empresa empresa, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(empresa.getActivo(), activo)) {
            throw new BusinessException("La empresa ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(EmpresaDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la empresa son obligatorios");
        }
    }
}