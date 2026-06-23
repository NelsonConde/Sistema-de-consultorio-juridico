package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.BarrioDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.BarrioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/barrios")
public class BarrioController {

    private final BarrioService barrioService;

    public BarrioController(BarrioService barrioService) {
        this.barrioService = barrioService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<BarrioDTO> listar() {
        return barrioService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<BarrioDTO> listarTodos() {
        return barrioService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public BarrioDTO obtenerPorId(@PathVariable Long id) {
        return barrioService.obtenerPorId(id);
    }

    // Barrios activos de un municipio activo.
    @GetMapping("/municipio/{municipioId}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<BarrioDTO> listarPorMunicipio(@PathVariable Long municipioId) {
        return barrioService.listarPorMunicipio(municipioId);
    }

    // Todos los barrios de un municipio para administración.
    @GetMapping("/municipio/{municipioId}/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<BarrioDTO> listarTodosPorMunicipio(@PathVariable Long municipioId) {
        return barrioService.listarTodosPorMunicipio(municipioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public BarrioDTO crear(@Valid @RequestBody BarrioDTO barrioDTO) {
        return barrioService.crear(barrioDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public BarrioDTO actualizar(@PathVariable Long id, @Valid @RequestBody BarrioDTO barrioDTO) {
        return barrioService.actualizar(id, barrioDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public BarrioDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return barrioService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        barrioService.eliminar(id);
    }
}