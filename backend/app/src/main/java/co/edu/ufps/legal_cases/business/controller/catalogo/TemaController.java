package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.TemaDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.TemaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/temas")
public class TemaController {

    private final TemaService temaService;

    public TemaController(TemaService temaService) {
        this.temaService = temaService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TemaDTO> listar() {
        return temaService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<TemaDTO> listarTodos() {
        return temaService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public TemaDTO obtenerPorId(@PathVariable Long id) {
        return temaService.obtenerPorId(id);
    }

    // Temas activos de un área activa.
    @GetMapping("/area/{areaId}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TemaDTO> listarPorArea(@PathVariable Long areaId) {
        return temaService.listarPorArea(areaId);
    }

    // Todos los temas de un área para administración.
    @GetMapping("/area/{areaId}/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<TemaDTO> listarTodosPorArea(@PathVariable Long areaId) {
        return temaService.listarTodosPorArea(areaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TemaDTO crear(@Valid @RequestBody TemaDTO temaDTO) {
        return temaService.crear(temaDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TemaDTO actualizar(@PathVariable Long id, @Valid @RequestBody TemaDTO temaDTO) {
        return temaService.actualizar(id, temaDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TemaDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return temaService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        temaService.eliminar(id);
    }
}