package co.edu.ufps.legal_cases.business.controller.persona;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CAMBIAR_ESTADO_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERSONAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_PERSONAS;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import co.edu.ufps.legal_cases.business.dto.persona.PersonaDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
import co.edu.ufps.legal_cases.business.service.persona.PersonaService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/personas")
@Tag(name = "Personas", description = "Consulta y gestión de personas")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + VER_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    @Operation(
            summary = "Consultar personas",
            description = "Devuelve un listado paginado de personas visibles dentro del alcance del usuario autenticado, "
                    + "con búsqueda y ordenamiento. El contrato es un resumen y no la ficha completa.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado paginado de personas obtenido correctamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de paginación, búsqueda u ordenamiento inválidos"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuario no autenticado"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuario sin permiso VER_PERSONAS o GESTIONAR_PERSONAS")
    })
    public PageResponseDTO<PersonaResumenDTO> listar(
            @Parameter(
                    description = "Texto opcional para buscar por nombres, apellidos, nombre completo "
                            + "o número de documento. Máximo funcional de 100 caracteres después de normalizar.",
                    schema = @Schema(maxLength = 100))
            @RequestParam(required = false) String search,

            @Parameter(
                    description = "Número de página público, con base 1.",
                    schema = @Schema(defaultValue = "1", minimum = "1"))
            @RequestParam(defaultValue = "1") int page,

            @Parameter(
                    description = "Cantidad de elementos por página.",
                    schema = @Schema(defaultValue = "10", minimum = "1", maximum = "50"))
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Campo público de ordenamiento.",
                    schema = @Schema(
                            defaultValue = "nombres",
                            allowableValues = {
                                    "nombres",
                                    "apellidos",
                                    "numeroDocumento",
                                    "tipoDocumento",
                                    "tipoPersona",
                                    "activo"
                            }))
            @RequestParam(defaultValue = "nombres") String sortBy,

            @Parameter(
                    description = "Dirección de ordenamiento.",
                    schema = @Schema(
                            defaultValue = "asc",
                            allowableValues = { "asc", "desc" }))
            @RequestParam(defaultValue = "asc") String direction) {

        return personaService.listar(search, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + VER_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    @Operation(
            summary = "Consultar detalle de una persona",
            description = "Devuelve el detalle completo únicamente después de validar que la persona está dentro "
                    + "del alcance autorizado. Un ID inexistente y una persona fuera del alcance producen "
                    + "el mismo 404 genérico.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la persona disponible"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuario no autenticado"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuario sin permiso funcional"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Persona inexistente o fuera del alcance autorizado")
    })
    public PersonaDTO obtenerPorId(
            @Parameter(
                    description = "Identificador de la persona",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id) {

        return personaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('" + CREAR_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    // En el correo no se puede usar el "No informa" como estrategia porque en
    // @Email no se admite (debe ser null o correcto)
    public PersonaDTO crear(@Valid @RequestBody PersonaDTO personaDTO) {
        return personaService.crear(personaDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + EDITAR_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    public PersonaDTO actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PersonaDTO personaDTO) {

        return personaService.actualizar(id, personaDTO);
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyAuthority('" + VER_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    @Operation(
            summary = "Consultar personas activas",
            description = "Devuelve únicamente personas activas visibles dentro del alcance del usuario autenticado, "
                    + "en un listado paginado con búsqueda y ordenamiento.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado paginado de personas activas obtenido correctamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de paginación, búsqueda u ordenamiento inválidos"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuario no autenticado"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuario sin permiso VER_PERSONAS o GESTIONAR_PERSONAS")
    })
    public PageResponseDTO<PersonaResumenDTO> listarActivos(
            @Parameter(
                    description = "Texto opcional para buscar por nombres, apellidos, nombre completo "
                            + "o número de documento. Máximo funcional de 100 caracteres después de normalizar.",
                    schema = @Schema(maxLength = 100))
            @RequestParam(required = false) String search,

            @Parameter(
                    description = "Número de página público, con base 1.",
                    schema = @Schema(defaultValue = "1", minimum = "1"))
            @RequestParam(defaultValue = "1") int page,

            @Parameter(
                    description = "Cantidad de elementos por página.",
                    schema = @Schema(defaultValue = "10", minimum = "1", maximum = "50"))
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Campo público de ordenamiento.",
                    schema = @Schema(
                            defaultValue = "nombres",
                            allowableValues = {
                                    "nombres",
                                    "apellidos",
                                    "numeroDocumento",
                                    "tipoDocumento",
                                    "tipoPersona",
                                    "activo"
                            }))
            @RequestParam(defaultValue = "nombres") String sortBy,

            @Parameter(
                    description = "Dirección de ordenamiento.",
                    schema = @Schema(
                            defaultValue = "asc",
                            allowableValues = { "asc", "desc" }))
            @RequestParam(defaultValue = "asc") String direction) {

        return personaService.listarActivos(search, page, size, sortBy, direction);
    }

    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('" + CAMBIAR_ESTADO_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    public void desactivar(
            @PathVariable Long id,
            @RequestParam Long version) {

        personaService.desactivar(id, version);
    }

    @PatchMapping("/{id}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('" + CAMBIAR_ESTADO_PERSONAS + "', '" + GESTIONAR_PERSONAS + "')")
    public void reactivar(
            @PathVariable Long id,
            @RequestParam Long version) {

        personaService.reactivar(id, version);
    }
}