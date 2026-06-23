package co.edu.ufps.legal_cases.business.service.catalogo.barrio;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.BarrioDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;

@Component
public class BarrioMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public BarrioDTO convertirADTO(Barrio barrio) {
        return new BarrioDTO(
                barrio.getId(),
                barrio.getNombre(),
                barrio.getMunicipio().getId(),
                barrio.getActivo());
    }

    public Barrio crearEntidad(String nombre, Municipio municipio) {
        Barrio barrio = new Barrio();
        barrio.setNombre(nombre);
        barrio.setMunicipio(municipio);
        barrio.setActivo(true);
        return barrio;
    }

    public void aplicarDatos(Barrio barrio, String nombre, Municipio municipio) {
        barrio.setNombre(nombre);
        barrio.setMunicipio(municipio);
    }
}