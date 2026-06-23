package co.edu.ufps.legal_cases.business.service.perfil.estudiante;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;

@Component
public class EstudianteMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public EstudianteDTO convertirADTO(Estudiante estudiante) {
        EstudianteDTO dto = new EstudianteDTO();

        dto.setId(estudiante.getId());
        dto.setNombre(estudiante.getNombre());

        dto.setTipoDocumentoId(
                estudiante.getTipoDocumento() != null
                        ? estudiante.getTipoDocumento().getId()
                        : null);

        dto.setDocumento(estudiante.getDocumento());
        dto.setEmail(estudiante.getEmail());
        dto.setTelefono(estudiante.getTelefono());
        dto.setUsuario(estudiante.getUsuario());

        dto.setSedeId(
                estudiante.getSede() != null
                        ? estudiante.getSede().getId()
                        : null);

        dto.setCodigo(estudiante.getCodigo());

        dto.setAsesorId(
                estudiante.getAsesor() != null
                        ? estudiante.getAsesor().getId()
                        : null);

        dto.setActivo(estudiante.getActivo());
        dto.setConciliacion(estudiante.getConciliacion());

        return dto;
    }

    public void aplicarDatos(Estudiante estudiante, DatosEstudiante datos) {
        estudiante.setNombre(datos.nombre());
        estudiante.setTipoDocumento(datos.tipoDocumento());
        estudiante.setDocumento(datos.documento());
        estudiante.setEmail(datos.email());
        estudiante.setTelefono(datos.telefono());
        estudiante.setUsuario(datos.usuario());
        estudiante.setSede(datos.sede());
        estudiante.setCodigo(datos.codigo());
        estudiante.setAsesor(datos.asesor());
        estudiante.setActivo(datos.activo());
        estudiante.setConciliacion(datos.conciliacion());
    }
}