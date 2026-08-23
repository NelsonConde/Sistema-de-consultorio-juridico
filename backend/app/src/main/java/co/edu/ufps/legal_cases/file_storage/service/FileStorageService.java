package co.edu.ufps.legal_cases.file_storage.service;

import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;

/** Fachada compatible con la API existente para almacenamiento documental. */
@Service
public class FileStorageService {

    private final StorageProvider storageProvider;
    private final FileAssetService fileAssetService;
    private final FileValidationService fileValidationService;

    public FileStorageService(
            StorageProvider storageProvider,
            FileAssetService fileAssetService,
            FileValidationService fileValidationService) {
        this.storageProvider = storageProvider;
        this.fileAssetService = fileAssetService;
        this.fileValidationService = fileValidationService;
    }

    @Auditable(action = "CARGAR_ARCHIVO", entityName = "FileAsset")
    public String storeFile(MultipartFile file, String subDir) {
        fileValidationService.validate(file);
        String fileName = cleanFileName(file);
        return storeAndRegister(file, buildObjectKey(subDir, fileName));
    }

    @Auditable(action = "REEMPLAZAR_ARCHIVO", entityName = "FileAsset")
    public String storeFileAs(MultipartFile file, String subDir, String targetFileName) {
        if (file == null) {
            throw new FileStorageException("El archivo es obligatorio");
        }
        fileValidationService.validate(file);
        String fileName = cleanKeyPart(targetFileName, "nombre de archivo");
        return storeAndRegister(file, buildObjectKey(subDir, fileName));
    }

    @Auditable(action = "DESCARGAR_ARCHIVO", entityName = "FileAsset")
    public Resource loadFileAsResource(String fileName) {
        return storageProvider.load(normalizeKey(fileName, "ruta de archivo"));
    }

    @Auditable(action = "LISTAR_ARCHIVOS", entityName = "FileAsset")
    public List<String> listFiles(String subDir) {
        return storageProvider.list(normalizePrefix(subDir));
    }

    public List<String> listDirectories() {
        return storageProvider.listDirectories("");
    }

    private String storeAndRegister(MultipartFile file, String objectKey) {
        boolean existingActive = fileAssetService.isActive(objectKey);
        if (!existingActive) {
            fileAssetService.begin(objectKey, file);
        }
        try {
            String storedKey = storageProvider.store(file, objectKey);
            fileAssetService.activate(storedKey, file);
            return storedKey;
        } catch (RuntimeException ex) {
            if (existingActive) {
                // No se elimina la clave anterior: el reemplazo pudo no haber
                // llegado a completarse y debe conservarse el documento válido.
                throw ex;
            }
            boolean cleanupSucceeded = true;
            try {
                storageProvider.delete(objectKey);
            } catch (RuntimeException cleanupException) {
                cleanupSucceeded = false;
                try {
                    fileAssetService.markDeletePending(objectKey);
                } catch (RuntimeException stateException) {
                    cleanupException.addSuppressed(stateException);
                }
                ex.addSuppressed(cleanupException);
            }
            if (cleanupSucceeded) {
                try {
                    fileAssetService.markFailed(objectKey);
                } catch (RuntimeException stateException) {
                    ex.addSuppressed(stateException);
                }
            }
            throw ex;
        }
    }

    private static String cleanFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("El archivo es obligatorio");
        }
        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "nombre de archivo");
        return cleanKeyPart(StringUtils.cleanPath(originalName), "nombre de archivo");
    }

    private static String buildObjectKey(String subDir, String fileName) {
        String normalizedDirectory = normalizePrefix(subDir);
        return normalizedDirectory.isEmpty() ? fileName : normalizedDirectory + fileName;
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = normalizeKey(value, "directorio");
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static String normalizeKey(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new FileStorageException("La " + description + " es obligatoria");
        }
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new FileStorageException("La " + description + " contiene una ruta inválida");
        }
        return normalized;
    }

    private static String cleanKeyPart(String value, String description) {
        String normalized = normalizeKey(value, description);
        if (normalized.contains("/")) {
            throw new FileStorageException("El " + description + " no puede contener directorios");
        }
        return normalized;
    }
}
