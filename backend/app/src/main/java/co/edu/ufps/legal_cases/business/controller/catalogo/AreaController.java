package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.AreaDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.AreaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    // Activas para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<AreaDTO> listar() {
        return areaService.listar();
    }

    // Todas para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<AreaDTO> listarTodos() {
        return areaService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public AreaDTO obtenerPorId(@PathVariable Long id) {
        return areaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public AreaDTO crear(@Valid @RequestBody AreaDTO areaDTO) {
        return areaService.crear(areaDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public AreaDTO actualizar(@PathVariable Long id, @Valid @RequestBody AreaDTO areaDTO) {
        return areaService.actualizar(id, areaDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public AreaDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return areaService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        areaService.eliminar(id);
    }
}