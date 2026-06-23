package co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion;

import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.TipoDestinatarioReunionConciliacion;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReunionConciliacionDestinatarioDTO {

    private String email;

    private String nombre;

    private TipoDestinatarioReunionConciliacion tipoDestinatario;
}
