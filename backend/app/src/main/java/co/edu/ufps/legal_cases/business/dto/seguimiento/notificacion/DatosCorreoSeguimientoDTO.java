package co.edu.ufps.legal_cases.business.dto.seguimiento.notificacion;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

// DTO interno para enviar solo lo necesario del seguimiento al servicio de correo
@Getter
@AllArgsConstructor
public class DatosCorreoSeguimientoDTO {

    private Long seguimientoId;

    private String descripcion;

    private String categoria;

    private Long consultaId;

    private LocalDate fechaEntrega;

    private Integer diasNotificacion;

    private Boolean notificarPartes;

    private Boolean notificarEstudiante;

    private Boolean alertaDisciplinaria;
}