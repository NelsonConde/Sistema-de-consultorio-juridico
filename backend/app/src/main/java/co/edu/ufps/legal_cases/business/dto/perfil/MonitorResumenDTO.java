package co.edu.ufps.legal_cases.business.dto.perfil;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitorResumenDTO {

    private Long id;

    private String nombre;

    private String documento;

    private String email;

    private String usuario;

    private String codigo;

    private Boolean activo;

    private Long sedeId;

    private String sedeNombre;
}
