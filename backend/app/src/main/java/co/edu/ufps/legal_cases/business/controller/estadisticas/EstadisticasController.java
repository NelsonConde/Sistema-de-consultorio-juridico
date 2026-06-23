package co.edu.ufps.legal_cases.business.controller.estadisticas;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_REPORTES;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.estadisticas.EstadisticasSemestreDTO;
import co.edu.ufps.legal_cases.business.dto.estadisticas.SemestreDTO;
import co.edu.ufps.legal_cases.business.service.estadisticas.EstadisticasService;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    // Semestres disponibles para el selector del frontend.
    @GetMapping("/semestres")
    @PreAuthorize("hasAuthority('" + VER_REPORTES + "') or hasAuthority('" + VER_CONSULTAS + "')")
    public List<SemestreDTO> listarSemestresDisponibles() {
        return estadisticasService.listarSemestresDisponibles();
    }

    // Estadísticas globales por semestre predefinido — admin.
    @GetMapping("/{año}/semestre/{semestre}")
    @PreAuthorize("hasAuthority('" + VER_REPORTES + "')")
    public EstadisticasSemestreDTO obtenerPorSemestre(
            @PathVariable int año,
            @PathVariable int semestre) {
        return estadisticasService.obtenerEstadisticasSemestre(año, semestre);
    }

    // PDF por semestre predefinido — admin.
    @GetMapping("/{año}/semestre/{semestre}/pdf")
    @PreAuthorize("hasAuthority('" + VER_REPORTES + "')")
    public ResponseEntity<byte[]> descargarPdfSemestre(
            @PathVariable int año,
            @PathVariable int semestre) {
        byte[] pdf = estadisticasService.generarPdfSemestre(año, semestre);
        return pdfResponse(pdf, "estadisticas-" + año + "-s" + semestre + ".pdf");
    }

    // Estadísticas globales por rango libre de fechas — admin.
    @GetMapping("/reporte")
    @PreAuthorize("hasAuthority('" + VER_REPORTES + "')")
    public EstadisticasSemestreDTO obtenerPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return estadisticasService.obtenerPorRango(fechaInicio, fechaFin);
    }

    // PDF por rango libre de fechas — admin.
    @GetMapping("/reporte/pdf")
    @PreAuthorize("hasAuthority('" + VER_REPORTES + "')")
    public ResponseEntity<byte[]> descargarPdfRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        byte[] pdf = estadisticasService.generarPdfRango(fechaInicio, fechaFin);
        return pdfResponse(pdf, "estadisticas-" + fechaInicio + "-" + fechaFin + ".pdf");
    }

    // Estadísticas por perfil — inicio/dashboard.
    @GetMapping("/{año}/semestre/{semestre}/estudiante/{id}")
    @PreAuthorize("hasAuthority('" + VER_CONSULTAS + "')")
    public EstadisticasSemestreDTO obtenerPorEstudiante(
            @PathVariable int año, @PathVariable int semestre, @PathVariable Long id) {
        return estadisticasService.obtenerPorEstudiante(año, semestre, id);
    }

    @GetMapping("/{año}/semestre/{semestre}/asesor/{id}")
    @PreAuthorize("hasAuthority('" + VER_CONSULTAS + "')")
    public EstadisticasSemestreDTO obtenerPorAsesor(
            @PathVariable int año, @PathVariable int semestre, @PathVariable Long id) {
        return estadisticasService.obtenerPorAsesor(año, semestre, id);
    }

    @GetMapping("/{año}/semestre/{semestre}/monitor/{id}")
    @PreAuthorize("hasAuthority('" + VER_CONSULTAS + "')")
    public EstadisticasSemestreDTO obtenerPorMonitor(
            @PathVariable int año, @PathVariable int semestre, @PathVariable Long id) {
        return estadisticasService.obtenerPorMonitor(año, semestre, id);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String nombreArchivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}