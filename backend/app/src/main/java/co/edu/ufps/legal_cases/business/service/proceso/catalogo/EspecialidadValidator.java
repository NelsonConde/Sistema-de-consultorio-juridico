package co.edu.ufps.legal_cases.business.service.proceso.catalogo;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.proceso.EspecialidadDTO;
import co.edu.ufps.legal_cases.business.model.proceso.Especialidad;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;
import co.edu.ufps.legal_cases.business.repository.proceso.EspecialidadRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class EspecialidadValidator {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadValidator(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    public void validarCreacion(EspecialidadDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }
    }

    public void validarActualizacion(Long id, EspecialidadDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la especialidad");
        }
    }

    public String normalizarNombre(String nombre) {
        String nombreNormalizado = normalizarTexto(nombre);

        if (nombreNormalizado == null || nombreNormalizado.isBlank()) {
            throw new BusinessException("El nombre de la especialidad es obligatorio");
        }

        if (nombreNormalizado.length() > 80) {
            throw new BusinessException("El nombre no puede superar los 80 caracteres");
        }

        return nombreNormalizado;
    }

    public void validarNombreDisponible(String nombre, Long organoControlId) {
        if (especialidadRepository.existsByNombreIgnoreCaseAndOrganoControlId(nombre, organoControlId)) {
            throw new BusinessException(
                    "Ya existe una especialidad con ese nombre para el órgano de control seleccionado");
        }
    }

    public void validarNombreDisponibleParaActualizacion(
            String nombre,
            Long organoControlId,
            Long id) {

        if (especialidadRepository.existsByNombreIgnoreCaseAndOrganoControlIdAndIdNot(
                nombre,
                organoControlId,
                id)) {
            throw new BusinessException(
                    "Ya existe una especialidad con ese nombre para el órgano de control seleccionado");
        }
    }

    public void validarExistenCambios(
            Especialidad especialidad,
            String nombreNuevo,
            OrganoControl organoControlNuevo) {

        boolean mismoNombre = Objects.equals(especialidad.getNombre(), nombreNuevo);
        boolean mismoOrgano = especialidad.getOrganoControl() != null
                && Objects.equals(especialidad.getOrganoControl().getId(), organoControlNuevo.getId());

        if (mismoNombre && mismoOrgano) {
            throw new BusinessException("No hay cambios para actualizar");
        }
    }

    public void validarCambioEstado(Especialidad especialidad, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(especialidad.getActivo(), activo)) {
            throw new BusinessException("La especialidad ya tiene ese estado");
        }
    }

    private void validarDtoObligatorio(EspecialidadDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la especialidad son obligatorios");
        }

        if (dto.getOrganoControlId() == null) {
            throw new BusinessException("El órgano de control es obligatorio");
        }
    }
}