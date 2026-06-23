package co.edu.ufps.legal_cases.business.service.persona.condicion;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.CondicionDTO;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;

@Component
public class CondicionMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public CondicionDTO convertirADTO(Condicion condicion) {
        return new CondicionDTO(
                condicion.getId(),
                condicion.getNombre(),
                condicion.getActivo());
    }

    public Condicion crearEntidad(String nombre) {
        Condicion condicion = new Condicion();
        condicion.setNombre(nombre);
        condicion.setActivo(true);
        return condicion;
    }

    public void aplicarDatos(Condicion condicion, String nombre) {
        condicion.setNombre(nombre);
    }
}