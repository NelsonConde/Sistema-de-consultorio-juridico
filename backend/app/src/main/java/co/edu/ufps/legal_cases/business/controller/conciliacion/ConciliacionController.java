package co.edu.ufps.legal_cases.business.controller.conciliacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CONCLUIR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.PROGRAMAR_REUNIONES_CONCILIACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.REPROGRAMAR_REUNIONES_CONCILIACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;

import java.util.List;

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
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionRequestDTO;
import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResponseDTO;
import co.edu.ufps.legal_cases.business.service.conciliacion.ConciliacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conciliaciones")
@RequiredArgsConstructor
public class ConciliacionController {

    private final ConciliacionService conciliacionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + VER_CONCILIACIONES + "')")
    public List<ConciliacionResponseDTO> listar() {
        return conciliacionService.listar();
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
            @RequestParam Long estudianteId) {
        return conciliacionService.asignarEstudiante(id, estudianteId);
    }

    @PatchMapping("/{id}/conciliador")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO asignarConciliador(
            @PathVariable Long id,
            @RequestParam Long conciliadorId) {
        return conciliacionService.asignarConciliador(id, conciliadorId);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIACIONES + "', '" + CONCLUIR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return conciliacionService.cambiarEstado(id, estado);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIACIONES + "', '" + CONCLUIR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO finalizar(
            @PathVariable Long id,
            @RequestParam String estado,
            @RequestParam("acta") MultipartFile acta) {
        return conciliacionService.finalizar(id, estado, acta);
    }

    @PostMapping("/{id}/solicitud")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public ConciliacionResponseDTO reemplazarSolicitud(
            @PathVariable Long id,
            @RequestParam("solicitud") MultipartFile solicitud) {
        return conciliacionService.reemplazarSolicitud(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CONCILIACIONES + "')")
    public void desactivar(@PathVariable Long id) {
        conciliacionService.desactivar(id);
    }
}
