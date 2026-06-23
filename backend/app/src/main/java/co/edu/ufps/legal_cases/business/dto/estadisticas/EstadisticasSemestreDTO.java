package co.edu.ufps.legal_cases.business.dto.estadisticas;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO de respuesta para las estadísticas de un semestre.
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasSemestreDTO {

    private Integer año;
    private Integer semestre;
    private String periodoInicio;
    private String periodoFin;

    // --- Consultas ---
    private long consultasFinalizadas;
    private long consultasPendientes;
    private long totalConsultas;
    private List<ConteoDTO> consultasPorEstado;
    private List<ConteoDTO> consultasPorArea;
    private List<ConteoDTO> consultasPorTipoViolencia;

    // --- Personas ---
    private long totalPersonasAtendidas;
    private List<ConteoDTO> personasPorGenero;
    private List<ConteoDTO> personasPorEstrato;
    private List<ConteoDTO> personasPorZona;
    private List<ConteoDTO> personasPorGrupoEtnico;
    private List<ConteoDTO> personasPorMunicipio;
    private List<ConteoDTO> personasPorCondicion;

    // --- Procesos ---
    private List<ConteoDTO> procesosPorEstado;

    // --- Conciliaciones ---
    private long totalConciliaciones;
    private List<ConteoDTO> conciliacionesPorEstado;

    // --- Seguimientos ---
    private long totalSeguimientos;
    private List<ConteoDTO> seguimientosPorEstado;

    // --- Académico ---
    private long totalEstudiantesActivos;
    private long totalEstudiantesHabilitadosConciliacion;
}