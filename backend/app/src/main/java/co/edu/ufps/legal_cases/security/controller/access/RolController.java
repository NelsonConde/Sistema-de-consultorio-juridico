package co.edu.ufps.legal_cases.security.controller.access;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_PERMISOS_A_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ROLES;

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

import co.edu.ufps.legal_cases.security.dto.access.RolDTO;
import co.edu.ufps.legal_cases.security.service.access.RolService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + VER_ROLES + "')")
    public List<RolDTO> listar() {
        return rolService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + VER_ROLES + "')")
    public List<RolDTO> listarActivos() {
        return rolService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + VER_ROLES + "')")
    public RolDTO obtenerPorId(@PathVariable Long id) {
        return rolService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + CREAR_ROLES + "')")
    public RolDTO crear(@Valid @RequestBody RolDTO dto) {
        return rolService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + EDITAR_ROLES + "')")
    public RolDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody RolDTO dto) {
        return rolService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + EDITAR_ROLES + "')")
    public RolDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return rolService.cambiarEstado(id, activo);
    }

    @PatchMapping("/{rolId}/permisos/{permisoId}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + ASIGNAR_PERMISOS_A_ROLES + "')")
    public RolDTO asignarPermiso(
            @PathVariable Long rolId,
            @PathVariable Long permisoId) {
        return rolService.asignarPermiso(rolId, permisoId);
    }

    @DeleteMapping("/{rolId}/permisos/{permisoId}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ROLES + "', '" + ASIGNAR_PERMISOS_A_ROLES + "')")
    public RolDTO quitarPermiso(
            @PathVariable Long rolId,
            @PathVariable Long permisoId) {
        return rolService.quitarPermiso(rolId, permisoId);
    }
}