package co.edu.ufps.legal_cases.file_storage.controller;

import java.util.List;
import java.util.UUID;

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

import co.edu.ufps.legal_cases.file_storage.dto.FileDownloadResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadCompletionRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadResponse;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.service.FileResourceService;
import jakarta.validation.Valid;

/** API de archivos orientada a recursos del dominio, no a rutas del bucket. */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
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

    @DeleteMapping("/archivos/{fileId}")
    public ResponseEntity<Void> eliminarArchivo(
            @PathVariable Long fileId,
            @RequestParam(required = false) Long parentId) {
        fileResourceService.delete(fileId, parentId);
        return ResponseEntity.noContent().build();
    }
}
