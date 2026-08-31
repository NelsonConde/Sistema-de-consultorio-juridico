package co.edu.ufps.legal_cases.business.controller.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ELIMINAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ALERTAS_DISCIPLINARIAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seguimientos")
@RequiredArgsConstructor
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    @GetMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public List<SeguimientoResponseDTO> listarPorConsulta(@PathVariable Long consultaId) {
        return seguimientoService.listarPorConsulta(consultaId);
    }

    @GetMapping("/consulta/{consultaId}/visibles-estudiante")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public List<SeguimientoResponseDTO> listarVisiblesParaEstudiantePorConsulta(@PathVariable Long consultaId) {
        return seguimientoService.listarVisiblesParaEstudiantePorConsulta(consultaId);
    }

    @GetMapping("/autor/{autorId}")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public List<SeguimientoResponseDTO> listarPorAutor(@PathVariable Long autorId) {
        return seguimientoService.listarPorAutor(autorId);
    }

    /**
     * Devuelve los seguimientos visibles para el usuario autenticado según su rol.
     * Se usa para el calendario de actividades en el frontend.
     * - Administrador: ve todos.
     * - Asesor/Monitor: ve los de consultas dentro de su alcance.
     * - Estudiante: ve solo los marcados como notificarEstudiante = true.
     * - Conciliador: por ahora no ve ninguno.
     */
    @GetMapping("/calendario")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public List<SeguimientoResponseDTO> listarParaCalendario() {
        return seguimientoService.listarParaCalendario();
    }

    @GetMapping("/alertas-disciplinarias")
    @PreAuthorize("hasAuthority('" + VER_ALERTAS_DISCIPLINARIAS + "')")
    public List<SeguimientoResponseDTO> listarAlertasDisciplinarias() {
        return seguimientoService.listarAlertasDisciplinarias();
    }

    @GetMapping("/fecha-entrega")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public List<SeguimientoResponseDTO> listarPorFechaEntrega(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrega) {
        return seguimientoService.listarPorFechaEntrega(fechaEntrega);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public SeguimientoResponseDTO obtenerPorId(@PathVariable Long id) {
        return seguimientoService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + CREAR_SEGUIMIENTOS + "')")
    public SeguimientoResponseDTO crear(@Valid @RequestBody SeguimientoRequestDTO dto) {
        return seguimientoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + EDITAR_SEGUIMIENTOS + "')")
    public SeguimientoResponseDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SeguimientoRequestDTO dto) {
        return seguimientoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + EDITAR_SEGUIMIENTOS + "')")
    public SeguimientoResponseDTO cambiarEstadoSeguimiento(
            @PathVariable Long id,
            @RequestParam EstadoSeguimiento estado) {
        return seguimientoService.cambiarEstadoSeguimiento(id, estado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + ELIMINAR_SEGUIMIENTOS + "')")
    public void eliminar(@PathVariable Long id) {
        seguimientoService.eliminar(id);
    }
}