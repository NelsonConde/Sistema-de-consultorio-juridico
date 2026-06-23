package co.edu.ufps.legal_cases.business.service.estadisticas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.SemestreDTO;
import co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas.EstadisticasPerfilQueryService;
import co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas.EstadisticasQueryService;
import co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas.EstadisticasRangoQueryService;
import co.edu.ufps.legal_cases.business.service.estadisticas.pdf.EstadisticasPdfService;

// Fachada del módulo de estadísticas. El controller entra por aquí.
@Service
public class EstadisticasService {

    private final EstadisticasQueryService estadisticasQueryService;
    private final EstadisticasRangoQueryService estadisticasRangoQueryService;
    private final EstadisticasPerfilQueryService estadisticasPerfilQueryService;
    private final EstadisticasPdfService estadisticasPdfService;

    public EstadisticasService(
            EstadisticasQueryService estadisticasQueryService,
            EstadisticasRangoQueryService estadisticasRangoQueryService,
            EstadisticasPerfilQueryService estadisticasPerfilQueryService,
            EstadisticasPdfService estadisticasPdfService) {
        this.estadisticasQueryService = estadisticasQueryService;
        this.estadisticasRangoQueryService = estadisticasRangoQueryService;
        this.estadisticasPerfilQueryService = estadisticasPerfilQueryService;
        this.estadisticasPdfService = estadisticasPdfService;
    }

    public List<SemestreDTO> listarSemestresDisponibles() {
        return estadisticasQueryService.listarSemestresDisponibles();
    }

    public EstadisticasSemestreDTO obtenerEstadisticasSemestre(int año, int semestre) {
        return estadisticasQueryService.obtenerEstadisticasSemestre(año, semestre);
    }

    public EstadisticasSemestreDTO obtenerPorRango(LocalDate fechaInicio, LocalDate fechaFin) {
        return estadisticasRangoQueryService.obtenerPorRango(fechaInicio, fechaFin);
    }

    public byte[] generarPdfSemestre(int año, int semestre) {
        return estadisticasPdfService.generarReporteSemestral(
                estadisticasQueryService.obtenerEstadisticasSemestre(año, semestre));
    }

    public byte[] generarPdfRango(LocalDate fechaInicio, LocalDate fechaFin) {
        return estadisticasPdfService.generarReporteSemestral(
                estadisticasRangoQueryService.obtenerPorRango(fechaInicio, fechaFin));
    }

    public EstadisticasSemestreDTO obtenerPorEstudiante(int año, int semestre, Long id) {
        return estadisticasPerfilQueryService.obtenerPorEstudiante(año, semestre, id);
    }

    public EstadisticasSemestreDTO obtenerPorAsesor(int año, int semestre, Long id) {
        return estadisticasPerfilQueryService.obtenerPorAsesor(año, semestre, id);
    }

    public EstadisticasSemestreDTO obtenerPorMonitor(int año, int semestre, Long id) {
        return estadisticasPerfilQueryService.obtenerPorMonitor(año, semestre, id);
    }
}