package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.NacionalidadDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.NacionalidadService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/nacionalidades")
public class NacionalidadController {

    private final NacionalidadService nacionalidadService;

    public NacionalidadController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    // Activas para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<NacionalidadDTO> listar() {
        return nacionalidadService.listar();
    }

    // Todas para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<NacionalidadDTO> listarTodos() {
        return nacionalidadService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public NacionalidadDTO obtenerPorId(@PathVariable Long id) {
        return nacionalidadService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public NacionalidadDTO crear(@Valid @RequestBody NacionalidadDTO nacionalidadDTO) {
        return nacionalidadService.crear(nacionalidadDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public NacionalidadDTO actualizar(@PathVariable Long id, @Valid @RequestBody NacionalidadDTO nacionalidadDTO) {
        return nacionalidadService.actualizar(id, nacionalidadDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public NacionalidadDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return nacionalidadService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        nacionalidadService.eliminar(id);
    }
}