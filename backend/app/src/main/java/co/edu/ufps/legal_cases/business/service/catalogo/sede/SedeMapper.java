package co.edu.ufps.legal_cases.business.service.catalogo.sede;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.SedeDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;

@Component
public class SedeMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public SedeDTO convertirADTO(Sede sede) {
        SedeDTO dto = new SedeDTO();

        dto.setId(sede.getId());
        dto.setNombre(sede.getNombre());
        dto.setActivo(sede.getActivo());

        return dto;
    }

    public Sede crearEntidad(String nombre) {
        Sede sede = new Sede();
        sede.setNombre(nombre);
        sede.setActivo(true);
        return sede;
    }

    public void aplicarDatos(Sede sede, String nombre) {
        sede.setNombre(nombre);
    }
}