package co.edu.ufps.legal_cases.business.service.proceso.catalogo;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.proceso.OrganoControlDTO;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;
import co.edu.ufps.legal_cases.business.repository.proceso.EspecialidadRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.OrganoControlRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class OrganoControlValidator {

    private final OrganoControlRepository organoControlRepository;
    private final EspecialidadRepository especialidadRepository;

    public OrganoControlValidator(
            OrganoControlRepository organoControlRepository,
            EspecialidadRepository especialidadRepository) {
        this.organoControlRepository = organoControlRepository;
        this.especialidadRepository = especialidadRepository;
    }

    public void validarCreacion(OrganoControlDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, OrganoControlDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id del órgano de control");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre del órgano de control es obligatorio");
        }

        if (nombreNormalizado.length() > 80) {
            throw new BusinessException("El nombre no puede superar los 80 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre) {
        if (organoControlRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un órgano de control con ese nombre");
        }
    }

    public void validarNombreDisponibleParaActualizacion(String nombre, Long id) {
        if (organoControlRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new BusinessException("Ya existe un órgano de control con ese nombre");
        }
    }

    public void validarPuedeDesactivarse(Long id) {
        // Si tiene especialidades activas, primero deben desactivarse ellas.
        if (especialidadRepository.existsByOrganoControlIdAndActivoTrue(id)) {
            throw new BusinessException(
                    "No se puede desactivar el órgano de control porque tiene especialidades activas");
        }
    }

    public void validarExistenCambios(OrganoControl organoControl, String nombreNuevo) {
        if (Objects.equals(organoControl.getNombre(), nombreNuevo)) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(OrganoControl organoControl, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(organoControl.getActivo(), activo)) {
            throw new BusinessException("El órgano de control ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(OrganoControlDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos del órgano de control son obligatorios");
        }
    }
}