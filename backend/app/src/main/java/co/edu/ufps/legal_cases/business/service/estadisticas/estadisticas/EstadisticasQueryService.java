package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.SemestreDTO;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.estadisticas.periodo.PeriodoEstadistico;
import co.edu.ufps.legal_cases.business.service.estadisticas.periodo.PeriodoEstadisticoService;

// QueryService para estadísticas filtradas por periodo académico.
// Para rango libre de fechas usar EstadisticasRangoQueryService.
@Service
public class EstadisticasQueryService {

    private final ConsultaRepository consultaRepository;
    private final ProcesoRepository procesoRepository;
    private final ConciliacionRepository conciliacionRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstadisticasMapperService mapper;
    private final PeriodoEstadisticoService periodoEstadisticoService;

    public EstadisticasQueryService(
            ConsultaRepository consultaRepository,
            ProcesoRepository procesoRepository,
            ConciliacionRepository conciliacionRepository,
            SeguimientoRepository seguimientoRepository,
            EstudianteRepository estudianteRepository,
            EstadisticasMapperService mapper,
            PeriodoEstadisticoService periodoEstadisticoService) {

        this.consultaRepository = consultaRepository;
        this.procesoRepository = procesoRepository;
        this.conciliacionRepository = conciliacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.estudianteRepository = estudianteRepository;
        this.mapper = mapper;
        this.periodoEstadisticoService = periodoEstadisticoService;
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerEstadisticasSemestre(
            int año,
            int semestre) {

        PeriodoEstadistico periodo =
                periodoEstadisticoService.obtener(año, semestre);

        long[] conteos = mapper.extraerFinalizadasYPendientes(
                consultaRepository
                        .contarFinalizadasYPendientesPorPeriodoRaw(
                                periodo.inicio(),
                                periodo.fin()));

        long finalizadas = conteos[0];
        long pendientes = conteos[1];

        long totalPersonas = mapper.extraerEscalar(
                consultaRepository
                        .contarPersonasAtendidasPorPeriodo(
                                periodo.inicio(),
                                periodo.fin()));

        long totalConciliaciones = mapper.extraerEscalar(
                conciliacionRepository
                        .contarConciliacionesPorPeriodo(
                                periodo.inicio(),
                                periodo.fin()));

        long totalSeguimientos = mapper.extraerEscalar(
                seguimientoRepository
                        .contarSeguimientosPorPeriodo(
                                periodo.inicio(),
                                periodo.fin()));

        long totalEstudiantes =
                estudianteRepository
                        .findByActivoTrueOrderByNombreAsc()
                        .size();

        long totalEstudiantesConciliacion =
                estudianteRepository
                        .findByConciliacionTrueAndActivoTrue()
                        .size();

        return EstadisticasSemestreDTO.builder()
                .año(periodo.año())
                .semestre(periodo.semestre())
                .periodoInicio(periodo.inicio().toString())
                .periodoFin(periodo.fin().toString())
                .consultasFinalizadas(finalizadas)
                .consultasPendientes(pendientes)
                .totalConsultas(finalizadas + pendientes)
                .consultasPorEstado(mapper.mapear2(
                        consultaRepository
                                .contarConsultasPorEstadoPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .consultasPorArea(mapper.mapear3(
                        consultaRepository
                                .contarConsultasPorAreaPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .consultasPorTipoViolencia(mapper.mapear2(
                        consultaRepository
                                .contarConsultasPorTipoViolenciaPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .totalPersonasAtendidas(totalPersonas)
                .personasPorGenero(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorGeneroPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .personasPorEstrato(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorEstratoPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .personasPorZona(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorZonaPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .personasPorGrupoEtnico(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorGrupoEtnicoPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .personasPorMunicipio(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorMunicipioPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .personasPorCondicion(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorCondicionPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .procesosPorEstado(mapper.mapear2(
                        procesoRepository
                                .contarProcesosPorEstadoPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .totalConciliaciones(totalConciliaciones)
                .conciliacionesPorEstado(mapper.mapear2(
                        conciliacionRepository
                                .contarConciliacionesPorEstadoPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .totalSeguimientos(totalSeguimientos)
                .seguimientosPorEstado(mapper.mapear2(
                        seguimientoRepository
                                .contarSeguimientosPorEstadoPorPeriodo(
                                        periodo.inicio(),
                                        periodo.fin())))
                .totalEstudiantesActivos(totalEstudiantes)
                .totalEstudiantesHabilitadosConciliacion(
                        totalEstudiantesConciliacion)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SemestreDTO> listarSemestresDisponibles() {

        return periodoEstadisticoService
                .listarDisponibles()
                .stream()
                .map(periodo -> new SemestreDTO(
                        periodo.año(),
                        periodo.semestre(),
                        periodo.año() + "-" + periodo.semestre(),
                        periodo.inicio().toString(),
                        periodo.fin().toString()))
                .toList();
    }
}