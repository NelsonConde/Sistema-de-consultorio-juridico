package co.edu.ufps.legal_cases.business.controller.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.APROBAR_RESPUESTAS_SEGUIMIENTO;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.RESPONDER_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaDecisionDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaRequestDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoRespuestaService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seguimientos")
public class SeguimientoRespuestaController {

    private final SeguimientoRespuestaService seguimientoRespuestaService;

    public SeguimientoRespuestaController(SeguimientoRespuestaService seguimientoRespuestaService) {
        this.seguimientoRespuestaService = seguimientoRespuestaService;
    }

    // El estudiante responde un seguimiento visible para él.
    @PostMapping("/{seguimientoId}/respuestas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + RESPONDER_SEGUIMIENTOS + "')")
    public SeguimientoRespuestaResponseDTO crear(
            @PathVariable Long seguimientoId,
            @Valid @RequestBody SeguimientoRespuestaRequestDTO dto) {
        return seguimientoRespuestaService.crear(seguimientoId, dto);
    }

    // El estudiante puede editar su respuesta solo mientras esté pendiente.
    @PutMapping("/respuestas/{id}")
    @PreAuthorize("hasAuthority('" + RESPONDER_SEGUIMIENTOS + "')")
    public SeguimientoRespuestaResponseDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SeguimientoRespuestaRequestDTO dto) {
        return seguimientoRespuestaService.actualizar(id, dto);
    }

    // Consulta una respuesta específica respetando alcance.
    @GetMapping("/respuestas/{id}")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public SeguimientoRespuestaResponseDTO obtenerPorId(@PathVariable Long id) {
        return seguimientoRespuestaService.obtenerPorId(id);
    }

    // Lista respuestas asociadas a un seguimiento.
    @GetMapping("/{seguimientoId}/respuestas")
    @PreAuthorize("hasAuthority('" + VER_SEGUIMIENTOS + "')")
    public List<SeguimientoRespuestaResponseDTO> listarPorSeguimiento(@PathVariable Long seguimientoId) {
        return seguimientoRespuestaService.listarPorSeguimiento(seguimientoId);
    }

    // Lista respuestas pendientes para revisión dentro del alcance del usuario.
    @GetMapping("/respuestas/pendientes")
    @PreAuthorize("hasAuthority('" + APROBAR_RESPUESTAS_SEGUIMIENTO + "')")
    public PageResponseDTO<SeguimientoRespuestaResponseDTO> listarPendientes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return seguimientoRespuestaService.listarPendientes(
                search,
                page,
                size,
                sortBy,
                direction,
                fechaDesde,
                fechaHasta);
    }

    // Asesor, monitor o administrador aprueban o rechazan la respuesta.
    @PatchMapping("/respuestas/{id}/decision")
    @PreAuthorize("hasAuthority('" + APROBAR_RESPUESTAS_SEGUIMIENTO + "')")
    public SeguimientoRespuestaResponseDTO decidir(
            @PathVariable Long id,
            @Valid @RequestBody SeguimientoRespuestaDecisionDTO dto) {
        return seguimientoRespuestaService.decidir(id, dto);
    }
}
