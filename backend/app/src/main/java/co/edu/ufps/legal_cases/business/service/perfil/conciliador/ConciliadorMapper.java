package co.edu.ufps.legal_cases.business.service.perfil.conciliador;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;

@Component
public class ConciliadorMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public ConciliadorDTO convertirADTO(Conciliador conciliador) {
        ConciliadorDTO dto = new ConciliadorDTO();

        dto.setId(conciliador.getId());
        dto.setNombre(conciliador.getNombre());

        dto.setTipoDocumentoId(
                conciliador.getTipoDocumento() != null
                        ? conciliador.getTipoDocumento().getId()
                        : null);

        dto.setDocumento(conciliador.getDocumento());
        dto.setEmail(conciliador.getEmail());
        dto.setTelefono(conciliador.getTelefono());
        dto.setUsuario(conciliador.getUsuario());

        dto.setSedeId(
                conciliador.getSede() != null
                        ? conciliador.getSede().getId()
                        : null);

        dto.setCodigo(conciliador.getCodigo());
        dto.setTipoConciliador(conciliador.getTipoConciliador());
        dto.setActivo(conciliador.getActivo());

        return dto;
    }

    public void aplicarDatos(Conciliador conciliador, DatosConciliador datos) {
        conciliador.setNombre(datos.nombre());
        conciliador.setTipoDocumento(datos.tipoDocumento());
        conciliador.setDocumento(datos.documento());
        conciliador.setEmail(datos.email());
        conciliador.setTelefono(datos.telefono());
        conciliador.setUsuario(datos.usuario());
        conciliador.setSede(datos.sede());
        conciliador.setCodigo(datos.codigo());
        conciliador.setTipoConciliador(datos.tipoConciliador());
        conciliador.setActivo(datos.activo());
    }
}