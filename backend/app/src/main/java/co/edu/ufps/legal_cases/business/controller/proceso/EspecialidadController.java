package co.edu.ufps.legal_cases.business.controller.proceso;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

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

import co.edu.ufps.legal_cases.business.dto.proceso.EspecialidadDTO;
import co.edu.ufps.legal_cases.business.service.proceso.EspecialidadService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    // Activas para formularios y combos.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<EspecialidadDTO> listar() {
        return especialidadService.listar();
    }

    // Todas para administración del catálogo.
    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public List<EspecialidadDTO> listarTodos() {
        return especialidadService.listarTodos();
    }

    // Activas filtradas por órgano de control.
    @GetMapping("/organo-control/{organoControlId}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<EspecialidadDTO> listarPorOrganoControl(@PathVariable Long organoControlId) {
        return especialidadService.listarPorOrganoControl(organoControlId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public EspecialidadDTO obtenerPorId(@PathVariable Long id) {
        return especialidadService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public EspecialidadDTO crear(@Valid @RequestBody EspecialidadDTO dto) {
        return especialidadService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public EspecialidadDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadDTO dto) {
        return especialidadService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public EspecialidadDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        return especialidadService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public void eliminar(@PathVariable Long id) {
        especialidadService.eliminar(id);
    }
}