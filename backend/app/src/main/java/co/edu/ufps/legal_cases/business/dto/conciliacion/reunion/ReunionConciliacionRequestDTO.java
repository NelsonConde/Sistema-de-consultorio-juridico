package co.edu.ufps.legal_cases.business.dto.conciliacion.reunion;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReunionConciliacionRequestDTO {

    @NotNull(message = "La fecha de la reunión es obligatoria")
    private LocalDateTime fechaReunion;

    @NotNull(message = "La sede de la reunión es obligatoria")
    private Long sedeId;

    @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
    private String observaciones;
}
