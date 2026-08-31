package co.edu.ufps.legal_cases.business.controller.proceso;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PROCESOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PROCESOS;

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

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoDTO;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.service.proceso.ProcesoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/procesos")
public class ProcesoController {

    private final ProcesoService procesoService;

    public ProcesoController(ProcesoService procesoService) {
        this.procesoService = procesoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_PROCESOS + "', '" + GESTIONAR_PROCESOS + "')")
    public List<ProcesoDTO> listar() {
        return procesoService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_PROCESOS + "', '" + GESTIONAR_PROCESOS + "')")
    public ProcesoDTO obtenerPorId(@PathVariable Long id) {
        return procesoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_PROCESOS + "')")
    public ProcesoDTO crear(@Valid @RequestBody ProcesoDTO dto) {
        return procesoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_PROCESOS + "')")
    public ProcesoDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProcesoDTO dto) {
        return procesoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + GESTIONAR_PROCESOS + "')")
    public ProcesoDTO cambiarEstadoProceso(
            @PathVariable Long id,
            @RequestParam EstadoProceso estado) {
        return procesoService.cambiarEstadoProceso(id, estado);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_PROCESOS + "')")
    public ProcesoDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return procesoService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_PROCESOS + "')")
    public void eliminar(@PathVariable Long id) {
        procesoService.eliminar(id);
    }
}