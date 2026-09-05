package co.edu.ufps.legal_cases.business.repository.perfil;

public interface MonitorResumenProjection {

    Long getId();

    String getNombre();

    String getDocumento();

    String getEmail();

    String getUsuario();

    String getCodigo();

    Boolean getActivo();

    Long getSedeId();

    String getSedeNombre();
}
