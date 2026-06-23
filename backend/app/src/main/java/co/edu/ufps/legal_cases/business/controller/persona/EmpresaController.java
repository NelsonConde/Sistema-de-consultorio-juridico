package co.edu.ufps.legal_cases.business.controller.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.persona.EmpresaDTO;
import co.edu.ufps.legal_cases.business.service.persona.EmpresaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<EmpresaDTO> listar() {
        return empresaService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<EmpresaDTO> listarTodos() {
        return empresaService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public EmpresaDTO obtenerPorId(@PathVariable Long id) {
        return empresaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public EmpresaDTO crear(@Valid @RequestBody EmpresaDTO empresaDTO) {
        return empresaService.crear(empresaDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public EmpresaDTO actualizar(@PathVariable Long id, @Valid @RequestBody EmpresaDTO empresaDTO) {
        return empresaService.actualizar(id, empresaDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public EmpresaDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return empresaService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
    }
}