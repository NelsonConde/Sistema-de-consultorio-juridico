package co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DatosNotificacionSeguimientoDTO {

    private Long seguimientoId;

    private Long consultaId;

    private Long autorUsuarioSistemaId;

    // En UsuarioSistema, username corresponde al correo usado para notificaciones
    private String autorEmail;
}