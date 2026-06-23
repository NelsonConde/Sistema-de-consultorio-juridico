package co.edu.ufps.legal_cases.business.service.catalogo.municipio;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.MunicipioDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;

@Component
public class MunicipioMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public MunicipioDTO convertirADTO(Municipio municipio) {
        return new MunicipioDTO(
                municipio.getId(),
                municipio.getNombre(),
                municipio.getDepartamento().getId(),
                municipio.getActivo());
    }

    public Municipio crearEntidad(String nombre, Departamento departamento) {
        Municipio municipio = new Municipio();
        municipio.setNombre(nombre);
        municipio.setDepartamento(departamento);
        municipio.setActivo(true);
        return municipio;
    }

    public void aplicarDatos(Municipio municipio, String nombre, Departamento departamento) {
        municipio.setNombre(nombre);
        municipio.setDepartamento(departamento);
    }
}