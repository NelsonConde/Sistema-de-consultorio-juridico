package co.edu.ufps.legal_cases.file_storage.service;

import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.dto.ExpedienteDocumentoResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileDownloadResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileResponse;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadRequest;
import co.edu.ufps.legal_cases.file_storage.dto.FileUploadResponse;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

/** Caso de uso único para adjuntar archivos a recursos funcionales. */
@Service
public class FileResourceService {

    private final FileAssetService fileAssetService;
    private final FileValidationService validationService;
    private final FileResourceAuthorizationService authorizationService;
    private final StorageProvider storageProvider;
    private final UsuarioActualService usuarioActualService;
    private final ConsultaAccessService consultaAccessService;
    private final Duration uploadUrlValidity;
    private final Duration downloadUrlValidity;

    @Autowired
    public FileResourceService(
            FileAssetService fileAssetService,
            FileValidationService validationService,
            FileResourceAuthorizationService authorizationService,
            StorageProvider storageProvider,
            UsuarioActualService usuarioActualService,
            ConsultaAccessService consultaAccessService,
            @Value("${file.storage.upload-url-validity:PT10M}") Duration uploadUrlValidity,
            @Value("${file.storage.download-url-validity:PT5M}") Duration downloadUrlValidity) {
        this.fileAssetService = fileAssetService;
        this.validationService = validationService;
        this.authorizationService = authorizationService;
        this.storageProvider = storageProvider;
        this.usuarioActualService = usuarioActualService;
        this.consultaAccessService = consultaAccessService;
        this.uploadUrlValidity = uploadUrlValidity;
        this.downloadUrlValidity = downloadUrlValidity;
    }

    public FileResourceService(
            FileAssetService fileAssetService,
            FileValidationService validationService,
            FileResourceAuthorizationService authorizationService,
            StorageProvider storageProvider,
            UsuarioActualService usuarioActualService,
            Duration uploadUrlValidity,
            Duration downloadUrlValidity) {
        this(fileAssetService, validationService, authorizationService, storageProvider,
                usuarioActualService, null, uploadUrlValidity, downloadUrlValidity);
    }

    @Transactional
    public FileUploadResponse initiate(
            FileResourceType type,
            Long resourceId,
            Long parentId,
            FileUploadRequest request) {
        authorizationService.authorizeUpload(type, resourceId, parentId);
        validationService.validateMetadata(request.fileName(), request.size(), request.contentType());
        if (type == FileResourceType.CONCILIACION) {
            validationService.validatePdfMetadata(request.fileName(), request.contentType());
        }
        validationService.validateChecksum(request.checksum());

        FileAsset asset = fileAssetService.startUpload(
                type,
                resourceId,
                request.fileName(),
                request.contentType(),
                request.size(),
                request.checksum(),
                request.documentoLogico(),
                request.tipoDocumental());

        try {
            StorageProvider.PresignedUpload upload = storageProvider.createUploadUrl(
                    asset.getObjectKey(),
                    request.contentType(),
                    request.size(),
                    uploadUrlValidity);
            return new FileUploadResponse(
                    asset.getUploadId().toString(), upload.url(), upload.expiresAt());
        } catch (RuntimeException ex) {
            fileAssetService.markUploadFailed(asset.getUploadId());
            throw ex;
        }
    }

    /**
     * Adaptador interno para operaciones de negocio que aún deben ser
     * atómicas con la creación o finalización de una entidad.
     */
    @Transactional
    public FileAsset storeMultipartAfterAuthorization(
            FileResourceType type,
            Long resourceId,
            Long parentId,
            MultipartFile file) {
        return storeMultipartAfterAuthorization(type, resourceId, parentId, file, null, null);
    }

