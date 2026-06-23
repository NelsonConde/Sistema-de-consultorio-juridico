package co.edu.ufps.legal_cases.business.service.catalogo.tema;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.TemaDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;

@Component
public class TemaMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public TemaDTO convertirADTO(Tema tema) {
        return new TemaDTO(
                tema.getId(),
                tema.getNombre(),
                tema.getArea().getId(),
                tema.getActivo());
    }

    public Tema crearEntidad(String nombre, Area area) {
        Tema tema = new Tema();
        tema.setNombre(nombre);
        tema.setArea(area);
        tema.setActivo(true);
        return tema;
    }

    public void aplicarDatos(Tema tema, String nombre, Area area) {
        tema.setNombre(nombre);
        tema.setArea(area);
    }
}