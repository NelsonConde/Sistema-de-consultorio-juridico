package co.edu.ufps.legal_cases.business.service.catalogo.area;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.AreaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;

@Component
public class AreaMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public AreaDTO convertirADTO(Area area) {
        return new AreaDTO(
                area.getId(),
                area.getNombre(),
                area.getActivo());
    }

    public Area crearEntidad(String nombre) {
        Area area = new Area();
        area.setNombre(nombre);
        area.setActivo(true);
        return area;
    }

    public void aplicarDatos(Area area, String nombre) {
        area.setNombre(nombre);
    }
}