package co.edu.ufps.legal_cases.business.dto.seguimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import lombok.Getter;
import lombok.Setter;

// No se hacen validaciones porque es una salida
@Getter
@Setter
public class SeguimientoResponseDTO {

    private Long id;

    private String descripcion;

    private LocalDate fechaEntrega;

    private Integer diasNotificacion;

    private Boolean notificarPartes;

    private Boolean notificarEstudiante;

    private Boolean alertaDisciplinaria;

    private EstadoSeguimiento estado;

    private Long categoriaSeguimientoId;

    private String categoriaSeguimientoNombre;

    private Long consultaId;

    private Long autorId;

    private String autorUsername;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}