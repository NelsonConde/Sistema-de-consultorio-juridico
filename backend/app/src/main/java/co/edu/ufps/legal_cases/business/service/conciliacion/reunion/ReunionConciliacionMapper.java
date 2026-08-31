package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;

// Convierte la reunión de conciliación a DTO de salida.
@Component
public class ReunionConciliacionMapper {

    public ReunionConciliacionResponseDTO convertirAResponseDTO(ReunionConciliacion reunion) {
        if (reunion == null) {
            return null;
        }

        ReunionConciliacionResponseDTO dto = new ReunionConciliacionResponseDTO();
        dto.setConciliacionId(reunion.getConciliacionId());
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
}
