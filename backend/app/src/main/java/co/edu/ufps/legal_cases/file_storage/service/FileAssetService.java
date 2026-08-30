package co.edu.ufps.legal_cases.file_storage.service;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

/** Persistencia de metadata documental, separada del proveedor físico. */
@Service
public class FileAssetService {

    private final FileAssetRepository repository;
    private final UsuarioActualService usuarioActualService;
    private final String bucket;

    public FileAssetService(
            FileAssetRepository repository,
            UsuarioActualService usuarioActualService,
            @Value("${supabase.storage.bucket:legal-documents}") String bucket) {
        this.repository = repository;
        this.usuarioActualService = usuarioActualService;
        this.bucket = bucket;
    }

    @Transactional
    public FileAsset startUpload(
            FileResourceType resourceType,
            Long resourceId,
            String originalFileName,
            String contentType,
            long size,
            String checksum) {
        if (resourceType == null || resourceId == null || resourceId <= 0) {
            throw new BusinessException("El recurso documental es obligatorio");
        }

        String safeName = cleanFileName(originalFileName);
        FileAsset asset = new FileAsset();
        asset.setUploadId(UUID.randomUUID());
        asset.setBucket(bucket);
        asset.setObjectKey(resourceType.name().toLowerCase() + "/" + resourceId + "/"
                + UUID.randomUUID() + "-" + safeName);
        asset.setResourceType(resourceType.name());
        asset.setResourceId(resourceId);
        asset.setOriginalFileName(safeName);
        asset.setContentType(contentType);
        asset.setSize(size);
        asset.setChecksum(checksum == null ? "" : checksum);
        asset.setUploadedBy(usuarioActualService.obtenerUsuarioActual());
        asset.setStatus(FileAssetStatus.UPLOADING);
        asset.setActive(false);
        return repository.save(asset);
    }

    @Transactional(readOnly = true)
    public FileAsset findByUploadId(UUID uploadId) {
        return repository.findByUploadId(uploadId)
                .orElseThrow(() -> new BusinessException("La carga documental no existe"));
    }

    @Transactional(readOnly = true)
    public FileAsset findReady(Long id) {
        FileAsset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Archivo no encontrado"));
        if (asset.getStatus() != FileAssetStatus.READY
                && asset.getStatus() != FileAssetStatus.ACTIVE) {
            throw new BusinessException("El archivo no está disponible");
        }
        return asset;
    }

    @Transactional(readOnly = true)
    public List<FileAsset> listReady(FileResourceType resourceType, Long resourceId) {
        List<FileAsset> assets = new ArrayList<>(repository
                .findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                resourceType.name(),
                resourceId,
                List.of(FileAssetStatus.READY, FileAssetStatus.ACTIVE)));
        if (resourceType == FileResourceType.RESPUESTA) {
            assets.addAll(repository.findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                    "SEGUIMIENTO_RESPUESTA",
                    resourceId,
                    List.of(FileAssetStatus.READY, FileAssetStatus.ACTIVE)));
        }
        assets.sort(Comparator.comparing(FileAsset::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return assets;
    }

    @Transactional
    public FileAsset markReady(UUID uploadId, long size, String contentType) {
        FileAsset asset = findByUploadId(uploadId);
        if (asset.getStatus() != FileAssetStatus.UPLOADING
                && asset.getStatus() != FileAssetStatus.PENDING) {
            if (asset.getStatus() == FileAssetStatus.READY
                    || asset.getStatus() == FileAssetStatus.ACTIVE) {
                return asset;
            }
            throw new BusinessException("La carga documental no puede finalizarse");
        }
        asset.setSize(size);
        if (contentType != null && !contentType.isBlank()) {
            asset.setContentType(contentType);
        }
        asset.setStatus(FileAssetStatus.READY);
        asset.setActive(true);
        return repository.save(asset);
    }

    @Transactional
    public void markUploadFailed(UUID uploadId) {
        FileAsset asset = findByUploadId(uploadId);
        asset.setStatus(FileAssetStatus.FAILED);
        asset.setActive(false);
        repository.save(asset);
    }

    @Transactional
    public void markFailedByObjectKey(String objectKey) {
        repository.findByBucketAndObjectKey(bucket, objectKey).ifPresent(asset -> {
            asset.setStatus(FileAssetStatus.FAILED);
            asset.setActive(false);
            repository.save(asset);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileAsset markDeleted(Long id) {
        FileAsset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Archivo no encontrado"));
        asset.setStatus(FileAssetStatus.DELETED);
        asset.setActive(false);
        return repository.save(asset);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileAsset markDeletePending(Long id) {
        FileAsset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Archivo no encontrado"));
        asset.setStatus(FileAssetStatus.DELETE_PENDING);
        asset.setActive(false);
        return repository.save(asset);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileAsset restoreReady(Long id) {
        FileAsset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Archivo no encontrado"));
        asset.setStatus(FileAssetStatus.READY);
        asset.setActive(true);
        return repository.save(asset);
    }

    private static String cleanFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("El archivo debe tener un nombre");
        }
        String normalized = org.springframework.util.StringUtils.cleanPath(originalName).replace('\\', '/');
        if (normalized.contains("/") || normalized.contains("..")
                || normalized.indexOf('\0') >= 0
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new BusinessException("El nombre del archivo no es válido");
        }
        return normalized;
    }
}
