package co.edu.ufps.legal_cases.business.service.catalogo.departamento;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.catalogo.DepartamentoDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;

@Component
public class DepartamentoMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public DepartamentoDTO convertirADTO(Departamento departamento) {
        return new DepartamentoDTO(
                departamento.getId(),
                departamento.getNombre(),
                departamento.getActivo());
    }

    public Departamento crearEntidad(String nombre) {
        Departamento departamento = new Departamento();
        departamento.setNombre(nombre);
        departamento.setActivo(true);
        return departamento;
    }

    public void aplicarDatos(Departamento departamento, String nombre) {
        departamento.setNombre(nombre);
    }
}