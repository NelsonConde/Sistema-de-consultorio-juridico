package co.edu.ufps.legal_cases.business.service.catalogo.tipoDocumento;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDocumentoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class TipoDocumentoValidator {

    private final TipoDocumentoRepository tipoDocumentoRepository;

    public TipoDocumentoValidator(TipoDocumentoRepository tipoDocumentoRepository) {
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    public void validarCreacion(TipoDocumentoDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, TipoDocumentoDTO dto, TipoDocumento tipoDocumento) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), tipoDocumento.getId())) {
            throw new BusinessException("No se permite cambiar el id del tipo de documento");
        }
    }

    public String normalizarNombre(TipoDocumentoDTO dto) {
        validarDtoObligatorio(dto);

        String nombre = normalizarTexto(dto.getNombre());

        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre es obligatorio");
        }

        return nombre;
    }

    public void validarNombreDisponible(String nombre) {
        if (tipoDocumentoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un tipo de documento con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un tipo de documento con ese nombre");
        }
    }

    public void validarExistenCambios(
            TipoDocumento tipoDocumento,
            String nombreNuevo) {

        if (tipoDocumento.getNombre().equalsIgnoreCase(nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(TipoDocumento tipoDocumento, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(tipoDocumento.getActivo(), activo)) {
            throw new BusinessException("El tipo de documento ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(TipoDocumentoDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del tipo de documento son obligatorios");
        }
    }
}