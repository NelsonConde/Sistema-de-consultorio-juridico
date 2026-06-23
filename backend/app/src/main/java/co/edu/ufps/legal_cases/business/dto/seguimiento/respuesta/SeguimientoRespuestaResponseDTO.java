package co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import lombok.Getter;
import lombok.Setter;

// No se hacen validaciones porque es una salida.
@Getter
@Setter
public class SeguimientoRespuestaResponseDTO {

    private Long id;

    private Long seguimientoId;

    private Long consultaId;

    private Long estudianteId;

    private String estudianteNombre;

    private String contenido;

    private EstadoRespuestaSeguimiento estado;

    // Permite que el frontend muestre si la respuesta fue enviada fuera del plazo.
    private Boolean fueraPlazo;

    private String observacionRevision;

    private Long revisadoPorId;

    private String revisadoPorUsername;

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private LocalDateTime fechaDecision;
}