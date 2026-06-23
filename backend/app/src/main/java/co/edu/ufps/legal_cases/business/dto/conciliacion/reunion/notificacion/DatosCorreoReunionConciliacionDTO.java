package co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.notificacion;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MotivoNotificacionReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.notificacion.MomentoNotificacionReunionConciliacion;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DatosCorreoReunionConciliacionDTO {

    private Long conciliacionId;

    private Long consultaId;

    private LocalDateTime fechaReunion;

    private String sedeNombre;

    private String observaciones;

    private MotivoNotificacionReunionConciliacion motivo;

    private MomentoNotificacionReunionConciliacion momentoNotificacion;

    private String detalleError;
}
