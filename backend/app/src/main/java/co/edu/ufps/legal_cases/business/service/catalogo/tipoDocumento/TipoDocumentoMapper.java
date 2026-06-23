package co.edu.ufps.legal_cases.business.service.catalogo.tipoDocumento;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDocumentoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;

@Component
public class TipoDocumentoMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public TipoDocumentoDTO convertirADTO(TipoDocumento tipoDocumento) {
        TipoDocumentoDTO dto = new TipoDocumentoDTO();

        dto.setId(tipoDocumento.getId());
        dto.setNombre(tipoDocumento.getNombre());
        dto.setActivo(tipoDocumento.getActivo());

        return dto;
    }

    public TipoDocumento crearEntidad(String nombre) {
        TipoDocumento tipoDocumento = new TipoDocumento();

        tipoDocumento.setNombre(nombre);
        tipoDocumento.setActivo(true);

        return tipoDocumento;
    }

    public void aplicarDatos(TipoDocumento tipoDocumento, String nombre) {
        tipoDocumento.setNombre(nombre);
    }
}