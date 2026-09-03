package co.edu.ufps.legal_cases.business.dto.persona;

/**
 * Contrato minimo para localizar y seleccionar personas sin exponer su ficha
 * completa.
 */
public record PersonaResumenDTO(
        Long id,
        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumentoEnmascarado,
        String tipoPersona,
        Boolean activo) {
}