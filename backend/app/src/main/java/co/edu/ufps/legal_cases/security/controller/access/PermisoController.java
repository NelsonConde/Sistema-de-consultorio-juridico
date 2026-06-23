package co.edu.ufps.legal_cases.security.controller.access;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_PERMISOS_A_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERMISOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import co.edu.ufps.legal_cases.security.dto.access.PermisoDTO;
import co.edu.ufps.legal_cases.security.service.access.PermisoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/permisos")
public class PermisoController {

    private final PermisoService permisoService;

    public PermisoController(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_PERMISOS + "', '" + ASIGNAR_PERMISOS_A_ROLES + "')")
    public List<PermisoDTO> listar() {
        return permisoService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_PERMISOS + "', '" + ASIGNAR_PERMISOS_A_ROLES + "')")
    public List<PermisoDTO> listarActivos() {
        return permisoService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_PERMISOS + "', '" + ASIGNAR_PERMISOS_A_ROLES + "')")
    public PermisoDTO obtenerPorId(@PathVariable Long id) {
        return permisoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_PERMISOS + "')")
    public PermisoDTO crear(@Valid @RequestBody PermisoDTO dto) {
        return permisoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_PERMISOS + "')")
    public PermisoDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PermisoDTO dto) {
        return permisoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_PERMISOS + "')")
    public PermisoDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return permisoService.cambiarEstado(id, activo);
    }
}