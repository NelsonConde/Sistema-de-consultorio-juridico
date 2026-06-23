package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;

@Component
public class MonitorMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public MonitorDTO convertirADTO(Monitor monitor) {
        MonitorDTO dto = new MonitorDTO();

        dto.setId(monitor.getId());
        dto.setNombre(monitor.getNombre());

        dto.setTipoDocumentoId(
                monitor.getTipoDocumento() != null
                        ? monitor.getTipoDocumento().getId()
                        : null);

        dto.setDocumento(monitor.getDocumento());
        dto.setEmail(monitor.getEmail());
        dto.setTelefono(monitor.getTelefono());
        dto.setUsuario(monitor.getUsuario());
        dto.setCodigo(monitor.getCodigo());

        dto.setSedeId(
                monitor.getSede() != null
                        ? monitor.getSede().getId()
                        : null);

        dto.setActivo(monitor.getActivo());

        return dto;
    }

    public void aplicarDatos(Monitor monitor, DatosMonitor datos) {
        monitor.setNombre(datos.nombre());
        monitor.setTipoDocumento(datos.tipoDocumento());
        monitor.setDocumento(datos.documento());
        monitor.setEmail(datos.email());
        monitor.setTelefono(datos.telefono());
        monitor.setUsuario(datos.usuario());
        monitor.setCodigo(datos.codigo());
        monitor.setSede(datos.sede());
        monitor.setActivo(datos.activo());
    }
}