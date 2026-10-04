package co.edu.ufps.legal_cases.business.repository.perfil;

import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;

public interface ConciliadorResumenProjection {

    Long getId();

    String getNombre();

    String getDocumento();

    String getEmail();

    String getUsuario();

    String getCodigo();

    Boolean getActivo();

    TipoConciliador getTipoConciliador();

    Long getSedeId();

    String getSedeNombre();
}
