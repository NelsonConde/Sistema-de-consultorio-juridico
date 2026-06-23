package co.edu.ufps.legal_cases.business.service.catalogo.sede;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.SedeDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class SedeValidator {

    private final SedeRepository sedeRepository;

    public SedeValidator(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    public void validarCreacion(SedeDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, SedeDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la sede");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la sede es obligatorio");
        }

        if (nombreNormalizado.length() > 100) {
            throw new BusinessException("El nombre de la sede no puede superar los 100 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (sedeRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una sede con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (sedeRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una sede con ese nombre");
        }
    }

    public void validarExistenCambios(Sede sede, String nombreNuevo) {
        if (Objects.equals(sede.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Sede sede, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(sede.getActivo(), activo)) {
            throw new BusinessException("La sede ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(SedeDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la sede son obligatorios");
        }
    }
}