package co.edu.ufps.legal_cases.business.dto.seguimiento;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeguimientoRequestDTO {

    private Long id;

    @NotBlank(message = "La descripción del seguimiento es obligatoria")
    @Size(max = 200, message = "La descripción del seguimiento no puede superar 200 caracteres")
    private String descripcion;

    // Si es null, no se programa recordatorio.
    @FutureOrPresent(message = "La fecha de entrega no puede ser anterior a la fecha actual")
    private LocalDate fechaEntrega;

    // Dias antes de la fecha de entrega para enviar el recordatorio.
    @Min(value = 0, message = "Los días de notificación no pueden ser negativos")
    private Integer diasNotificacion;

    // Notifica por correo a la persona principal, partes y contrapartes.
    private Boolean notificarPartes;

    // Notifica por correo al estudiante y permite que vea el seguimiento.
    private Boolean notificarEstudiante;

    // Marca el seguimiento como alerta disciplinaria para notificar administrativos.
    private Boolean alertaDisciplinaria;

    @NotNull(message = "La categoría del seguimiento es obligatoria")
    private Long categoriaSeguimientoId;

    @NotNull(message = "La consulta es obligatoria")
    private Long consultaId;

    // Regla condicional:
    // No se pueden definir días de notificación si no hay fecha de entrega.
    @AssertTrue(message = "No se pueden definir días de notificación sin fecha de entrega")
    public boolean isDiasNotificacionConsistente() {
        return diasNotificacion == null || fechaEntrega != null;
    }
}