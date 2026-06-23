package co.edu.ufps.legal_cases.business.controller.consulta;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ARCHIVAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_CONSULTAS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.service.consulta.ConsultaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    /**
     * Búsqueda de consultas jurídicas filtrada según el usuario autenticado.
     * El frontend no debe filtrar consultas ajenas; el backend devuelve solo las
     * permitidas.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CONSULTAS + "', '" + GESTIONAR_CONSULTAS + "')")
    public List<ConsultaBusquedaDTO> buscar(
            @RequestParam(required = false, defaultValue = "") String search) {
        return consultaService.buscarParaUsuarioActual(search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CONSULTAS + "', '" + GESTIONAR_CONSULTAS + "')")
    public ConsultaDTO obtenerPorId(@PathVariable Long id) {
        return consultaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + CREAR_CONSULTAS + "', '" + GESTIONAR_CONSULTAS + "')")
    public ConsultaDTO crear(@Valid @RequestBody ConsultaDTO dto) {
        return consultaService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + EDITAR_CONSULTAS + "', '" + GESTIONAR_CONSULTAS + "')")
    public ConsultaDTO actualizar(@PathVariable Long id, @Valid @RequestBody ConsultaDTO dto) {
        return consultaService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('" + CAMBIAR_ESTADO_CONSULTAS + "', '" + ARCHIVAR_CONSULTAS + "', '"
            + GESTIONAR_CONSULTAS + "')")
    public ConsultaDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoConsulta estado) {
        return consultaService.cambiarEstado(id, estado);
    }

    /**
     * Se conserva el endpoint DELETE por compatibilidad.
     * Internamente no elimina físicamente la consulta: la archiva.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + ARCHIVAR_CONSULTAS + "')")
    public void eliminar(@PathVariable Long id) {
        consultaService.eliminar(id);
    }

    @PatchMapping("/{id}/archivar")
    @PreAuthorize("hasAuthority('" + ARCHIVAR_CONSULTAS + "')")
    public ConsultaDTO archivar(@PathVariable Long id) {
        return consultaService.archivar(id);
    }

    @GetMapping("/archivadas")
    @PreAuthorize("hasAuthority('" + ARCHIVAR_CONSULTAS + "')")
    public List<ConsultaBusquedaDTO> listarArchivadas() {
        return consultaService.listarArchivadas();
    }

    @PatchMapping("/{id}/desarchivar")
    @PreAuthorize("hasAuthority('" + ARCHIVAR_CONSULTAS + "')")
    public ConsultaDTO desarchivar(@PathVariable Long id) {
        return consultaService.desarchivar(id);
    }
}