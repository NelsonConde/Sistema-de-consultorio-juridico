package co.edu.ufps.legal_cases.business.repository.consulta;

import java.time.Instant;

/**
 * Proyección de solo lectura para evitar exponer identificadores polimórficos
 * y resolver los nombres en una única consulta paginada.
 */
public interface ConsultaReasignacionHistorialResumenProjection {

    Long getId();

    String getTipoResponsable();

    String getResponsableAnteriorNombre();

    String getResponsableNuevoNombre();

    String getMotivo();

    String getActorNombre();

    Instant getOcurridoEn();
}
