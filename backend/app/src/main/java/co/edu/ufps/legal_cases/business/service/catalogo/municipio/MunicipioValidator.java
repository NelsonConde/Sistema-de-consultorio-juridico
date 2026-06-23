package co.edu.ufps.legal_cases.business.service.catalogo.municipio;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.MunicipioDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.repository.catalogo.MunicipioRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class MunicipioValidator {

    private final MunicipioRepository municipioRepository;

    public MunicipioValidator(MunicipioRepository municipioRepository) {
        this.municipioRepository = municipioRepository;
    }

    public void validarCreacion(MunicipioDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, MunicipioDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del municipio");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del municipio es obligatorio");
        }

        if (nombreNormalizado.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre, Long departamentoId) {
        if (municipioRepository.existsByNombreIgnoreCaseAndDepartamentoId(nombre, departamentoId)) {
            throw new BusinessException("Ya existe un municipio con ese nombre en el departamento seleccionado");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long departamentoId, Long id) {
        if (municipioRepository.existsByNombreIgnoreCaseAndDepartamentoIdAndIdNot(nombre, departamentoId, id)) {
            throw new BusinessException("Ya existe un municipio con ese nombre en el departamento seleccionado");
        }
    }

    public void validarExistenCambios(Municipio municipio, String nombreNuevo, Departamento departamentoNuevo) {
        boolean mismoNombre = Objects.equals(municipio.getNombre(), nombreNuevo);
        boolean mismoDepartamento = municipio.getDepartamento() != null
                && Objects.equals(municipio.getDepartamento().getId(), departamentoNuevo.getId());

        if (mismoNombre && mismoDepartamento) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Municipio municipio, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(municipio.getActivo(), activo)) {
            throw new BusinessException("El municipio ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(MunicipioDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del municipio son obligatorios");
        }

        if (dto.getDepartamentoId() == null) {
            throw new BusinessException("El departamento es obligatorio");
        }
    }
}