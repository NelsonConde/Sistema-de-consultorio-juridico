package co.edu.ufps.legal_cases.business.repository.perfil;

public interface AdministrativoResumenProjection {

    Long getId();

    String getNombre();

    String getDocumento();

    String getEmail();

    String getUsuario();

    String getCodigo();

    Boolean getActivo();

    Boolean getDirectora();

    Long getSedeId();

    String getSedeNombre();
}
