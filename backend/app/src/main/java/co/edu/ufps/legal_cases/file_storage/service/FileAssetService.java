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
        return startUpload(resourceType, resourceId, originalFileName, contentType, size, checksum, null, null);
    }

    @Transactional
    public FileAsset startUpload(
            FileResourceType resourceType,
            Long resourceId,
            String originalFileName,
            String contentType,
            long size,
            String checksum,
            UUID documentoLogico,
            String tipoDocumental) {
        if (resourceType == null || resourceId == null || resourceId <= 0) {
            throw new BusinessException("El recurso documental es obligatorio");
        }

        String safeName = cleanFileName(originalFileName);

        UUID resolvedDocLogico;
        int version;
        FileAsset referenciaAnterior = null;

        if (documentoLogico == null) {
            resolvedDocLogico = UUID.randomUUID();
            version = 1;
        } else {
            // Se bloquea pesimísticamente la versión vigente actual para control de concurrencia
            var optVigente = repository.findVigenteForUpdate(documentoLogico, FileAssetStatus.VIGENTE);
            if (optVigente.isPresent()) {
                FileAsset vigente = optVigente.get();
                if (!vigente.getResourceType().equalsIgnoreCase(resourceType.name())
                        || !vigente.getResourceId().equals(resourceId)) {
                    throw new BusinessException("El documento lógico no pertenece al recurso indicado");
                }
                resolvedDocLogico = documentoLogico;
                version = vigente.getVersion() + 1;
                referenciaAnterior = vigente;
            } else {
                List<FileAsset> existing = repository.findByDocumentoLogico(documentoLogico);
                if (existing.isEmpty()) {
                    resolvedDocLogico = documentoLogico;
                    version = 1;
                } else {
                    FileAsset any = existing.get(0);
                    if (!any.getResourceType().equalsIgnoreCase(resourceType.name())
                            || !any.getResourceId().equals(resourceId)) {
                        throw new BusinessException("El documento lógico no pertenece al recurso indicado");
                    }
                    resolvedDocLogico = documentoLogico;
                    Integer maxVer = repository.findMaxVersionByDocumentoLogico(documentoLogico);
                    version = (maxVer != null && maxVer >= 1) ? maxVer + 1 : 1;
                    referenciaAnterior = any;
                }
            }
        }

        // Autor y origen se calculan estrictamente en el servidor
        var usuarioActual = usuarioActualService.obtenerUsuarioActual();
        String origen = (usuarioActual != null) ? "CARGA_USUARIO" : "SISTEMA";

        String resolvedTipoDoc = resolveTipoDocumental(tipoDocumental, resourceType, safeName);

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
        asset.setUploadedBy(usuarioActual);
        asset.setDocumentoLogico(resolvedDocLogico);
        asset.setVersion(version);
        asset.setTipoDocumental(resolvedTipoDoc);
        asset.setOrigen(origen);
        asset.setReferenciaAnterior(referenciaAnterior);
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
        if (asset.getStatus() != FileAssetStatus.VIGENTE
                && asset.getStatus() != FileAssetStatus.READY
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
                List.of(FileAssetStatus.VIGENTE, FileAssetStatus.READY, FileAssetStatus.ACTIVE)));
        if (resourceType == FileResourceType.RESPUESTA) {
            assets.addAll(repository.findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                    "SEGUIMIENTO_RESPUESTA",
                    resourceId,
                    List.of(FileAssetStatus.VIGENTE, FileAssetStatus.READY, FileAssetStatus.ACTIVE)));
        }
        assets.sort(Comparator.comparing(FileAsset::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return assets;
    }

    @Transactional(readOnly = true)
    public List<FileAsset> listVersions(UUID documentoLogico) {
        if (documentoLogico == null) {
            throw new BusinessException("El documento lógico es obligatorio");
        }
        return repository.findByDocumentoLogicoOrderByVersionDesc(documentoLogico);
    }

    @Transactional
    public FileAsset markReady(UUID uploadId, long size, String contentType) {
        FileAsset asset = findByUploadId(uploadId);
        if (asset.getStatus() != FileAssetStatus.UPLOADING
                && asset.getStatus() != FileAssetStatus.PENDING) {
            if (asset.getStatus() == FileAssetStatus.VIGENTE
                    || asset.getStatus() == FileAssetStatus.READY
                    || asset.getStatus() == FileAssetStatus.ACTIVE) {
                return asset;
            }
            throw new BusinessException("La carga documental no puede finalizarse");
        }

        // Si es una nueva versión, pasar la versión anterior a HISTORICO
        if (asset.getReferenciaAnterior() != null) {
            FileAsset anterior = asset.getReferenciaAnterior();
            if (anterior.getStatus() == FileAssetStatus.VIGENTE
                    || anterior.getStatus() == FileAssetStatus.READY
                    || anterior.getStatus() == FileAssetStatus.ACTIVE) {
                anterior.setStatus(FileAssetStatus.HISTORICO);
                anterior.setActive(false);
                repository.save(anterior);
            }
        }

        // Asegurar que no quede otra versión VIGENTE para este documento lógico
        repository.findByDocumentoLogicoAndStatus(asset.getDocumentoLogico(), FileAssetStatus.VIGENTE)
                .ifPresent(otroVigente -> {
                    if (!otroVigente.getId().equals(asset.getId())) {
                        otroVigente.setStatus(FileAssetStatus.HISTORICO);
                        otroVigente.setActive(false);
                        repository.save(otroVigente);
                    }
                });

        asset.setSize(size);
        if (contentType != null && !contentType.isBlank()) {
            asset.setContentType(contentType);
        }
        asset.setStatus(FileAssetStatus.VIGENTE);
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
        if (asset.getStatus() == FileAssetStatus.HISTORICO) {
            throw new BusinessException("Las versiones históricas son inmutables y no pueden eliminarse");
        }
        asset.setStatus(FileAssetStatus.DELETED);
        asset.setActive(false);
        return repository.save(asset);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileAsset markDeletePending(Long id) {
        FileAsset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Archivo no encontrado"));
        if (asset.getStatus() == FileAssetStatus.HISTORICO) {
            throw new BusinessException("Las versiones históricas son inmutables y no pueden eliminarse");
        }
        asset.setStatus(FileAssetStatus.DELETE_PENDING);
        asset.setActive(false);
        return repository.save(asset);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileAsset restoreReady(Long id) {
        FileAsset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Archivo no encontrado"));
        asset.setStatus(FileAssetStatus.VIGENTE);
        asset.setActive(true);
        return repository.save(asset);
    }

    private static String resolveTipoDocumental(String tipo, FileResourceType resourceType, String safeName) {
        if (tipo != null && !tipo.isBlank()) {
            return tipo.trim().toUpperCase(java.util.Locale.ROOT);
        }
        if (resourceType == FileResourceType.CONCILIACION) {
            String lower = safeName.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("solicitud")) {
                return "CONCILIACION_SOLICITUD";
            }
            if (lower.contains("acta")) {
                return "CONCILIACION_ACTA";
            }
            return "CONCILIACION_DOCUMENTO";
        }
        if (resourceType == FileResourceType.CONSULTA) {
            return "CONSULTA_ANEXO";
        }
        if (resourceType == FileResourceType.RESPUESTA) {
            return "RESPUESTA_EVIDENCIA";
        }
        if (resourceType == FileResourceType.SEGUIMIENTO) {
            return "SEGUIMIENTO_ANEXO";
        }
        if (resourceType == FileResourceType.PROCESO) {
            return "PROCESO_DOCUMENTO";
        }
        return "GENERAL";
    }

    @Transactional(readOnly = true)
    public List<FileAsset> findExpedienteFiles(
            Long consultaId,
            String tipoDocumental,
            String resourceType,
            String origen,
            String autor,
            java.time.LocalDateTime fechaDesde,
            java.time.LocalDateTime fechaHasta) {
        if (consultaId == null || consultaId <= 0) {
            throw new BusinessException("El id de la consulta es obligatorio");
        }

        Long autorId = null;
        if (autor != null && !autor.isBlank()) {
            try {
                autorId = Long.valueOf(autor.trim());
            } catch (NumberFormatException ignored) {
                // autor no es numérico, se filtra como texto
            }
        }

        List<FileAssetStatus> visibleStatuses = List.of(
                FileAssetStatus.VIGENTE,
                FileAssetStatus.READY,
                FileAssetStatus.ACTIVE);

        return repository.findExpedienteFiles(
                consultaId,
                visibleStatuses,
                (tipoDocumental != null && !tipoDocumental.isBlank()) ? tipoDocumental.trim() : null,
                (resourceType != null && !resourceType.isBlank()) ? resourceType.trim() : null,
                (origen != null && !origen.isBlank()) ? origen.trim() : null,
                (autor != null && !autor.isBlank()) ? autor.trim() : null,
                autorId,
                fechaDesde,
                fechaHasta);
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
