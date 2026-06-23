package co.edu.ufps.legal_cases.business.service.catalogo.tipo;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;

@Component
public class TipoMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public TipoDTO convertirADTO(Tipo tipo) {
        return new TipoDTO(
                tipo.getId(),
                tipo.getNombre(),
                tipo.getTema().getId(),
                tipo.getActivo());
    }

    public Tipo crearEntidad(String nombre, Tema tema) {
        Tipo tipo = new Tipo();
        tipo.setNombre(nombre);
        tipo.setTema(tema);
        tipo.setActivo(true);
        return tipo;
    }

    public void aplicarDatos(Tipo tipo, String nombre, Tema tema) {
        tipo.setNombre(nombre);
        tipo.setTema(tema);
    }
}