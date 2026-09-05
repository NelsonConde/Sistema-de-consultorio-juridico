package co.edu.ufps.legal_cases.file_storage.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ufps.legal_cases.file_storage.dto.ExpedienteDocumentoResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileDownloadResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadCompletionRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadResponse;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.service.FileResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** API de archivos orientada a recursos del dominio, no a rutas del bucket. */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Archivos", description = "Operaciones de carga, descarga y consulta documental agregada del expediente")
public class FileResourceController {

    private final FileResourceService fileResourceService;

    public FileResourceController(FileResourceService fileResourceService) {
        this.fileResourceService = fileResourceService;
    }

    @PostMapping("/consultas/{consultaId}/archivos/uploads")
    public FileUploadResponse iniciarCargaConsulta(
            @PathVariable Long consultaId,
            @Valid @RequestBody FileUploadRequest request) {
        return fileResourceService.initiate(
                FileResourceType.CONSULTA, consultaId, null, request);
    }

    @GetMapping("/consultas/{consultaId}/archivos")
    public List<FileResponse> listarArchivosConsulta(@PathVariable Long consultaId) {
        return fileResourceService.list(FileResourceType.CONSULTA, consultaId, null);
    }

    @PostMapping("/seguimientos/{seguimientoId}/archivos/uploads")
    public FileUploadResponse iniciarCargaSeguimiento(
            @PathVariable Long seguimientoId,
            @Valid @RequestBody FileUploadRequest request) {
        return fileResourceService.initiate(
                FileResourceType.SEGUIMIENTO, seguimientoId, null, request);
    }

    @GetMapping("/seguimientos/{seguimientoId}/archivos")
    public List<FileResponse> listarArchivosSeguimiento(@PathVariable Long seguimientoId) {
        return fileResourceService.list(FileResourceType.SEGUIMIENTO, seguimientoId, null);
    }

    @PostMapping("/seguimientos/{seguimientoId}/respuestas/{respuestaId}/archivos/uploads")
    public FileUploadResponse iniciarCargaRespuesta(
            @PathVariable Long seguimientoId,
            @PathVariable Long respuestaId,
            @Valid @RequestBody FileUploadRequest request) {
        return fileResourceService.initiate(
                FileResourceType.RESPUESTA, respuestaId, seguimientoId, request);
    }

    @GetMapping("/seguimientos/{seguimientoId}/respuestas/{respuestaId}/archivos")
    public List<FileResponse> listarArchivosRespuesta(
            @PathVariable Long seguimientoId,
            @PathVariable Long respuestaId) {
        return fileResourceService.list(FileResourceType.RESPUESTA, respuestaId, seguimientoId);
    }

    @PostMapping("/conciliaciones/{conciliacionId}/archivos/uploads")
    public FileUploadResponse iniciarCargaConciliacion(
            @PathVariable Long conciliacionId,
            @Valid @RequestBody FileUploadRequest request) {
        return fileResourceService.initiate(
                FileResourceType.CONCILIACION, conciliacionId, null, request);
    }

    @GetMapping("/conciliaciones/{conciliacionId}/archivos")
    public List<FileResponse> listarArchivosConciliacion(@PathVariable Long conciliacionId) {
        return fileResourceService.list(FileResourceType.CONCILIACION, conciliacionId, null);
    }

    @PostMapping("/procesos/{procesoId}/archivos/uploads")
    @Operation(summary = "Iniciar carga de archivo para un proceso")
    public FileUploadResponse iniciarCargaProceso(
            @PathVariable Long procesoId,
            @Valid @RequestBody FileUploadRequest request) {
        return fileResourceService.initiate(
                FileResourceType.PROCESO, procesoId, null, request);
    }

    @GetMapping("/procesos/{procesoId}/archivos")
    @Operation(summary = "Listar archivos de un proceso")
    public List<FileResponse> listarArchivosProceso(@PathVariable Long procesoId) {
        return fileResourceService.list(FileResourceType.PROCESO, procesoId, null);
    }

    @GetMapping("/consultas/{consultaId}/expediente/archivos")
    @Operation(
            summary = "Consultar expediente documental agregado",
            description = "Deriva y consolida en una sola vista todos los documentos del expediente: "
                    + "CONSULTA, SEGUIMIENTO, RESPUESTA, PROCESO y CONCILIACION, con filtros opcionales de tipo, origen, autor y fecha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado documental del expediente obtenido correctamente"),
            @ApiResponse(responseCode = "400", description = "Filtro de fechas u otro parámetro inválido"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuario sin permiso para acceder a consultas"),
            @ApiResponse(responseCode = "404", description = "Consulta inexistente o fuera del alcance autorizado")
    })
    public List<ExpedienteDocumentoResponse> listarArchivosExpediente(
            @Parameter(description = "ID de la consulta raíz del expediente", required = true)
            @PathVariable Long consultaId,
            @Parameter(description = "Filtro opcional por tipo documental específico (ej. CONSULTA_ANEXO, PROCESO_DOCUMENTO)")
            @RequestParam(required = false) String tipoDocumental,
            @Parameter(description = "Filtro opcional por tipo de recurso (CONSULTA, SEGUIMIENTO, RESPUESTA, PROCESO, CONCILIACION)")
            @RequestParam(required = false) String resourceType,
            @Parameter(description = "Filtro opcional por origen (CARGA_USUARIO, SISTEMA, MIGRADO)")
            @RequestParam(required = false) String origen,
            @Parameter(description = "Filtro opcional por autor (ID numérico o username/email)")
            @RequestParam(required = false) String autor,
            @Parameter(description = "Fecha inicial de creación (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fecha final de creación (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return fileResourceService.listExpedienteFiles(
                consultaId, tipoDocumental, resourceType, origen, autor, fechaDesde, fechaHasta);
    }

    @PostMapping("/file-uploads/{uploadId}/complete")
    public FileResponse completarCarga(
            @PathVariable UUID uploadId,
            @RequestBody(required = false) FileUploadCompletionRequest request) {
        return fileResourceService.complete(uploadId, request == null ? null : request.parentId());
    }

    @DeleteMapping("/file-uploads/{uploadId}")
    public ResponseEntity<Void> cancelarCarga(@PathVariable UUID uploadId) {
        fileResourceService.abort(uploadId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archivos/{fileId}/download")
    public FileDownloadResponse prepararDescarga(
            @PathVariable Long fileId,
            @RequestParam(required = false) Long parentId) {
        return fileResourceService.prepareDownload(fileId, parentId);
    }

    @GetMapping("/documentos/{documentoLogico}/versiones")
    public List<FileResponse> listarVersionesDocumento(
            @PathVariable UUID documentoLogico,
            @RequestParam(required = false) Long parentId) {
        return fileResourceService.listVersions(documentoLogico, parentId);
    }

    @DeleteMapping("/archivos/{fileId}")
    public ResponseEntity<Void> eliminarArchivo(
            @PathVariable Long fileId,
            @RequestParam(required = false) Long parentId) {
        fileResourceService.delete(fileId, parentId);
        return ResponseEntity.noContent().build();
    }
}