    @Transactional
    public FileAsset storeMultipartAfterAuthorization(
            FileResourceType type,
            Long resourceId,
            Long parentId,
            MultipartFile file,
            UUID documentoLogico,
            String tipoDocumental) {
        if (type == FileResourceType.CONCILIACION) {
            validationService.validatePdf(file);
        } else {
            validationService.validate(file);
        }
        FileAsset asset = fileAssetService.startUpload(
                type,
                resourceId,
                file.getOriginalFilename(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getSize(),
                null,
                documentoLogico,
                tipoDocumental);
        try {
            storageProvider.store(file, asset.getObjectKey());
            StorageProvider.StorageObjectMetadata metadata = storageProvider.head(asset.getObjectKey());
            return fileAssetService.markReady(
                    asset.getUploadId(),
                    metadata.contentLength(),
                    metadata.contentType());
        } catch (RuntimeException ex) {
            try {
                storageProvider.delete(asset.getObjectKey());
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            fileAssetService.markUploadFailed(asset.getUploadId());
            throw ex;
        }
    }

    public void discardStoredAsset(FileAsset asset) {
        if (asset == null || asset.getObjectKey() == null) {
            return;
        }

        storageProvider.delete(asset.getObjectKey());

        if (asset.getUploadId() != null) {
            fileAssetService.markUploadFailed(asset.getUploadId());
        }
    }

    @Transactional
    public FileResponse complete(UUID uploadId, Long parentId) {
        FileAsset asset = fileAssetService.findByUploadId(uploadId);
        if (asset.getUploadedBy() == null
                || !Objects.equals(asset.getUploadedBy().getId(), usuarioActualService.obtenerUsuarioActualId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permisos para finalizar esta carga");
        }
        authorizationService.authorizeUpload(
                authorizationService.parseType(asset.getResourceType()),
                asset.getResourceId(),
                parentId);

        StorageProvider.StorageObjectMetadata metadata = storageProvider.head(asset.getObjectKey());
        if (metadata.contentLength() != asset.getSize()) {
            fileAssetService.markUploadFailed(uploadId);
            throw new IllegalArgumentException("El tamaño del archivo no coincide con la carga declarada");
        }

        if (authorizationService.parseType(asset.getResourceType()) == FileResourceType.CONCILIACION) {
            validateSignedConciliationPdf(asset, uploadId);
        }

        FileAsset ready = fileAssetService.markReady(
                uploadId,
                metadata.contentLength(),
                metadata.contentType() == null ? asset.getContentType() : metadata.contentType());
        return toResponse(ready);
    }

    @Transactional
    public void abort(UUID uploadId) {
        FileAsset asset = fileAssetService.findByUploadId(uploadId);
        if (asset.getUploadedBy() == null
                || !Objects.equals(asset.getUploadedBy().getId(), usuarioActualService.obtenerUsuarioActualId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permisos para cancelar esta carga");
        }
        if (asset.getStatus() == co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus.UPLOADING
                || asset.getStatus() == co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus.PENDING) {
            storageProvider.delete(asset.getObjectKey());
            fileAssetService.markUploadFailed(uploadId);
        }
    }

    @Transactional(readOnly = true)
    @Auditable(
            action = "READ_FILE_LIST",
            entityName = "FileResource",
            entityId = "#resourceId",
            metadata = {"resourceType=#type", "parentId=#parentId"})
    public List<FileResponse> list(FileResourceType type, Long resourceId, Long parentId) {
        authorizationService.authorizeRead(type, resourceId, parentId);
        return fileAssetService.listReady(type, resourceId).stream()
                .map(FileResourceService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Auditable(
            action = "DOWNLOAD_FILE",
            entityName = "FileAsset",
            entityId = "#fileId",
            metadata = "parentId=#parentId")
    public FileDownloadResponse prepareDownload(Long fileId, Long parentId) {
        FileAsset asset = fileAssetService.findReady(fileId);
        authorizationService.authorizeRead(asset, parentId);
        StorageProvider.PresignedDownload download = storageProvider.createDownloadUrl(
                asset.getObjectKey(), downloadUrlValidity);
        return new FileDownloadResponse(
                asset.getOriginalFileName(),
                asset.getContentType(),
                download.url(),
                download.expiresAt());
    }

    @Transactional
    public void delete(Long fileId, Long parentId) {
        FileAsset asset = fileAssetService.findReady(fileId);
        FileResourceType type = authorizationService.parseType(asset.getResourceType());
        authorizationService.authorizeUpload(type, asset.getResourceId(), parentId);

        fileAssetService.markDeletePending(fileId);
        try {
            storageProvider.delete(asset.getObjectKey());
            fileAssetService.markDeleted(fileId);
        } catch (RuntimeException ex) {
            fileAssetService.restoreReady(fileId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listVersions(UUID documentoLogico, Long parentId) {
        List<FileAsset> versions = fileAssetService.listVersions(documentoLogico);
        if (!versions.isEmpty()) {
            authorizationService.authorizeRead(versions.get(0), parentId);
        }
        return versions.stream()
                .map(FileResourceService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Auditable(
            action = "READ_EXPEDIENTE_FILES",
            entityName = "Consulta",
            entityId = "#consultaId")
    public List<ExpedienteDocumentoResponse> listExpedienteFiles(
            Long consultaId,
            String tipoDocumental,
            String resourceType,
            String origen,
            String autor,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        if (consultaAccessService != null) {
            consultaAccessService.validarPuedeVerConsulta(consultaId);
        }

        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessException("La fecha desde no puede ser posterior a la fecha hasta");
        }

        java.time.LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        java.time.LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(23, 59, 59, 999999999) : null;

        List<FileAsset> assets = fileAssetService.findExpedienteFiles(
                consultaId,
                tipoDocumental,
                resourceType,
                origen,
                autor,
                desde,
                hasta);

        return assets.stream()
                .map(FileResourceService::toExpedienteResponse)
                .toList();
    }

    private static ExpedienteDocumentoResponse toExpedienteResponse(FileAsset asset) {
        String status = asset.getStatus() == null ? null : asset.getStatus().name();
        Long referenciaAnteriorId = (asset.getReferenciaAnterior() != null)
                ? asset.getReferenciaAnterior().getId()
                : null;
        long size = (asset.getSize() != null) ? asset.getSize() : 0L;
        Long autorId = asset.getUploadedBy() != null ? asset.getUploadedBy().getId() : null;
        String autorUsername = asset.getUploadedBy() != null ? asset.getUploadedBy().getUsername() : null;

        return new ExpedienteDocumentoResponse(
                asset.getId(),
                asset.getDocumentoLogico(),
                asset.getVersion(),
                asset.getTipoDocumental(),
                asset.getOrigen(),
                referenciaAnteriorId,
                asset.getResourceType(),
                asset.getResourceId(),
                asset.getOriginalFileName(),
                size,
                asset.getContentType(),
                status,
                autorId,
                autorUsername,
                asset.getCreatedAt());
    }

    private static FileResponse toResponse(FileAsset asset) {
        String status = asset.getStatus() == null ? null : asset.getStatus().name();
        Long referenciaAnteriorId = (asset.getReferenciaAnterior() != null)
                ? asset.getReferenciaAnterior().getId()
                : null;
        long size = (asset.getSize() != null) ? asset.getSize() : 0L;
        return new FileResponse(
                asset.getId(),
                asset.getDocumentoLogico(),
                asset.getVersion(),
                asset.getTipoDocumental(),
                asset.getOrigen(),
                referenciaAnteriorId,
                asset.getOriginalFileName(),
                size,
                asset.getContentType(),
                status,
                asset.getCreatedAt());
    }

    private void validateSignedConciliationPdf(FileAsset asset, UUID uploadId) {
        try (InputStream input = storageProvider.load(asset.getObjectKey()).getInputStream()) {
            validationService.validatePdfContent(asset.getOriginalFileName(), asset.getContentType(), input);
        } catch (IOException ex) {
            try {
                storageProvider.delete(asset.getObjectKey());
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            fileAssetService.markUploadFailed(uploadId);
            throw new FileStorageException("No se pudo validar el PDF de conciliación", ex);
        } catch (RuntimeException ex) {
            try {
                storageProvider.delete(asset.getObjectKey());
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            fileAssetService.markUploadFailed(uploadId);
            throw ex;
        }
    }
}
