package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionResumenProjection;

// Convierte la reunión de conciliación a DTO de salida.
@Component
public class ReunionConciliacionMapper {

    public ReunionConciliacionResponseDTO convertirAResponseDTO(ReunionConciliacion reunion) {
        if (reunion == null) {
            return null;
        }

        ReunionConciliacionResponseDTO dto = new ReunionConciliacionResponseDTO();
        dto.setConciliacionId(reunion.getConciliacionId());
        dto.setVersion(reunion.getVersion());
        dto.setConciliacionVersion(
                reunion.getConciliacion() != null
                        ? reunion.getConciliacion().getVersion()
                        : null);

        dto.setFechaReunion(reunion.getFechaReunion());
        dto.setObservaciones(reunion.getObservaciones());
        dto.setFechaCreacion(reunion.getFechaCreacion());
        dto.setFechaActualizacion(reunion.getFechaActualizacion());

        if (reunion.getSede() != null) {
            dto.setSedeId(reunion.getSede().getId());
            dto.setSedeNombre(reunion.getSede().getNombre());
        }

        return dto;
    }

    public ReunionConciliacionResumenDTO convertirAResumen(ReunionConciliacionResumenProjection reunion) {
        return new ReunionConciliacionResumenDTO(
                reunion.getConciliacionId(),
                reunion.getVersion(),
                reunion.getConciliacionVersion(),
                reunion.getConsultaId(),
                reunion.getEstadoCodigo(),
                reunion.getEstadoNombre(),
                reunion.getSedeId(),
                reunion.getSedeNombre(),
                reunion.getFechaReunion(),
                reunion.getObservaciones(),
                reunion.getEstudianteId(),
                reunion.getEstudianteNombre(),
                reunion.getConciliadorId(),
                reunion.getConciliadorNombre(),
                reunion.getFechaCreacion(),
                reunion.getFechaActualizacion());
    }
}
