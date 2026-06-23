package co.edu.ufps.legal_cases.business.controller.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.persona.TipoPersonaDTO;
import co.edu.ufps.legal_cases.business.service.persona.TipoPersonaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tipos-persona")
public class TipoPersonaController {

    private final TipoPersonaService tipoPersonaService;

    public TipoPersonaController(TipoPersonaService tipoPersonaService) {
        this.tipoPersonaService = tipoPersonaService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TipoPersonaDTO> listar() {
        return tipoPersonaService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<TipoPersonaDTO> listarTodos() {
        return tipoPersonaService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public TipoPersonaDTO obtenerPorId(@PathVariable Long id) {
        return tipoPersonaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoPersonaDTO crear(@Valid @RequestBody TipoPersonaDTO tipoPersonaDTO) {
        return tipoPersonaService.crear(tipoPersonaDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoPersonaDTO actualizar(@PathVariable Long id, @Valid @RequestBody TipoPersonaDTO tipoPersonaDTO) {
        return tipoPersonaService.actualizar(id, tipoPersonaDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoPersonaDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return tipoPersonaService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        tipoPersonaService.eliminar(id);
    }
}