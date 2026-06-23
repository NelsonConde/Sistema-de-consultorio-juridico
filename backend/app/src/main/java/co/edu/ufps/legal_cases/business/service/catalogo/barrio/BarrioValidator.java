package co.edu.ufps.legal_cases.business.service.catalogo.barrio;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.BarrioDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.repository.catalogo.BarrioRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class BarrioValidator {

    private final BarrioRepository barrioRepository;

    public BarrioValidator(BarrioRepository barrioRepository) {
        this.barrioRepository = barrioRepository;
    }

    public void validarCreacion(BarrioDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, BarrioDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del barrio");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del barrio es obligatorio");
        }

        if (nombreNormalizado.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre, Long municipioId) {
        if (barrioRepository.existsByNombreIgnoreCaseAndMunicipioId(nombre, municipioId)) {
            throw new BusinessException("Ya existe un barrio con ese nombre en el municipio seleccionado");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long municipioId, Long id) {
        if (barrioRepository.existsByNombreIgnoreCaseAndMunicipioIdAndIdNot(nombre, municipioId, id)) {
            throw new BusinessException("Ya existe un barrio con ese nombre en el municipio seleccionado");
        }
    }

    public void validarExistenCambios(Barrio barrio, String nombreNuevo, Municipio municipioNuevo) {
        boolean mismoNombre = Objects.equals(barrio.getNombre(), nombreNuevo);
        boolean mismoMunicipio = barrio.getMunicipio() != null
                && Objects.equals(barrio.getMunicipio().getId(), municipioNuevo.getId());

        if (mismoNombre && mismoMunicipio) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Barrio barrio, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(barrio.getActivo(), activo)) {
            throw new BusinessException("El barrio ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(BarrioDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del barrio son obligatorios");
        }

        if (dto.getMunicipioId() == null) {
            throw new BusinessException("El municipio es obligatorio");
        }
    }
}