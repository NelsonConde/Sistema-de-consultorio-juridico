package co.edu.ufps.legal_cases.business.controller.seguimiento;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_SEGUIMIENTOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATEGORIAS_SEGUIMIENTO;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;

import java.util.List;

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

import co.edu.ufps.legal_cases.business.dto.seguimiento.CategoriaSeguimientoDTO;
import co.edu.ufps.legal_cases.business.service.seguimiento.CategoriaSeguimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seguimientos/categorias")
@RequiredArgsConstructor
public class CategoriaSeguimientoController {

    private final CategoriaSeguimientoService categoriaSeguimientoService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public List<CategoriaSeguimientoDTO> listar() {
        return categoriaSeguimientoService.listar();
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyAuthority('" + VER_SEGUIMIENTOS + "', '" + CREAR_SEGUIMIENTOS + "', '"
            + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public List<CategoriaSeguimientoDTO> listarActivas() {
        return categoriaSeguimientoService.listarActivas();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public CategoriaSeguimientoDTO obtenerPorId(@PathVariable Long id) {
        return categoriaSeguimientoService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public CategoriaSeguimientoDTO crear(@Valid @RequestBody CategoriaSeguimientoDTO dto) {
        return categoriaSeguimientoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public CategoriaSeguimientoDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaSeguimientoDTO dto) {
        return categoriaSeguimientoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public CategoriaSeguimientoDTO cambiarActivo(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return categoriaSeguimientoService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATEGORIAS_SEGUIMIENTO + "')")
    public void eliminar(@PathVariable Long id) {
        categoriaSeguimientoService.eliminar(id);
    }
}