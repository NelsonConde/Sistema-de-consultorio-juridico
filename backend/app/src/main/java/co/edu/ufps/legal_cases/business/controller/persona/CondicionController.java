package co.edu.ufps.legal_cases.business.controller.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.persona.CondicionDTO;
import co.edu.ufps.legal_cases.business.service.persona.CondicionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/condiciones")
public class CondicionController {

    private final CondicionService condicionService;

    public CondicionController(CondicionService condicionService) {
        this.condicionService = condicionService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<CondicionDTO> listar() {
        return condicionService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<CondicionDTO> listarTodos() {
        return condicionService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public CondicionDTO obtenerPorId(@PathVariable Long id) {
        return condicionService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public CondicionDTO crear(@Valid @RequestBody CondicionDTO condicionDTO) {
        return condicionService.crear(condicionDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public CondicionDTO actualizar(@PathVariable Long id, @Valid @RequestBody CondicionDTO condicionDTO) {
        return condicionService.actualizar(id, condicionDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public CondicionDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return condicionService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        condicionService.eliminar(id);
    }
}