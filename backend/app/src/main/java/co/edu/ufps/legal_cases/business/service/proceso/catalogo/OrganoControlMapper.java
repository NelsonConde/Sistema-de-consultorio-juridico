package co.edu.ufps.legal_cases.business.service.proceso.catalogo;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.proceso.OrganoControlDTO;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;

@Component
public class OrganoControlMapper {

    // Define cómo se expone el órgano de control hacia la API.
    public OrganoControlDTO convertirADTO(OrganoControl organoControl) {
        return new OrganoControlDTO(
                organoControl.getId(),
                organoControl.getNombre(),
                organoControl.getActivo());
    }

    public OrganoControl crearEntidad(String nombre) {
        OrganoControl organoControl = new OrganoControl();
        organoControl.setNombre(nombre);
        organoControl.setActivo(true);
        return organoControl;
    }

    public void aplicarDatos(OrganoControl organoControl, String nombre) {
        organoControl.setNombre(nombre);
    }
}