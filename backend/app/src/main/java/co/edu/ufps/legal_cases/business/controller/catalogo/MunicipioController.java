package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.MunicipioDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.MunicipioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/municipios")
public class MunicipioController {

    private final MunicipioService municipioService;

    public MunicipioController(MunicipioService municipioService) {
        this.municipioService = municipioService;
    }

    // Activos para formularios, selects y uso normal.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<MunicipioDTO> listar() {
        return municipioService.listar();
    }

    // Todos para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<MunicipioDTO> listarTodos() {
        return municipioService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public MunicipioDTO obtenerPorId(@PathVariable Long id) {
        return municipioService.obtenerPorId(id);
    }

    // Municipios activos de un departamento activo.
    @GetMapping("/departamento/{departamentoId}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<MunicipioDTO> listarPorDepartamento(@PathVariable Long departamentoId) {
        return municipioService.listarPorDepartamento(departamentoId);
    }

    // Todos los municipios de un departamento para administración.
    @GetMapping("/departamento/{departamentoId}/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<MunicipioDTO> listarTodosPorDepartamento(@PathVariable Long departamentoId) {
        return municipioService.listarTodosPorDepartamento(departamentoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public MunicipioDTO crear(@Valid @RequestBody MunicipioDTO municipioDTO) {
        return municipioService.crear(municipioDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public MunicipioDTO actualizar(@PathVariable Long id, @Valid @RequestBody MunicipioDTO municipioDTO) {
        return municipioService.actualizar(id, municipioDTO);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public MunicipioDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return municipioService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        municipioService.eliminar(id);
    }
}