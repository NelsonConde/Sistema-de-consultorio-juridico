package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import java.time.LocalDate;
import java.util.ArrayList;
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
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// QueryService para estadísticas filtradas por semestre predefinido.
// Para rango libre de fechas usar EstadisticasRangoQueryService.
@Service
public class EstadisticasQueryService {

    private static final int AÑO_MINIMO = 2024;

    private final ConsultaRepository consultaRepository;
    private final ProcesoRepository procesoRepository;
    private final ConciliacionRepository conciliacionRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstadisticasMapperService mapper;

    public EstadisticasQueryService(
            ConsultaRepository consultaRepository,
            ProcesoRepository procesoRepository,
            ConciliacionRepository conciliacionRepository,
            SeguimientoRepository seguimientoRepository,
            EstudianteRepository estudianteRepository,
            EstadisticasMapperService mapper) {
        this.consultaRepository = consultaRepository;
        this.procesoRepository = procesoRepository;
        this.conciliacionRepository = conciliacionRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.estudianteRepository = estudianteRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerEstadisticasSemestre(int año, int semestre) {
        validarParametrosSemestre(año, semestre);

        long[] conteos = mapper.extraerFinalizadasYPendientes(
                consultaRepository.contarFinalizadasYPendientesPorSemestreRaw(año, semestre));
        long finalizadas = conteos[0];
        long pendientes = conteos[1];

        long totalPersonas = mapper.extraerEscalar(
                consultaRepository.contarPersonasAtendidasPorSemestre(año, semestre));

        long totalConciliaciones = mapper.extraerEscalar(
                conciliacionRepository.contarConciliacionesPorSemestre(año, semestre));

        long totalSeguimientos = mapper.extraerEscalar(
                seguimientoRepository.contarSeguimientosPorSemestre(año, semestre));

        long totalEstudiantes = estudianteRepository.findByActivoTrueOrderByNombreAsc().size();
        long totalEstudiantesConciliacion = estudianteRepository
                .findByConciliacionTrueAndActivoTrue().size();

        return EstadisticasSemestreDTO.builder()
                .año(año).semestre(semestre)
                .periodoInicio(calcularInicio(año, semestre).toString())
                .periodoFin(calcularFin(año, semestre).toString())
                .consultasFinalizadas(finalizadas)
                .consultasPendientes(pendientes)
                .totalConsultas(finalizadas + pendientes)
                .consultasPorEstado(mapper.mapear2(
                        consultaRepository.contarConsultasPorEstadoPorSemestre(año, semestre)))
                .consultasPorArea(mapper.mapear3(
                        consultaRepository.contarConsultasPorAreaPorSemestre(año, semestre)))
                .consultasPorTipoViolencia(mapper.mapear2(
                        consultaRepository.contarConsultasPorTipoViolenciaPorSemestre(año, semestre)))
                .totalPersonasAtendidas(totalPersonas)
                .personasPorGenero(mapper.mapear2(
                        consultaRepository.contarPersonasPorGeneroPorSemestre(año, semestre)))
                .personasPorEstrato(mapper.mapear2(
                        consultaRepository.contarPersonasPorEstratoPorSemestre(año, semestre)))
                .personasPorZona(mapper.mapear2(
                        consultaRepository.contarPersonasPorZonaPorSemestre(año, semestre)))
                .personasPorGrupoEtnico(mapper.mapear2(
                        consultaRepository.contarPersonasPorGrupoEtnicoPorSemestre(año, semestre)))
                .personasPorMunicipio(mapper.mapear2(
                        consultaRepository.contarPersonasPorMunicipioPorSemestre(año, semestre)))
                .personasPorCondicion(mapper.mapear2(
                        consultaRepository.contarPersonasPorCondicionPorSemestre(año, semestre)))
                .procesosPorEstado(mapper.mapear2(
                        procesoRepository.contarProcesosPorEstado()))
                .totalConciliaciones(totalConciliaciones)
                .conciliacionesPorEstado(mapper.mapear2(
                        conciliacionRepository.contarConciliacionesPorEstadoPorSemestre(año, semestre)))
                .totalSeguimientos(totalSeguimientos)
                .seguimientosPorEstado(mapper.mapear2(
                        seguimientoRepository.contarSeguimientosPorEstadoPorSemestre(año, semestre)))
                .totalEstudiantesActivos(totalEstudiantes)
                .totalEstudiantesHabilitadosConciliacion(totalEstudiantesConciliacion)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SemestreDTO> listarSemestresDisponibles() {
        List<SemestreDTO> semestres = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        int añoActual = hoy.getYear();

        for (int año = AÑO_MINIMO; año <= añoActual; año++) {
            LocalDate inicioS1 = LocalDate.of(año, 1, 1);
            if (!inicioS1.isAfter(hoy)) {
                semestres.add(new SemestreDTO(año, 1, año + "-1",
                        inicioS1.toString(), LocalDate.of(año, 6, 30).toString()));
            }
            LocalDate inicioS2 = LocalDate.of(año, 7, 1);
            if (!inicioS2.isAfter(hoy)) {
                semestres.add(new SemestreDTO(año, 2, año + "-2",
                        inicioS2.toString(), LocalDate.of(año, 12, 31).toString()));
            }
        }
        return semestres;
    }

    private void validarParametrosSemestre(int año, int semestre) {
        if (semestre < 1 || semestre > 2)
            throw new BusinessException("El semestre debe ser 1 o 2");
        if (año < AÑO_MINIMO)
            throw new BusinessException("No hay datos disponibles antes del año " + AÑO_MINIMO);
        if (calcularInicio(año, semestre).isAfter(LocalDate.now()))
            throw new BusinessException(
                    "No se pueden consultar estadísticas de un semestre que aún no ha comenzado");
    }

    private LocalDate calcularInicio(int año, int semestre) {
        return semestre == 1 ? LocalDate.of(año, 1, 1) : LocalDate.of(año, 7, 1);
    }

    private LocalDate calcularFin(int año, int semestre) {
        return semestre == 1 ? LocalDate.of(año, 6, 30) : LocalDate.of(año, 12, 31);
    }
}