package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaDecisionDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaRequestDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Component
public class SeguimientoRespuestaValidator {

    public void validarCreacion(SeguimientoRespuestaRequestDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null) {
            throw new BusinessException("El id no debe enviarse en la creación");
        }

        normalizarContenido(dto.getContenido());
    }

    public void validarActualizacion(Long id, SeguimientoRespuestaRequestDTO dto) {
        validarDtoObligatorio(dto);

        if (dto.getId() != null && !Objects.equals(dto.getId(), id)) {
            throw new BusinessException("No se permite cambiar el id de la respuesta");
        }

        normalizarContenido(dto.getContenido());
    }

    public void validarDecision(SeguimientoRespuestaDecisionDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la decisión son obligatorios");
        }

        if (dto.getEstado() == null) {
            throw new BusinessException("El estado de la respuesta es obligatorio");
        }

        if (dto.getEstado() != EstadoRespuestaSeguimiento.APROBADA
                && dto.getEstado() != EstadoRespuestaSeguimiento.RECHAZADA) {
            throw new BusinessException("La decisión debe ser APROBADA o RECHAZADA");
        }

        String observacion = normalizarTexto(dto.getObservacionRevision());

        if (EstadoRespuestaSeguimiento.RECHAZADA.equals(dto.getEstado())
                && (observacion == null || observacion.isBlank())) {
            throw new BusinessException("La observación de revisión es obligatoria al rechazar una respuesta");
        }

        validarLongitudObservacionRevision(observacion);
    }

    public String normalizarContenido(String contenido) {
        String contenidoNormalizado = normalizarTexto(contenido);

        if (contenidoNormalizado == null || contenidoNormalizado.isBlank()) {
            throw new BusinessException("La respuesta del seguimiento es obligatoria");
        }

        if (contenidoNormalizado.length() > 1000) {
            throw new BusinessException("La respuesta del seguimiento no puede superar 1000 caracteres");
        }

        return contenidoNormalizado;
    }

    public String normalizarObservacionRevision(String observacionRevision) {
        String observacion = normalizarTexto(observacionRevision);
        validarLongitudObservacionRevision(observacion);
        return observacion;
    }

    private void validarLongitudObservacionRevision(String observacion) {
        if (observacion != null && observacion.length() > 500) {
            throw new BusinessException("La observación de revisión no puede superar 500 caracteres");
        }
    }

    private void validarDtoObligatorio(SeguimientoRespuestaRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de la respuesta son obligatorios");
        }
    }
}