package co.edu.ufps.legal_cases.business.repository.perfil;

public interface AsesorResumenProjection {

    Long getId();

    String getNombre();

    String getDocumento();

    String getEmail();

    String getUsuario();

    String getCodigo();

    Boolean getActivo();

    Long getAreaId();

    String getAreaNombre();

    Long getSedeId();

    String getSedeNombre();
}
