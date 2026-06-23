package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.service.perfil.AsesorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/asesores")
public class AsesorController {

    private final AsesorService asesorService;

    public AsesorController(AsesorService asesorService) {
        this.asesorService = asesorService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_ASESORES_MONITORES + "', '" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<AsesorDTO> listar() {
        return asesorService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_PERFILES_AUXILIARES + "', '" + VER_ASESORES_MONITORES + "', '" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<AsesorDTO> listarActivos() {
        return asesorService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_ASESORES_MONITORES + "', '" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AsesorDTO obtenerPorId(@PathVariable Long id) {
        return asesorService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AsesorDTO crear(@Valid @RequestBody AsesorDTO dto) {
        return asesorService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AsesorDTO actualizar(@PathVariable Long id, @Valid @RequestBody AsesorDTO dto) {
        return asesorService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AsesorDTO cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return asesorService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public void eliminar(@PathVariable Long id) {
        asesorService.eliminar(id);
    }
}