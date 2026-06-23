package co.edu.ufps.legal_cases.business.service.persona.ocupacion;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.OcupacionDTO;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;

@Component
public class OcupacionMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public OcupacionDTO convertirADTO(Ocupacion ocupacion) {
        return new OcupacionDTO(
                ocupacion.getId(),
                ocupacion.getNombre(),
                ocupacion.getActivo());
    }

    public Ocupacion crearEntidad(String nombre) {
        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setNombre(nombre);
        ocupacion.setActivo(true);
        return ocupacion;
    }

    public void aplicarDatos(Ocupacion ocupacion, String nombre) {
        ocupacion.setNombre(nombre);
    }
}