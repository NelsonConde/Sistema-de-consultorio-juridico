package co.edu.ufps.legal_cases.business.service.catalogo.nacionalidad;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.NacionalidadDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;

@Component
public class NacionalidadMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public NacionalidadDTO convertirADTO(Nacionalidad nacionalidad) {
        return new NacionalidadDTO(
                nacionalidad.getId(),
                nacionalidad.getNombre(),
                nacionalidad.getActivo());
    }

    public Nacionalidad crearEntidad(String nombre) {
        Nacionalidad nacionalidad = new Nacionalidad();
        nacionalidad.setNombre(nombre);
        nacionalidad.setActivo(true);
        return nacionalidad;
    }

    public void aplicarDatos(Nacionalidad nacionalidad, String nombre) {
        nacionalidad.setNombre(nombre);
    }
}