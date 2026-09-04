package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResumenDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoResumenProjection;

@Component
public class SeguimientoMapper {

    // Define cómo se expone un seguimiento hacia la API.
    public SeguimientoResponseDTO convertirAResponseDTO(Seguimiento seguimiento) {
        SeguimientoResponseDTO dto = new SeguimientoResponseDTO();

        dto.setId(seguimiento.getId());
        dto.setVersion(seguimiento.getVersion());
        dto.setDescripcion(seguimiento.getDescripcion());
        dto.setFechaEntrega(seguimiento.getFechaEntrega());
        dto.setDiasNotificacion(seguimiento.getDiasNotificacion());
        dto.setNotificarPartes(seguimiento.getNotificarPartes());
        dto.setNotificarEstudiante(seguimiento.getNotificarEstudiante());
        dto.setAlertaDisciplinaria(seguimiento.getAlertaDisciplinaria());
        dto.setEstado(seguimiento.getEstado());

        dto.setCategoriaSeguimientoId(seguimiento.getCategoriaSeguimiento().getId());
        dto.setCategoriaSeguimientoNombre(seguimiento.getCategoriaSeguimiento().getNombre());

        dto.setConsultaId(seguimiento.getConsulta().getId());

        dto.setAutorId(seguimiento.getAutor().getId());
        dto.setAutorUsername(seguimiento.getAutor().getUsername());

        dto.setFechaCreacion(seguimiento.getFechaCreacion());
        dto.setFechaActualizacion(seguimiento.getFechaActualizacion());

        return dto;
    }

    public SeguimientoResumenDTO convertirAResumenDTO(SeguimientoResumenProjection seguimiento) {
        return new SeguimientoResumenDTO(
                seguimiento.getId(),
                seguimiento.getVersion(),
                seguimiento.getDescripcion(),
                seguimiento.getFechaEntrega(),
                seguimiento.getDiasNotificacion(),
                seguimiento.getNotificarPartes(),
                seguimiento.getNotificarEstudiante(),
                seguimiento.getAlertaDisciplinaria(),
                seguimiento.getEstado(),
                seguimiento.getCategoriaSeguimientoId(),
                seguimiento.getCategoriaSeguimientoNombre(),
                seguimiento.getConsultaId(),
                seguimiento.getAutorId(),
                seguimiento.getAutorUsername(),
                seguimiento.getFechaCreacion(),
                seguimiento.getFechaActualizacion());
    }

    public void aplicarDatos(Seguimiento seguimiento, DatosSeguimiento datos) {
        seguimiento.setDescripcion(datos.descripcion());
        seguimiento.setFechaEntrega(datos.fechaEntrega());
        seguimiento.setDiasNotificacion(datos.diasNotificacion());
        seguimiento.setNotificarPartes(datos.notificarPartes());
        seguimiento.setNotificarEstudiante(datos.notificarEstudiante());
        seguimiento.setAlertaDisciplinaria(datos.alertaDisciplinaria());
        seguimiento.setCategoriaSeguimiento(datos.categoria());
        seguimiento.setConsulta(datos.consulta());
    }
}
