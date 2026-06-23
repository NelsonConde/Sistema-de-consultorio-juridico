package co.edu.ufps.legal_cases.business.controller.catalogo;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CATALOGOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CATALOGOS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import co.edu.ufps.legal_cases.business.dto.catalogo.TipoDocumentoDTO;
import co.edu.ufps.legal_cases.business.service.catalogo.TipoDocumentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tipos-documento")
public class TipoDocumentoController {

    //Como tiene activo y desactivo no implemente algo para borrar y si se borra se borra de verdad, lo que hice fue un patch para cambiar el estado del tipo de documento a activo o inactivo, asi no se pierde la informacion de los casos que tengan ese tipo de documento asociado
    private final TipoDocumentoService tipoDocumentoService;

    public TipoDocumentoController(TipoDocumentoService tipoDocumentoService) {
        this.tipoDocumentoService = tipoDocumentoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TipoDocumentoDTO> listar() {
        return tipoDocumentoService.listar();
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public List<TipoDocumentoDTO> listarActivos() {
        return tipoDocumentoService.listarActivos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_CATALOGOS + "', '" + GESTIONAR_CATALOGOS + "')")
    public TipoDocumentoDTO obtenerPorId(@PathVariable Long id) {
        return tipoDocumentoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoDocumentoDTO crear(@Valid @RequestBody TipoDocumentoDTO dto) {
        return tipoDocumentoService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoDocumentoDTO actualizar(@PathVariable Long id, @Valid @RequestBody TipoDocumentoDTO dto) {
        return tipoDocumentoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAuthority('" + GESTIONAR_CATALOGOS + "')")
    public TipoDocumentoDTO cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return tipoDocumentoService.cambiarEstado(id, activo);
    }
}