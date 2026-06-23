package co.edu.ufps.legal_cases.business.controller.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.persona.OcupacionDTO;
import co.edu.ufps.legal_cases.business.service.persona.OcupacionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ocupaciones")
public class OcupacionController {

    private final OcupacionService ocupacionService;

    public OcupacionController(OcupacionService ocupacionService) {
        this.ocupacionService = ocupacionService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<OcupacionDTO> listar() {
        return ocupacionService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<OcupacionDTO> listarTodos() {
        return ocupacionService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public OcupacionDTO obtenerPorId(@PathVariable Long id) {
        return ocupacionService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public OcupacionDTO crear(@Valid @RequestBody OcupacionDTO ocupacionDTO) {
        return ocupacionService.crear(ocupacionDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public OcupacionDTO actualizar(@PathVariable Long id, @Valid @RequestBody OcupacionDTO ocupacionDTO) {
        return ocupacionService.actualizar(id, ocupacionDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public OcupacionDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return ocupacionService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        ocupacionService.eliminar(id);
    }
}