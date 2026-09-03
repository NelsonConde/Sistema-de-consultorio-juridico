package co.edu.ufps.legal_cases.business.repository.persona;

/**
 * Proyeccion cerrada para que el buscador recupere solamente las columnas que
 * necesita el resumen. El documento se obtiene exclusivamente para aplicar la
 * politica de enmascaramiento antes de construir la respuesta publica.
 */
public interface PersonaResumenProjection {

    Long getId();

    String getNombres();

    String getApellidos();

    String getTipoDocumento();

    String getNumeroDocumento();

    String getTipoPersona();

    Boolean getActivo();
}