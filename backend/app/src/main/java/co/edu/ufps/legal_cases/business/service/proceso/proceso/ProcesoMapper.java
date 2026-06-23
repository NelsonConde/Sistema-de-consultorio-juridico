package co.edu.ufps.legal_cases.business.service.proceso.proceso;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoDTO;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;

// Convierte procesos entre entidad y DTO.
// El service coordina el flujo, pero este mapper mantiene en un solo lugar cómo se expone el proceso hacia la API.
@Component
public class ProcesoMapper {

    public ProcesoDTO convertirADTO(Proceso proceso) {
        return new ProcesoDTO(
                proceso.getId(),
                proceso.getNumeroRadicado(),
                proceso.getDepartamento().getId(),
                proceso.getConsulta().getId(),
                proceso.getEspecialidad() != null
                        ? proceso.getEspecialidad().getId()
                        : null,
                proceso.getOrganoControl() != null
                        ? proceso.getOrganoControl().getId()
                        : null,
                proceso.getEstado(),
                proceso.getActivo());
    }

    public void aplicarDatos(Proceso proceso, DatosProceso datos) {
        proceso.setNumeroRadicado(datos.numeroRadicado());
        proceso.setDepartamento(datos.departamento());
        proceso.setConsulta(datos.consulta());
        proceso.setOrganoControl(datos.organoControl());
        proceso.setEspecialidad(datos.especialidad());
    }
}