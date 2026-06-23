package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONCILIADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.service.perfil.ConciliadorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conciliadores")
public class ConciliadorController {

    private final ConciliadorService conciliadorService;

    public ConciliadorController(ConciliadorService conciliadorService) {
        this.conciliadorService = conciliadorService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CONCILIADORES + "', '" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<ConciliadorDTO> listar() {
        return conciliadorService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_PERFILES_AUXILIARES + "', '" + VER_CONCILIADORES + "', '" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<ConciliadorDTO> listarActivos() {
        return conciliadorService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CONCILIADORES + "', '" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public ConciliadorDTO obtenerPorId(@PathVariable Long id) {
        return conciliadorService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public ConciliadorDTO crear(@Valid @RequestBody ConciliadorDTO dto) {
        return conciliadorService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public ConciliadorDTO actualizar(@PathVariable Long id, @Valid @RequestBody ConciliadorDTO dto) {
        return conciliadorService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public ConciliadorDTO cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return conciliadorService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_CONCILIADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public void eliminar(@PathVariable Long id) {
        conciliadorService.eliminar(id);
    }
}