package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.estadisticas.ConteoDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.service.estadisticas.periodo.PeriodoEstadistico;
import co.edu.ufps.legal_cases.business.service.estadisticas.periodo.PeriodoEstadisticoService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// QueryService para estadísticas filtradas por perfil.
// Estudiante ve sus propias consultas.
// Asesor y monitor ven las consultas donde están asignados.
@Service
public class EstadisticasPerfilQueryService {

    private final ConsultaRepository consultaRepository;
    private final ProcesoRepository procesoRepository;
    private final PeriodoEstadisticoService periodoEstadisticoService;

    public EstadisticasPerfilQueryService(
            ConsultaRepository consultaRepository,
            ProcesoRepository procesoRepository,
            PeriodoEstadisticoService periodoEstadisticoService) {
        this.consultaRepository = consultaRepository;
        this.procesoRepository = procesoRepository;
        this.periodoEstadisticoService = periodoEstadisticoService;
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerPorEstudiante(
            int año,
            int semestre,
            Long estudianteId) {

        PeriodoEstadistico periodo =
                validar(año, semestre, estudianteId, "estudiante");

        List<Object[]> consultas =
                consultaRepository
                        .contarFinalizadasYPendientesPorSemestreYEstudiante(
                                año,
                                semestre,
                                estudianteId);

        long[] conteos = extraerConteos(consultas);

        List<Object[]> personas =
                consultaRepository
                        .contarPersonasAtendidasPorSemestreYEstudiante(
                                año,
                                semestre,
                                estudianteId);

        List<Object[]> procesos =
                procesoRepository
                        .contarProcesosPorEstadoPorSemestreYEstudiante(
                                año,
                                semestre,
                                estudianteId);

        return construir(
                periodo,
                conteos,
                personas,
                procesos);
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerPorAsesor(
            int año,
            int semestre,
            Long asesorId) {

        PeriodoEstadistico periodo =
                validar(año, semestre, asesorId, "asesor");

        List<Object[]> consultas =
                consultaRepository
                        .contarFinalizadasYPendientesPorSemestreYAsesor(
                                año,
                                semestre,
                                asesorId);

        long[] conteos = extraerConteos(consultas);

        List<Object[]> personas =
                consultaRepository
                        .contarPersonasAtendidasPorSemestreYAsesor(
                                año,
                                semestre,
                                asesorId);

        List<Object[]> procesos =
                procesoRepository
                        .contarProcesosPorEstadoPorSemestreYAsesor(
                                año,
                                semestre,
                                asesorId);

        return construir(
                periodo,
                conteos,
                personas,
                procesos);
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerPorMonitor(
            int año,
            int semestre,
            Long monitorId) {

        PeriodoEstadistico periodo =
                validar(año, semestre, monitorId, "monitor");

        List<Object[]> consultas =
                consultaRepository
                        .contarFinalizadasYPendientesPorSemestreYMonitor(
                                año,
                                semestre,
                                monitorId);

        long[] conteos = extraerConteos(consultas);

        List<Object[]> personas =
                consultaRepository
                        .contarPersonasAtendidasPorSemestreYMonitor(
                                año,
                                semestre,
                                monitorId);

        List<Object[]> procesos =
                procesoRepository
                        .contarProcesosPorEstadoPorSemestreYMonitor(
                                año,
                                semestre,
                                monitorId);

        return construir(
                periodo,
                conteos,
                personas,
                procesos);
    }

    private EstadisticasSemestreDTO construir(
            PeriodoEstadistico periodo,
            long[] conteos,
            List<Object[]> personasResult,
            List<Object[]> procesosResult) {

        long finalizadas = conteos[0];
        long pendientes = conteos[1];

        long totalPersonas =
                personasResult != null
                        && !personasResult.isEmpty()
                        && personasResult.getFirst()[0] != null
                        ? ((Number) personasResult.getFirst()[0])
                        .longValue()
                        : 0L;

        List<ConteoDTO> procesosPorEstado =
                mapearConteo(procesosResult);

        return EstadisticasSemestreDTO.builder()
                .año(periodo.año())
                .semestre(periodo.semestre())
                .periodoInicio(periodo.inicio().toString())
                .periodoFin(periodo.fin().toString())
                .consultasFinalizadas(finalizadas)
                .consultasPendientes(pendientes)
                .totalConsultas(finalizadas + pendientes)
                .consultasPorArea(new ArrayList<>())
                .procesosPorEstado(procesosPorEstado)
                .totalPersonasAtendidas(totalPersonas)
                .build();
    }

    private long[] extraerConteos(List<Object[]> resultado) {
        Object[] fila =
                resultado != null && !resultado.isEmpty()
                        ? resultado.getFirst()
                        : null;

        long finalizadas =
                fila != null && fila[0] != null
                        ? ((Number) fila[0]).longValue()
                        : 0L;

        long pendientes =
                fila != null && fila[1] != null
                        ? ((Number) fila[1]).longValue()
                        : 0L;

        return new long[] { finalizadas, pendientes };
    }

    private List<ConteoDTO> mapearConteo(List<Object[]> filas) {
        List<ConteoDTO> resultado = new ArrayList<>();

        if (filas == null) {
            return resultado;
        }

        for (Object[] fila : filas) {
            boolean tresColumnas = fila.length >= 3;

            String nombre = tresColumnas
                    ? fila[1] != null
                    ? fila[1].toString()
                    : "Sin nombre"
                    : fila[0] != null
                    ? fila[0].toString()
                    : "Sin nombre";

            long cantidad = tresColumnas
                    ? fila[2] != null
                    ? ((Number) fila[2]).longValue()
                    : 0L
                    : fila[1] != null
                    ? ((Number) fila[1]).longValue()
                    : 0L;

            resultado.add(new ConteoDTO(nombre, cantidad));
        }

        return resultado;
    }

    private PeriodoEstadistico validar(
            int año,
            int semestre,
            Long perfilId,
            String tipo) {

        if (perfilId == null) {
            throw new BusinessException(
                    "El id de " + tipo + " es obligatorio");
        }

        return periodoEstadisticoService.obtener(año, semestre);
    }
}