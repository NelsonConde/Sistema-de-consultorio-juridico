package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionDetalleResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionPersonaDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.persona.Persona;

// Convierte conciliaciones entre entidad y DTO de salida.
// No valida permisos ni reglas de negocio; eso queda en AccessService y Validator.
@Component
public class ConciliacionMapper {

    public ConciliacionResponseDTO convertirAResponseDTO(Conciliacion conciliacion) {
        ConciliacionResponseDTO dto = new ConciliacionResponseDTO();

        dto.setId(conciliacion.getId());
        dto.setConsultaId(obtenerConsultaId(conciliacion));

        dto.setEstudianteId(conciliacion.getEstudiante() != null
                ? conciliacion.getEstudiante().getId()
                : null);
        dto.setEstudianteNombre(conciliacion.getEstudiante() != null
                ? conciliacion.getEstudiante().getNombre()
                : null);

        dto.setConciliadorId(conciliacion.getConciliador() != null
                ? conciliacion.getConciliador().getId()
                : null);
        dto.setConciliadorNombre(conciliacion.getConciliador() != null
                ? conciliacion.getConciliador().getNombre()
                : null);

        aplicarEstado(dto, conciliacion.getEstado());

        dto.setFechaConciliacion(conciliacion.getFechaConciliacion());
        dto.setDocumentoSolicitudPath(conciliacion.getDocumentoSolicitudPath());
        dto.setActaPath(conciliacion.getActaPath());

        dto.setSolicitadoPorId(conciliacion.getSolicitadoPor() != null
                ? conciliacion.getSolicitadoPor().getId()
                : null);
        dto.setSolicitadoPorUsername(conciliacion.getSolicitadoPor() != null
                ? conciliacion.getSolicitadoPor().getUsername()
                : null);

        dto.setActivo(conciliacion.getActivo());
        dto.setFechaCreacion(conciliacion.getFechaCreacion());
        dto.setFechaActualizacion(conciliacion.getFechaActualizacion());
        dto.setFechaFinalizacion(conciliacion.getFechaFinalizacion());

        return dto;
    }

    public ConciliacionDetalleResponseDTO convertirADetalleResponseDTO(Conciliacion conciliacion) {
        ConciliacionDetalleResponseDTO dto = new ConciliacionDetalleResponseDTO();

        dto.setId(conciliacion.getId());
        dto.setConsultaId(obtenerConsultaId(conciliacion));

        dto.setEstudianteId(conciliacion.getEstudiante() != null
                ? conciliacion.getEstudiante().getId()
                : null);
        dto.setEstudianteNombre(conciliacion.getEstudiante() != null
                ? conciliacion.getEstudiante().getNombre()
                : null);

        dto.setConciliadorId(conciliacion.getConciliador() != null
                ? conciliacion.getConciliador().getId()
                : null);
        dto.setConciliadorNombre(conciliacion.getConciliador() != null
                ? conciliacion.getConciliador().getNombre()
                : null);

        aplicarEstado(dto, conciliacion.getEstado());

        dto.setFechaConciliacion(conciliacion.getFechaConciliacion());

        Consulta consulta = conciliacion.getConsulta();

        if (consulta != null) {
            dto.setConsultante(convertirAPersonaDTO(consulta.getPersona()));
            dto.setPartes(convertirAPersonasDTO(consulta.getPartes()));
            dto.setContrapartes(convertirAPersonasDTO(consulta.getContrapartes()));
        }

        dto.setDocumentoSolicitudPath(conciliacion.getDocumentoSolicitudPath());
        dto.setActaPath(conciliacion.getActaPath());

        dto.setSolicitadoPorId(conciliacion.getSolicitadoPor() != null
                ? conciliacion.getSolicitadoPor().getId()
                : null);
        dto.setSolicitadoPorUsername(conciliacion.getSolicitadoPor() != null
                ? conciliacion.getSolicitadoPor().getUsername()
                : null);

        dto.setActivo(conciliacion.getActivo());
        dto.setFechaCreacion(conciliacion.getFechaCreacion());
        dto.setFechaActualizacion(conciliacion.getFechaActualizacion());
        dto.setFechaFinalizacion(conciliacion.getFechaFinalizacion());

        return dto;
    }

    private void aplicarEstado(ConciliacionResponseDTO dto, EstadoConciliacion estado) {
        if (estado == null) {
            return;
        }

        dto.setEstadoId(estado.getId());
        dto.setEstadoCodigo(estado.getCodigo());
        dto.setEstadoNombre(estado.getNombre());
    }

    private void aplicarEstado(ConciliacionDetalleResponseDTO dto, EstadoConciliacion estado) {
        if (estado == null) {
            return;
        }

        dto.setEstadoId(estado.getId());
        dto.setEstadoCodigo(estado.getCodigo());
        dto.setEstadoNombre(estado.getNombre());
    }

    private Long obtenerConsultaId(Conciliacion conciliacion) {
        return conciliacion.getConsulta() != null
                ? conciliacion.getConsulta().getId()
                : null;
    }

    private List<ConciliacionPersonaDTO> convertirAPersonasDTO(List<Persona> personas) {
        if (personas == null || personas.isEmpty()) {
            return Collections.emptyList();
        }

        return personas.stream()
                .filter(Objects::nonNull)
                .map(this::convertirAPersonaDTO)
                .toList();
    }

    private ConciliacionPersonaDTO convertirAPersonaDTO(Persona persona) {
        if (persona == null) {
            return null;
        }

        return new ConciliacionPersonaDTO(
                persona.getId(),
                construirNombrePersona(persona));
    }

    private String construirNombrePersona(Persona persona) {
        String nombres = persona.getNombres() != null ? persona.getNombres().trim() : "";
        String apellidos = persona.getApellidos() != null ? persona.getApellidos().trim() : "";

        String nombreCompleto = (nombres + " " + apellidos).trim();

        return nombreCompleto.isEmpty() ? null : nombreCompleto;
    }
}