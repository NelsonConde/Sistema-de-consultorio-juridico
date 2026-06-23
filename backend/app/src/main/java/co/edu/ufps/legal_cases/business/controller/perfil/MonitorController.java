package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.service.perfil.MonitorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/monitores")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_ASESORES_MONITORES + "', '" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<MonitorDTO> listar() {
        return monitorService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_PERFILES_AUXILIARES + "', '" + VER_ASESORES_MONITORES + "', '" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<MonitorDTO> listarActivos() {
        return monitorService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_ASESORES_MONITORES + "', '" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public MonitorDTO obtenerPorId(@PathVariable Long id) {
        return monitorService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public MonitorDTO crear(@Valid @RequestBody MonitorDTO dto) {
        return monitorService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public MonitorDTO actualizar(@PathVariable Long id, @Valid @RequestBody MonitorDTO dto) {
        return monitorService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public MonitorDTO cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return monitorService.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('" + GESTIONAR_ASESORES_MONITORES + "', '" + GESTIONAR_USUARIOS + "')")
    public void eliminar(@PathVariable Long id) {
        monitorService.eliminar(id);
    }
}