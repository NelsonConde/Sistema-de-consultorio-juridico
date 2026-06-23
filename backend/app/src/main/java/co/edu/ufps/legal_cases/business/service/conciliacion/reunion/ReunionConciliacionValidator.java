package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion.ConciliacionValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// Reglas de negocio específicas de programación y reprogramación de reunión.
@Component
public class ReunionConciliacionValidator {

    private static final int MAX_OBSERVACIONES = 300;

    private final ReunionConciliacionRepository reunionConciliacionRepository;
    private final ConciliacionValidator conciliacionValidator;

    public ReunionConciliacionValidator(
            ReunionConciliacionRepository reunionConciliacionRepository,
            ConciliacionValidator conciliacionValidator) {
        this.reunionConciliacionRepository = reunionConciliacionRepository;
        this.conciliacionValidator = conciliacionValidator;
    }

    public void validarProgramacion(Conciliacion conciliacion, ReunionConciliacionRequestDTO dto, Sede sede) {
        conciliacionValidator.validarPuedeProgramarFecha(conciliacion);
        validarRequest(dto);
        validarSede(sede);

        if (reunionConciliacionRepository.existsByConciliacion_Id(conciliacion.getId())) {
            throw new BusinessException("La conciliación ya tiene una reunión programada");
        }
    }

    public void validarReprogramacion(
            Conciliacion conciliacion,
            ReunionConciliacion reunionActual,
            ReunionConciliacionRequestDTO dto,
            Sede sedeNueva) {
        conciliacionValidator.validarPuedeProgramarFecha(conciliacion);
        validarRequest(dto);
        validarSede(sedeNueva);

        if (reunionActual == null) {
            throw new BusinessException("La conciliación no tiene reunión programada para reprogramar");
        }

        if (!hayCambioReal(reunionActual, dto, sedeNueva)) {
            throw new BusinessException("No hay cambios para reprogramar la reunión");
        }
    }

    public String normalizarObservaciones(String observaciones) {
        if (observaciones == null) {
            return null;
        }

        String valor = observaciones.trim();
        return valor.isBlank() ? null : valor;
    }

    private void validarRequest(ReunionConciliacionRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la reunión son obligatorios");
        }

        if (dto.getFechaReunion() == null) {
            throw new BusinessException("La fecha de la reunión es obligatoria");
        }

        if (!dto.getFechaReunion().isAfter(LocalDateTime.now())) {
            throw new BusinessException("La fecha de la reunión debe ser futura");
        }

        if (dto.getSedeId() == null) {
            throw new BusinessException("La sede de la reunión es obligatoria");
        }

        String observaciones = normalizarObservaciones(dto.getObservaciones());

        if (observaciones != null && observaciones.length() > MAX_OBSERVACIONES) {
            throw new BusinessException("Las observaciones no pueden superar 300 caracteres");
        }
    }

    private void validarSede(Sede sede) {
        if (sede == null || sede.getId() == null || !Boolean.TRUE.equals(sede.getActivo())) {
            throw new BusinessException("Sede no encontrada o inactiva");
        }
    }

    private boolean hayCambioReal(
            ReunionConciliacion reunionActual,
            ReunionConciliacionRequestDTO dto,
            Sede sedeNueva) {
        String observacionesNuevas = normalizarObservaciones(dto.getObservaciones());

        return !Objects.equals(reunionActual.getFechaReunion(), dto.getFechaReunion())
                || !Objects.equals(obtenerSedeId(reunionActual), sedeNueva.getId())
                || !Objects.equals(reunionActual.getObservaciones(), observacionesNuevas);
    }

    private Long obtenerSedeId(ReunionConciliacion reunion) {
        return reunion.getSede() != null ? reunion.getSede().getId() : null;
    }
}
