package co.edu.ufps.legal_cases.business.controller.conciliacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CONCLUIR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.PROGRAMAR_REUNIONES_CONCILIACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.REPROGRAMAR_REUNIONES_CONCILIACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionDetalleResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.service.conciliacion.ConciliacionService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conciliaciones")
@RequiredArgsConstructor
public class ConciliacionController {

    private final ConciliacionService conciliacionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + VER_CONCILIACIONES + "')")
    public PageResponseDTO<ConciliacionResumenDTO> buscar(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return conciliacionService.buscarParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                estado,
                fechaDesde,
                fechaHasta);
    }

    @GetMapping("/reuniones")
    @PreAuthorize("hasAuthority('" + VER_CONCILIACIONES + "')")
    public PageResponseDTO<ReunionConciliacionResumenDTO> buscarReuniones(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return conciliacionService.buscarReunionesParaUsuarioActual(
                search,
                page,
                size,
                sortBy,
                direction,
                estado,
                fechaDesde,
                fechaHasta);
    }

    @GetMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAuthority('" + VER_CONCILIACIONES + "')")
    public List<ConciliacionResponseDTO> listarPorConsulta(@PathVariable Long consultaId) {
        return conciliacionService.listarPorConsulta(consultaId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + VER_CONCILIACIONES + "')")
    public ConciliacionDetalleResponseDTO obtenerDetalle(@PathVariable Long id) {
        return conciliacionService.obtenerDetalle(id);
    }

    @PostMapping("/consulta/{consultaId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO crearDesdeConsulta(
            @PathVariable Long consultaId,
            @RequestParam("solicitud") MultipartFile solicitud) {
        return conciliacionService.crearDesdeConsulta(consultaId, solicitud);
    }

    @PostMapping("/{id}/reunion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + PROGRAMAR_REUNIONES_CONCILIACION + "')")
    public ReunionConciliacionResponseDTO programarReunion(
            @PathVariable Long id,
            @Valid @RequestBody ReunionConciliacionRequestDTO dto) {
        return conciliacionService.programarReunion(id, dto);
    }

    @PutMapping("/{id}/reunion")
    @PreAuthorize("hasAuthority('" + REPROGRAMAR_REUNIONES_CONCILIACION + "')")
    public ReunionConciliacionResponseDTO reprogramarReunion(
            @PathVariable Long id,
            @Valid @RequestBody ReunionConciliacionRequestDTO dto) {
        return conciliacionService.reprogramarReunion(id, dto);
    }

    @PatchMapping("/{id}/estudiante")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIACIONES + "', '" + CONCLUIR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO asignarEstudiante(
            @PathVariable Long id,
            @RequestParam Long estudianteId,
            @RequestParam Long version) {
        return conciliacionService.asignarEstudiante(id, estudianteId, version);
    }

    @PatchMapping("/{id}/conciliador")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO asignarConciliador(
            @PathVariable Long id,
            @RequestParam Long conciliadorId,
            @RequestParam Long version) {
        return conciliacionService.asignarConciliador(id, conciliadorId, version);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIACIONES + "', '" + CONCLUIR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado,
            @RequestParam Long version) {
        return conciliacionService.cambiarEstado(id, estado, version);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIACIONES + "', '" + CONCLUIR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO finalizar(
            @PathVariable Long id,
            @RequestParam String estado,
            @RequestParam("acta") MultipartFile acta,
            @RequestParam Long version) {
        return conciliacionService.finalizar(id, estado, acta, version);
    }

    @PostMapping("/{id}/solicitud")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO reemplazarSolicitud(
            @PathVariable Long id,
            @RequestParam("solicitud") MultipartFile solicitud,
            @RequestParam Long version) {
        return conciliacionService.reemplazarSolicitud(id, solicitud, version);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public void desactivar(
            @PathVariable Long id,
            @RequestParam Long version) {
        conciliacionService.desactivar(id, version);
    }
}
