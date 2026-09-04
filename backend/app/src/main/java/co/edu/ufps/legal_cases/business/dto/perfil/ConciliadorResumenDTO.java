package co.edu.ufps.legal_cases.business.dto.perfil;

import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import lombok.Data;

@Data
public class ConciliadorResumenDTO {

    private Long id;
    private String nombre;
    private String documento;
    private String email;
    private String usuario;
    private String codigo;
    private Boolean activo;
    private TipoConciliador tipoConciliador;
    
    // Proyectar sede si es necesario (el baseline dice "puede proyectar: Long sedeId, String sedeNombre")
    private Long sedeId;
    private String sedeNombre;
}
