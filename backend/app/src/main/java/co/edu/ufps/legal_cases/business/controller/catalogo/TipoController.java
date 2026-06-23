package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.TipoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tipos")
public class TipoController {

    private final TipoService tipoService;

    public TipoController(TipoService tipoService) {
        this.tipoService = tipoService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TipoDTO> listar() {
        return tipoService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<TipoDTO> listarTodos() {
        return tipoService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public TipoDTO obtenerPorId(@PathVariable Long id) {
        return tipoService.obtenerPorId(id);
    }

    // Tipos activos de un tema activo.
    @GetMapping("/tema/{temaId}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TipoDTO> listarPorTema(@PathVariable Long temaId) {
        return tipoService.listarPorTema(temaId);
    }

    // Todos los tipos de un tema para administración.
    @GetMapping("/tema/{temaId}/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<TipoDTO> listarTodosPorTema(@PathVariable Long temaId) {
        return tipoService.listarTodosPorTema(temaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoDTO crear(@Valid @RequestBody TipoDTO tipoDTO) {
        return tipoService.crear(tipoDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoDTO actualizar(@PathVariable Long id, @Valid @RequestBody TipoDTO tipoDTO) {
        return tipoService.actualizar(id, tipoDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return tipoService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        tipoService.eliminar(id);
    }
}