package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import java.util.List;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.persona.Persona;

// Convierte la entidad Consulta a DTO para evitar exponer directamente el modelo.
@Component
public class ConsultaMapper {

    public ConsultaDTO convertirADTO(Consulta c) {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setId(c.getId());
        dto.setFecha(c.getFecha());
        dto.setDescripcion(c.getDescripcion());
        dto.setHechos(c.getHechos());
        dto.setPretensiones(c.getPretensiones());
        dto.setConceptoJuridico(c.getConceptoJuridico());
        dto.setTramite(c.getTramite());
        dto.setObservaciones(c.getObservaciones());
        dto.setTipoViolencia(c.getTipoViolencia());
        dto.setEstado(c.getEstado());
        dto.setResultado(c.getResultado());
        dto.setPersonaId(c.getPersona() != null ? c.getPersona().getId() : null);
        dto.setSedeId(c.getSede() != null ? c.getSede().getId() : null);
        dto.setAreaId(c.getArea() != null ? c.getArea().getId() : null);
        dto.setTemaId(c.getTema() != null ? c.getTema().getId() : null);
        dto.setTipoId(c.getTipo() != null ? c.getTipo().getId() : null);
        dto.setAsesorId(c.getAsesor() != null ? c.getAsesor().getId() : null);
        dto.setMonitorId(c.getMonitor() != null ? c.getMonitor().getId() : null);
        dto.setEstudianteId(c.getEstudiante() != null ? c.getEstudiante().getId() : null);
        dto.setPartesIds(
                c.getPartes() != null
                        ? c.getPartes().stream().map(Persona::getId).toList()
                        : List.of()
        );
        dto.setContrapartesIds(
                c.getContrapartes() != null
                        ? c.getContrapartes().stream().map(Persona::getId).toList()
                        : List.of()
        );
        return dto;
    }

    public ConsultaBusquedaDTO convertirABusquedaDTO(Consulta c) {
        Persona p = c.getPersona();
        return new ConsultaBusquedaDTO(
                c.getId(),
                c.getDescripcion(),
                c.getFecha(),
                p != null ? p.getNombres() : null,
                p != null ? p.getApellidos() : null,
                p != null ? p.getNumeroDocumento() : null,
                c.getEstado());
    }
}