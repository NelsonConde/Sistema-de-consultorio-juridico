package co.edu.ufps.legal_cases.business.service.persona.ocupacion;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.OcupacionDTO;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.repository.persona.OcupacionRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class OcupacionValidator {

    private final OcupacionRepository ocupacionRepository;

    public OcupacionValidator(OcupacionRepository ocupacionRepository) {
        this.ocupacionRepository = ocupacionRepository;
    }

    public void validarCreacion(OcupacionDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, OcupacionDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la ocupación");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la ocupación es obligatorio");
        }

        if (nombreNormalizado.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (ocupacionRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una ocupación con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (ocupacionRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una ocupación con ese nombre");
        }
    }

    public void validarExistenCambios(Ocupacion ocupacion, String nombreNuevo) {
        if (Objects.equals(ocupacion.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Ocupacion ocupacion, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(ocupacion.getActivo(), activo)) {
            throw new BusinessException("La ocupación ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(OcupacionDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la ocupación son obligatorios");
        }
    }
}