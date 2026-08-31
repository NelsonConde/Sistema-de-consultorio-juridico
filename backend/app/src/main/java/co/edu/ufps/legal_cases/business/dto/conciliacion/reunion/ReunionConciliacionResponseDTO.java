package co.edu.ufps.legal_cases.business.dto.conciliacion.reunion;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// No se hacen validaciones porque es una salida.
@Getter
@Setter
public class ReunionConciliacionResponseDTO {

    private Long conciliacionId;

    private LocalDateTime fechaReunion;

    private Long sedeId;

    private String sedeNombre;

    private String observaciones;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
