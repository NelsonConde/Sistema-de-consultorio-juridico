package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.SedeDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.SedeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    // Activas para formularios y selects del frontend.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<SedeDTO> listar() {
        return sedeService.listar();
    }

    // Todas para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<SedeDTO> listarTodos() {
        return sedeService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public SedeDTO obtenerPorId(@PathVariable Long id) {
        return sedeService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public SedeDTO crear(@Valid @RequestBody SedeDTO dto) {
        return sedeService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public SedeDTO actualizar(@PathVariable Long id, @Valid @RequestBody SedeDTO dto) {
        return sedeService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public SedeDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return sedeService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        sedeService.eliminar(id);
    }
}