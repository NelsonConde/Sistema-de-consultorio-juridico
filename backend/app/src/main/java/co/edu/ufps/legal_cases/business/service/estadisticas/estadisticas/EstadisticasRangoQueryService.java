package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// QueryService para estadísticas con rango libre de fechas.
// Permite reportes históricos sin estar atados a semestres predefinidos.
@Service
public class EstadisticasRangoQueryService {

    private final ConsultaRepository consultaRepository;
    private final ProcesoRepository procesoRepository;
    private final ConciliacionRepository conciliacionRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstadisticasMapperService mapper;

    public EstadisticasRangoQueryService(
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
    public EstadisticasSemestreDTO obtenerPorRango(LocalDate fechaInicio, LocalDate fechaFin) {
        validar(fechaInicio, fechaFin);

        String inicio = fechaInicio.toString();
        String fin = fechaFin.toString();

        long[] conteos = mapper.extraerFinalizadasYPendientes(
                consultaRepository.contarFinalizadasYPendientesPorRango(inicio, fin));
        long finalizadas = conteos[0];
        long pendientes = conteos[1];

        long totalPersonas = mapper.extraerEscalar(
                consultaRepository.contarPersonasAtendidasPorRango(inicio, fin));

        long totalConciliaciones = mapper.extraerEscalar(
                conciliacionRepository.contarConciliacionesPorRango(inicio, fin));

        long totalSeguimientos = mapper.extraerEscalar(
                seguimientoRepository.contarSeguimientosPorRango(inicio, fin));

        long totalEstudiantes = estudianteRepository.findByActivoTrueOrderByNombreAsc().size();
        long totalEstudiantesConciliacion = estudianteRepository
                .findByConciliacionTrueAndActivoTrue().size();

        return EstadisticasSemestreDTO.builder()
                .año(null)
                .semestre(null)
                .periodoInicio(inicio)
                .periodoFin(fin)
                .consultasFinalizadas(finalizadas)
                .consultasPendientes(pendientes)
                .totalConsultas(finalizadas + pendientes)
                .consultasPorEstado(mapper.mapear2(
                        consultaRepository.contarConsultasPorEstadoPorRango(inicio, fin)))
                .consultasPorArea(mapper.mapear3(
                        consultaRepository.contarConsultasPorAreaPorRango(inicio, fin)))
                .consultasPorTipoViolencia(mapper.mapear2(
                        consultaRepository.contarConsultasPorTipoViolenciaPorRango(inicio, fin)))
                .totalPersonasAtendidas(totalPersonas)
                .personasPorGenero(mapper.mapear2(
                        consultaRepository.contarPersonasPorGeneroPorRango(inicio, fin)))
                .personasPorEstrato(mapper.mapear2(
                        consultaRepository.contarPersonasPorEstratoPorRango(inicio, fin)))
                .personasPorZona(mapper.mapear2(
                        consultaRepository.contarPersonasPorZonaPorRango(inicio, fin)))
                .personasPorGrupoEtnico(mapper.mapear2(
                        consultaRepository.contarPersonasPorGrupoEtnicoPorRango(inicio, fin)))
                .personasPorMunicipio(mapper.mapear2(
                        consultaRepository.contarPersonasPorMunicipioPorRango(inicio, fin)))
                .personasPorCondicion(mapper.mapear2(
                        consultaRepository.contarPersonasPorCondicionPorRango(inicio, fin)))
                .procesosPorEstado(mapper.mapear2(
                        procesoRepository.contarProcesosPorEstado()))
                .totalConciliaciones(totalConciliaciones)
                .conciliacionesPorEstado(mapper.mapear2(
                        conciliacionRepository.contarConciliacionesPorEstadoPorRango(inicio, fin)))
                .totalSeguimientos(totalSeguimientos)
                .seguimientosPorEstado(mapper.mapear2(
                        seguimientoRepository.contarSeguimientosPorEstadoPorRango(inicio, fin)))
                .totalEstudiantesActivos(totalEstudiantes)
                .totalEstudiantesHabilitadosConciliacion(totalEstudiantesConciliacion)
                .build();
    }

    private void validar(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null)
            throw new BusinessException("Las fechas de inicio y fin son obligatorias");
        if (fechaInicio.isAfter(fechaFin))
            throw new BusinessException("La fecha de inicio no puede ser posterior a la fecha fin");
        if (fechaInicio.getYear() < 2024)
            throw new BusinessException("No hay datos disponibles antes del año 2024");
        if (fechaInicio.isAfter(LocalDate.now()))
            throw new BusinessException("La fecha de inicio no puede ser futura");
    }
}