package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;

@Component
public class AdministrativoMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public AdministrativoDTO convertirADTO(Administrativo administrativo) {
        AdministrativoDTO dto = new AdministrativoDTO();

        dto.setId(administrativo.getId());
        dto.setNombre(administrativo.getNombre());

        dto.setTipoDocumentoId(
                administrativo.getTipoDocumento() != null
                        ? administrativo.getTipoDocumento().getId()
                        : null);

        dto.setDocumento(administrativo.getDocumento());
        dto.setEmail(administrativo.getEmail());
        dto.setTelefono(administrativo.getTelefono());
        dto.setUsuario(administrativo.getUsuario());
        dto.setCodigo(administrativo.getCodigo());

        dto.setSedeId(
                administrativo.getSede() != null
                        ? administrativo.getSede().getId()
                        : null);

        dto.setActivo(administrativo.getActivo());
        dto.setDirectora(administrativo.getDirectora());

        return dto;
    }

    public void aplicarDatos(Administrativo administrativo, DatosAdministrativo datos) {
        administrativo.setNombre(datos.nombre());
        administrativo.setTipoDocumento(datos.tipoDocumento());
        administrativo.setDocumento(datos.documento());
        administrativo.setEmail(datos.email());
        administrativo.setTelefono(datos.telefono());
        administrativo.setUsuario(datos.usuario());
        administrativo.setCodigo(datos.codigo());
        administrativo.setSede(datos.sede());
        administrativo.setActivo(datos.activo());
        administrativo.setDirectora(datos.directora());
    }
}