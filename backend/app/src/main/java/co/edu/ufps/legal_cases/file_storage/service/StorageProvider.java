package co.edu.ufps.legal_cases.file_storage.service;

import java.util.List;
import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstracción del almacenamiento documental. La aplicación trabaja con claves
 * lógicas y no conoce el proveedor físico de objetos.
 */
public interface StorageProvider {

    String store(MultipartFile file, String objectKey);

    Resource load(String objectKey);

    void delete(String objectKey);

    List<String> list(String prefix);

    List<String> listDirectories(String prefix);

    /**
     * Genera una URL temporal para que el cliente suba directamente al bucket.
     * Los proveedores antiguos pueden conservar el flujo proxy mientras se
     * completa la migración.
     */
    default PresignedUpload createUploadUrl(
            String objectKey,
            String contentType,
            long contentLength,
            Duration validity) {
        throw new UnsupportedOperationException("El proveedor no soporta cargas firmadas");
    }

    default StorageObjectMetadata head(String objectKey) {
        throw new UnsupportedOperationException("El proveedor no soporta consulta de metadata");
    }

    default PresignedDownload createDownloadUrl(String objectKey, Duration validity) {
        throw new UnsupportedOperationException("El proveedor no soporta descargas firmadas");
    }

    record PresignedUpload(String url, java.time.Instant expiresAt) {
    }

    record PresignedDownload(String url, java.time.Instant expiresAt) {
    }

    record StorageObjectMetadata(long contentLength, String contentType) {
    }
}
