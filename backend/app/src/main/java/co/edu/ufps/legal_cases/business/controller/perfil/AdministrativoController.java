package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ADMINISTRADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ADMINISTRADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.service.perfil.AdministrativoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/administrativos")
public class AdministrativoController {

    private final AdministrativoService administrativoService;

    public AdministrativoController(AdministrativoService administrativoService) {
        this.administrativoService = administrativoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_ADMINISTRADORES + "', '" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<AdministrativoDTO> listar() {
        return administrativoService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_PERFILES_AUXILIARES + "', '" + VER_ADMINISTRADORES + "', '" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<AdministrativoDTO> listarActivos() {
        return administrativoService.listarActivos();
    }

    @GetMapping("/directoras")
    @PreAuthorize("hasAnyAuthority('" + VER_ADMINISTRADORES + "', '" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<AdministrativoDTO> listarDirectoras() {
        return administrativoService.listarDirectoras();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_ADMINISTRADORES + "', '" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AdministrativoDTO obtenerPorId(@PathVariable Long id) {
        return administrativoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AdministrativoDTO crear(@Valid @RequestBody AdministrativoDTO dto) {
        return administrativoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AdministrativoDTO actualizar(@PathVariable Long id, @Valid @RequestBody AdministrativoDTO dto) {
        return administrativoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AdministrativoDTO cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return administrativoService.cambiarEstado(id, activo);
    }

    @PatchMapping("/{id}/directora")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public AdministrativoDTO cambiarDirectora(@PathVariable Long id, @RequestParam Boolean directora) {
        return administrativoService.cambiarDirectora(id, directora);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ADMINISTRADORES + "', '" + GESTIONAR_USUARIOS + "')")
    public void eliminar(@PathVariable Long id) {
        administrativoService.eliminar(id);
    }
}