package co.edu.ufps.legal_cases.business.dto.seguimiento;

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoResumenDTO {

    private Long id;

    private Long version;

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
