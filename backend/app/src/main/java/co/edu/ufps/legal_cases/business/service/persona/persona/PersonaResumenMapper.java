package co.edu.ufps.legal_cases.business.service.persona.persona;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;

/**
 * Construye el contrato minimo de listado y centraliza la politica temporal de
 * enmascaramiento definida.
 */
@Component
public class PersonaResumenMapper {

    private static final int CARACTERES_VISIBLES_DOCUMENTO = 4;

    public PersonaResumenDTO convertirAResumen(PersonaResumenProjection persona) {
        return new PersonaResumenDTO(
                persona.getId(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getTipoDocumento(),
                enmascararDocumento(persona.getNumeroDocumento()),
                persona.getTipoPersona(),
                persona.getActivo());
    }

    String enmascararDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return null;
        }

        int longitud = numeroDocumento.length();

        if (longitud <= CARACTERES_VISIBLES_DOCUMENTO) {
            return "*".repeat(longitud);
        }

        int inicioVisible = longitud - CARACTERES_VISIBLES_DOCUMENTO;
        return "*".repeat(inicioVisible) + numeroDocumento.substring(inicioVisible);
    }
}