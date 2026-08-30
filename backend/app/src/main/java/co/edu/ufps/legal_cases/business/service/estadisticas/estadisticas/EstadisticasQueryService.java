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

// QueryService para estadísticas filtradas por semestre predefinido.
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
                        .contarFinalizadasYPendientesPorSemestreRaw(
                                año,
                                semestre));

        long finalizadas = conteos[0];
        long pendientes = conteos[1];

        long totalPersonas = mapper.extraerEscalar(
                consultaRepository.contarPersonasAtendidasPorSemestre(
                        año,
                        semestre));

        long totalConciliaciones = mapper.extraerEscalar(
                conciliacionRepository.contarConciliacionesPorSemestre(
                        año,
                        semestre));

        long totalSeguimientos = mapper.extraerEscalar(
                seguimientoRepository.contarSeguimientosPorSemestre(
                        año,
                        semestre));

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
                                .contarConsultasPorEstadoPorSemestre(
                                        año,
                                        semestre)))
                .consultasPorArea(mapper.mapear3(
                        consultaRepository
                                .contarConsultasPorAreaPorSemestre(
                                        año,
                                        semestre)))
                .consultasPorTipoViolencia(mapper.mapear2(
                        consultaRepository
                                .contarConsultasPorTipoViolenciaPorSemestre(
                                        año,
                                        semestre)))
                .totalPersonasAtendidas(totalPersonas)
                .personasPorGenero(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorGeneroPorSemestre(
                                        año,
                                        semestre)))
                .personasPorEstrato(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorEstratoPorSemestre(
                                        año,
                                        semestre)))
                .personasPorZona(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorZonaPorSemestre(
                                        año,
                                        semestre)))
                .personasPorGrupoEtnico(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorGrupoEtnicoPorSemestre(
                                        año,
                                        semestre)))
                .personasPorMunicipio(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorMunicipioPorSemestre(
                                        año,
                                        semestre)))
                .personasPorCondicion(mapper.mapear2(
                        consultaRepository
                                .contarPersonasPorCondicionPorSemestre(
                                        año,
                                        semestre)))
                .procesosPorEstado(mapper.mapear2(
                        procesoRepository.contarProcesosPorEstado()))
                .totalConciliaciones(totalConciliaciones)
                .conciliacionesPorEstado(mapper.mapear2(
                        conciliacionRepository
                                .contarConciliacionesPorEstadoPorSemestre(
                                        año,
                                        semestre)))
                .totalSeguimientos(totalSeguimientos)
                .seguimientosPorEstado(mapper.mapear2(
                        seguimientoRepository
                                .contarSeguimientosPorEstadoPorSemestre(
                                        año,
                                        semestre)))
                .totalEstudiantesActivos(totalEstudiantes)
                .totalEstudiantesHabilitadosConciliacion(
                        totalEstudiantesConciliacion)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SemestreDTO> listarSemestresDisponibles() {
        return periodoEstadisticoService.listarDisponibles()
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