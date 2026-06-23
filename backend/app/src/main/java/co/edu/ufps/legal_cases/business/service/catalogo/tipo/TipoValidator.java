package co.edu.ufps.legal_cases.business.service.catalogo.tipo;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class TipoValidator {

    private final TipoRepository tipoRepository;

    public TipoValidator(TipoRepository tipoRepository) {
        this.tipoRepository = tipoRepository;
    }

    public void validarCreacion(TipoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, TipoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del tipo");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del tipo es obligatorio");
        }

        if (nombreNormalizado.length() > 80) {
            throw new BusinessException("El nombre del tipo no puede superar los 80 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre, Long temaId) {
        if (tipoRepository.existsByNombreIgnoreCaseAndTemaId(nombre, temaId)) {
            throw new BusinessException("Ya existe un tipo con ese nombre en el tema seleccionado");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long temaId, Long id) {
        if (tipoRepository.existsByNombreIgnoreCaseAndTemaIdAndIdNot(nombre, temaId, id)) {
            throw new BusinessException("Ya existe un tipo con ese nombre en el tema seleccionado");
        }
    }

    public void validarExistenCambios(Tipo tipo, String nombreNuevo, Tema temaNuevo) {
        boolean mismoNombre = Objects.equals(tipo.getNombre(), nombreNuevo);
        boolean mismoTema = tipo.getTema() != null
                && Objects.equals(tipo.getTema().getId(), temaNuevo.getId());

        if (mismoNombre && mismoTema) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Tipo tipo, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(tipo.getActivo(), activo)) {
            throw new BusinessException("El tipo ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(TipoDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del tipo son obligatorios");
        }

        if (dto.getTemaId() == null) {
            throw new BusinessException("El tema es obligatorio");
        }
    }
}