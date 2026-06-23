package co.edu.ufps.legal_cases.business.dto.conciliacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import lombok.Getter;
import lombok.Setter;

// No se hacen validaciones porque es una salida.
@Getter
@Setter
public class ConciliacionDetalleResponseDTO {

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

    private ReunionConciliacionResponseDTO reunion;

    private ConciliacionPersonaDTO consultante;

    private List<ConciliacionPersonaDTO> partes = new ArrayList<>();

    private List<ConciliacionPersonaDTO> contrapartes = new ArrayList<>();

    private String documentoSolicitudPath;

    private String actaPath;

    private Long solicitadoPorId;

    private String solicitadoPorUsername;

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private LocalDateTime fechaFinalizacion;
}
