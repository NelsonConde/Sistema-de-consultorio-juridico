package co.edu.ufps.legal_cases.business.service.catalogo.nacionalidad;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.NacionalidadDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.repository.catalogo.NacionalidadRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class NacionalidadValidator {

    private final NacionalidadRepository nacionalidadRepository;

    public NacionalidadValidator(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    public void validarCreacion(NacionalidadDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, NacionalidadDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la nacionalidad");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la nacionalidad es obligatorio");
        }

        if (nombreNormalizado.length() > 100) {
            throw new BusinessException("El nombre no puede superar los 100 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (nacionalidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una nacionalidad con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (nacionalidadRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe una nacionalidad con ese nombre");
        }
    }

    public void validarExistenCambios(Nacionalidad nacionalidad, String nombreNuevo) {
        if (Objects.equals(nacionalidad.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Nacionalidad nacionalidad, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(nacionalidad.getActivo(), activo)) {
            throw new BusinessException("La nacionalidad ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(NacionalidadDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la nacionalidad son obligatorios");
        }
    }
}