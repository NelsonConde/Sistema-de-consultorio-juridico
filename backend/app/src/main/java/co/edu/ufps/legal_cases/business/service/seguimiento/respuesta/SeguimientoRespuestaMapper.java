package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;

@Component
public class SeguimientoRespuestaMapper {

    // Convierte la entidad a DTO para no exponer directamente el modelo.
    public SeguimientoRespuestaResponseDTO convertirAResponseDTO(SeguimientoRespuesta respuesta) {
        SeguimientoRespuestaResponseDTO dto = new SeguimientoRespuestaResponseDTO();

        dto.setId(respuesta.getId());

        dto.setSeguimientoId(
                respuesta.getSeguimiento() != null
                        ? respuesta.getSeguimiento().getId()
                        : null
        );

        dto.setConsultaId(
                respuesta.getSeguimiento() != null
                        && respuesta.getSeguimiento().getConsulta() != null
                                ? respuesta.getSeguimiento().getConsulta().getId()
                                : null
        );

        dto.setEstudianteId(
                respuesta.getEstudiante() != null
                        ? respuesta.getEstudiante().getId()
                        : null
        );

        dto.setEstudianteNombre(
                respuesta.getEstudiante() != null
                        ? respuesta.getEstudiante().getNombre()
                        : null
        );

        dto.setContenido(respuesta.getContenido());
        dto.setEstado(respuesta.getEstado());
        dto.setFueraPlazo(respuesta.getFueraPlazo());
        dto.setObservacionRevision(respuesta.getObservacionRevision());

        dto.setRevisadoPorId(
                respuesta.getRevisadoPor() != null
                        ? respuesta.getRevisadoPor().getId()
                        : null
        );

        dto.setRevisadoPorUsername(
                respuesta.getRevisadoPor() != null
                        ? respuesta.getRevisadoPor().getUsername()
                        : null
        );

        dto.setActivo(respuesta.getActivo());
        dto.setFechaCreacion(respuesta.getFechaCreacion());
        dto.setFechaActualizacion(respuesta.getFechaActualizacion());
        dto.setFechaDecision(respuesta.getFechaDecision());

        return dto;
    }
}