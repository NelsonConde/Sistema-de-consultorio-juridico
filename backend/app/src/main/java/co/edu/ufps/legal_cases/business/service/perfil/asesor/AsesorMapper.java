package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;

@Component
public class AsesorMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public AsesorDTO convertirADTO(Asesor asesor) {
        AsesorDTO dto = new AsesorDTO();

        dto.setId(asesor.getId());
        dto.setNombre(asesor.getNombre());

        dto.setTipoDocumentoId(
                asesor.getTipoDocumento() != null
                        ? asesor.getTipoDocumento().getId()
                        : null);

        dto.setDocumento(asesor.getDocumento());
        dto.setEmail(asesor.getEmail());
        dto.setTelefono(asesor.getTelefono());
        dto.setUsuario(asesor.getUsuario());

        dto.setSedeId(
                asesor.getSede() != null
                        ? asesor.getSede().getId()
                        : null);

        dto.setCodigo(asesor.getCodigo());

        dto.setAreaId(
                asesor.getArea() != null
                        ? asesor.getArea().getId()
                        : null);

        dto.setActivo(asesor.getActivo());

        return dto;
    }

    public void aplicarDatos(Asesor asesor, DatosAsesor datos) {
        asesor.setNombre(datos.nombre());
        asesor.setTipoDocumento(datos.tipoDocumento());
        asesor.setDocumento(datos.documento());
        asesor.setEmail(datos.email());
        asesor.setTelefono(datos.telefono());
        asesor.setUsuario(datos.usuario());
        asesor.setSede(datos.sede());
        asesor.setCodigo(datos.codigo());
        asesor.setArea(datos.area());
        asesor.setActivo(datos.activo());
    }
}