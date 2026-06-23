package co.edu.ufps.legal_cases.business.service.persona.empresa;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.EmpresaDTO;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;

@Component
public class EmpresaMapper {

    // Convierte la entidad a DTO para evitar exponer directamente el modelo.
    public EmpresaDTO convertirADTO(Empresa empresa) {
        return new EmpresaDTO(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getActivo());
    }

    public Empresa crearEntidad(String nombre) {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        empresa.setActivo(true);
        return empresa;
    }

    public void aplicarDatos(Empresa empresa, String nombre) {
        empresa.setNombre(nombre);
    }
}