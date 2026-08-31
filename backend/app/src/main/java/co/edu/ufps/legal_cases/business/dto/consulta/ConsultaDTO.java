package co.edu.ufps.legal_cases.business.dto.consulta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultaDTO {

    private Long id;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    @NotBlank(message = "Los hechos son obligatorios")
    private String hechos;

    @NotBlank(message = "Las pretensiones son obligatorias")
    private String pretensiones;

    @NotBlank(message = "El concepto jurídico es obligatorio")
    private String conceptoJuridico;

    @NotBlank(message = "El trámite es obligatorio")
    @Size(max = 100, message = "El trámite no puede superar los 100 caracteres")
    private String tramite;

    private String observaciones;

    @Size(max = 100, message = "El tipo de violencia no puede superar los 100 caracteres")
    private String tipoViolencia;

    private EstadoConsulta estado;

    @Size(max = 100, message = "El resultado no puede superar los 100 caracteres")
    private String resultado;

    @NotNull(message = "La persona es obligatoria")
    private Long personaId;

    // IDs de las partes (ManyToMany)
    private List<Long> partesIds = new ArrayList<>();

    // IDs de las contrapartes (ManyToMany)
    private List<Long> contrapartesIds = new ArrayList<>();

    @NotNull(message = "La sede es obligatoria")
    private Long sedeId;

    @NotNull(message = "El área es obligatoria")
    private Long areaId;

    @NotNull(message = "El tema es obligatorio")
    private Long temaId;

    private Long tipoId;
    private Long asesorId;
    private Long monitorId;
    private Long estudianteId;
}