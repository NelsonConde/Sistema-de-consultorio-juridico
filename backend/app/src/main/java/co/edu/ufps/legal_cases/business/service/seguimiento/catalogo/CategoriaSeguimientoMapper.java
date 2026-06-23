package co.edu.ufps.legal_cases.business.service.seguimiento.catalogo;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.seguimiento.CategoriaSeguimientoDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;

@Component
public class CategoriaSeguimientoMapper {

    // Define cómo se expone la categoría hacia la API.
    public CategoriaSeguimientoDTO convertirADTO(CategoriaSeguimiento categoria) {
        CategoriaSeguimientoDTO dto = new CategoriaSeguimientoDTO();

        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setActivo(categoria.getActivo());

        return dto;
    }

    public CategoriaSeguimiento crearEntidad(String nombre) {
        CategoriaSeguimiento categoria = new CategoriaSeguimiento();

        categoria.setNombre(nombre);
        categoria.setActivo(true);

        return categoria;
    }

    public void aplicarDatos(CategoriaSeguimiento categoria, String nombre) {
        categoria.setNombre(nombre);
    }
}