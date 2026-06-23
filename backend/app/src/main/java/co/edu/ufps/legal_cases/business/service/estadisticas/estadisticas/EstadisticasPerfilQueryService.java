package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.dto.estadisticas.ConteoDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

// QueryService para estadísticas filtradas por perfil.
// Estudiante ve sus propias consultas.
// Asesor y monitor ven las consultas donde están asignados.
@Service
public class EstadisticasPerfilQueryService {

    private final ConsultaRepository consultaRepository;
    private final ProcesoRepository procesoRepository;

    private static final int AÑO_MINIMO = 2024;

    public EstadisticasPerfilQueryService(
            ConsultaRepository consultaRepository,
            ProcesoRepository procesoRepository) {
        this.consultaRepository = consultaRepository;
        this.procesoRepository = procesoRepository;
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerPorEstudiante(int año, int semestre, Long estudianteId) {
        validar(año, semestre, estudianteId, "estudiante");

        List<Object[]> consultas = consultaRepository
                .contarFinalizadasYPendientesPorSemestreYEstudiante(año, semestre, estudianteId);
        long[] conteos = extraerConteos(consultas);

        List<Object[]> personas = consultaRepository
                .contarPersonasAtendidasPorSemestreYEstudiante(año, semestre, estudianteId);

        List<Object[]> procesos = procesoRepository
                .contarProcesosPorEstadoYEstudiante(estudianteId);

        return construir(año, semestre, conteos, personas, procesos);
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerPorAsesor(int año, int semestre, Long asesorId) {
        validar(año, semestre, asesorId, "asesor");

        List<Object[]> consultas = consultaRepository
                .contarFinalizadasYPendientesPorSemestreYAsesor(año, semestre, asesorId);
        long[] conteos = extraerConteos(consultas);

        List<Object[]> personas = consultaRepository
                .contarPersonasAtendidasPorSemestreYAsesor(año, semestre, asesorId);

        List<Object[]> procesos = procesoRepository
                .contarProcesosPorEstadoYAsesor(asesorId);

        return construir(año, semestre, conteos, personas, procesos);
    }

    @Transactional(readOnly = true)
    public EstadisticasSemestreDTO obtenerPorMonitor(int año, int semestre, Long monitorId) {
        validar(año, semestre, monitorId, "monitor");

        List<Object[]> consultas = consultaRepository
                .contarFinalizadasYPendientesPorSemestreYMonitor(año, semestre, monitorId);
        long[] conteos = extraerConteos(consultas);

        List<Object[]> personas = consultaRepository
                .contarPersonasAtendidasPorSemestreYMonitor(año, semestre, monitorId);

        List<Object[]> procesos = procesoRepository
                .contarProcesosPorEstadoYMonitor(monitorId);

        return construir(año, semestre, conteos, personas, procesos);
    }

    private EstadisticasSemestreDTO construir(int año, int semestre, long[] conteos,
                                              List<Object[]> personasResult, List<Object[]> procesosResult) {

        long finalizadas = conteos[0];
        long pendientes = conteos[1];

        long totalPersonas = (personasResult != null && !personasResult.isEmpty()
                && personasResult.get(0)[0] != null)
                ? ((Number) personasResult.get(0)[0]).longValue() : 0L;

        List<ConteoDTO> procesosPorEstado = mapearConteo(procesosResult);

        LocalDate inicio = calcularInicio(año, semestre);
        LocalDate fin = calcularFin(año, semestre);

        return EstadisticasSemestreDTO.builder()
                .año(año)
                .semestre(semestre)
                .periodoInicio(inicio.toString())
                .periodoFin(fin.toString())
                .consultasFinalizadas(finalizadas)
                .consultasPendientes(pendientes)
                .totalConsultas(finalizadas + pendientes)
                .consultasPorArea(new ArrayList<>())
                .procesosPorEstado(procesosPorEstado)
                .totalPersonasAtendidas(totalPersonas)
                .build();
    }

    private long[] extraerConteos(List<Object[]> resultado) {
        Object[] fila = (resultado != null && !resultado.isEmpty()) ? resultado.get(0) : null;
        long finalizadas = fila != null && fila[0] != null ? ((Number) fila[0]).longValue() : 0L;
        long pendientes = fila != null && fila[1] != null ? ((Number) fila[1]).longValue() : 0L;
        return new long[]{finalizadas, pendientes};
    }

    private List<ConteoDTO> mapearConteo(List<Object[]> filas) {
        List<ConteoDTO> resultado = new ArrayList<>();
        if (filas == null) return resultado;
        for (Object[] fila : filas) {
            boolean tresColumnas = fila.length >= 3;
            String nombre = tresColumnas
                    ? (fila[1] != null ? fila[1].toString() : "Sin nombre")
                    : (fila[0] != null ? fila[0].toString() : "Sin nombre");
            long cantidad = tresColumnas
                    ? (fila[2] != null ? ((Number) fila[2]).longValue() : 0L)
                    : (fila[1] != null ? ((Number) fila[1]).longValue() : 0L);
            resultado.add(new ConteoDTO(nombre, cantidad));
        }
        return resultado;
    }

    private void validar(int año, int semestre, Long perfilId, String tipo) {
        if (semestre < 1 || semestre > 2) {
            throw new BusinessException("El semestre debe ser 1 o 2");
        }
        if (año < AÑO_MINIMO) {
            throw new BusinessException("No hay datos disponibles antes del año " + AÑO_MINIMO);
        }
        if (perfilId == null) {
            throw new BusinessException("El id de " + tipo + " es obligatorio");
        }
        if (calcularInicio(año, semestre).isAfter(LocalDate.now())) {
            throw new BusinessException("No se pueden consultar estadísticas de un semestre que aún no ha comenzado");
        }
    }

    private LocalDate calcularInicio(int año, int semestre) {
        return semestre == 1 ? LocalDate.of(año, 1, 1) : LocalDate.of(año, 7, 1);
    }

    private LocalDate calcularFin(int año, int semestre) {
        return semestre == 1 ? LocalDate.of(año, 6, 30) : LocalDate.of(año, 12, 31);
    }
}