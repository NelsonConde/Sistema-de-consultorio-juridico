package co.edu.ufps.legal_cases.business.service.persona.tipopersona;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.TipoPersonaDTO;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;

@Component
public class TipoPersonaMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public TipoPersonaDTO convertirADTO(TipoPersona tipoPersona) {
        return new TipoPersonaDTO(
                tipoPersona.getId(),
                tipoPersona.getNombre(),
                tipoPersona.getActivo());
    }

    public TipoPersona crearEntidad(String nombre) {
        TipoPersona tipoPersona = new TipoPersona();
        tipoPersona.setNombre(nombre);
        tipoPersona.setActivo(true);
        return tipoPersona;
    }

    public void aplicarDatos(TipoPersona tipoPersona, String nombre) {
        tipoPersona.setNombre(nombre);
    }
}