package co.edu.ufps.legal_cases.business.controller.perfil;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_ESTUDIANTES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ESTUDIANTES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERFILES_AUXILIARES;
import co.edu.ufps.legal_cases.business.dto.perfil.ImportacionEstudiantesDTO;
import co.edu.ufps.legal_cases.business.service.perfil.estudiante.EstudianteExcelService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import co.edu.ufps.legal_cases.business.dto.perfil.EstudianteDTO;
import co.edu.ufps.legal_cases.business.service.perfil.EstudianteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController {

    private final EstudianteService estudianteService;
    private final EstudianteExcelService estudianteExcelService;

    public EstudianteController(EstudianteService estudianteService, EstudianteExcelService estudianteExcelService) {

        this.estudianteService = estudianteService;
        this.estudianteExcelService = estudianteExcelService;

    }

    // Listado con alcance:
    // Administrador ve todos; asesor ve solo sus estudiantes.
    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_ESTUDIANTES + "', '" + VER_PERFILES_AUXILIARES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<EstudianteDTO> listar() {
        return estudianteService.listar();
    }

    // Solo activos con el mismo alcance del listado general.
    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_ESTUDIANTES + "', '" + VER_PERFILES_AUXILIARES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<EstudianteDTO> listarActivos() {
        return estudianteService.listarActivos();
    }

    @GetMapping("/conciliacion")
    @PreAuthorize("hasAnyAuthority('" + VER_ESTUDIANTES + "', '" + VER_PERFILES_AUXILIARES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<EstudianteDTO> listarConConciliacion() {
        return estudianteService.listarConConciliacion();
    }

    // Si el usuario es asesor, solo puede consultar su propio id de asesor.
    @GetMapping("/activos/asesor/{asesorId}")
    @PreAuthorize("hasAnyAuthority('" + VER_ESTUDIANTES + "', '" + VER_PERFILES_AUXILIARES + "', '" + GESTIONAR_USUARIOS + "')")
    public List<EstudianteDTO> listarActivosPorAsesor(@PathVariable Long asesorId) {
        return estudianteService.listarActivosPorAsesor(asesorId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_ESTUDIANTES + "', '" + VER_PERFILES_AUXILIARES + "', '" + GESTIONAR_USUARIOS + "')")
    public EstudianteDTO obtenerPorId(@PathVariable Long id) {
        return estudianteService.obtenerPorId(id);
    }

    // Crear estudiante crea también usuario del sistema.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + CREAR_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public EstudianteDTO crear(@Valid @RequestBody EstudianteDTO dto) {
        return estudianteService.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + EDITAR_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public EstudianteDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EstudianteDTO dto
    ) {
        return estudianteService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasAnyAuthority('" + CAMBIAR_ESTADO_ESTUDIANTES + "', '" + GESTIONAR_USUARIOS + "')")
    public EstudianteDTO cambiarEstado(
            @PathVariable Long id,
            @RequestParam Boolean activo
    ) {
        return estudianteService.cambiarEstado(id, activo);
    }

    @PatchMapping("/{id}/conciliacion")
    @PreAuthorize("hasAnyAuthority('" + EDITAR_USUARIOS + "', '" + GESTIONAR_USUARIOS + "')")
    public EstudianteDTO cambiarConciliacion(
            @PathVariable Long id,
            @RequestParam Boolean conciliacion
    ) {
        return estudianteService.cambiarConciliacion(id, conciliacion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + GESTIONAR_USUARIOS + "')")
    public void eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
    }

    @PostMapping("/importar")
    @PreAuthorize(
            "hasAuthority('" + GESTIONAR_USUARIOS + "')"
    )
    public ResponseEntity<?> importar(@RequestParam MultipartFile archivo) {

        try {

            ImportacionEstudiantesDTO resultado =
                    estudianteExcelService.importar(archivo);

            return ResponseEntity.ok(resultado);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno: " + e.getMessage());

        }
    }
}