package co.edu.ufps.legal_cases.business.dto.conciliacion;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// No se hacen validaciones porque es una salida.
@Getter
@Setter
public class ConciliacionResponseDTO {

    private Long id;

    private Long consultaId;

    private Long estudianteId;

    private String estudianteNombre;

    private Long conciliadorId;

    private String conciliadorNombre;

    private Long estadoId;

    private String estadoCodigo;

    private String estadoNombre;

    private LocalDateTime fechaConciliacion;

    private String documentoSolicitudPath;

    private String actaPath;

    private Long solicitadoPorId;

    private String solicitadoPorUsername;

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private LocalDateTime fechaFinalizacion;
}