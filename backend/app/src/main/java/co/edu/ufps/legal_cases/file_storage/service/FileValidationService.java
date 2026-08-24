package co.edu.ufps.legal_cases.file_storage.service;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "txt");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo es obligatorio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo supera el límite de 10 MB");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("El archivo debe tener un nombre");
        }

        int extensionSeparator = fileName.lastIndexOf('.');
        String extension = extensionSeparator < 0
                ? ""
                : fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("El tipo de archivo no está permitido");
        }

        validateKnownSignature(file, extension);
    }

    /** Valida metadatos antes de emitir una URL firmada. */
    public void validateMetadata(String fileName, long size, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("El archivo debe tener un nombre");
        }
        if (size <= 0 || size > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo supera el límite de 10 MB");
        }

        int extensionSeparator = fileName.lastIndexOf('.');
        String extension = extensionSeparator < 0
                ? ""
                : fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("El tipo de archivo no está permitido");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException("El tipo de contenido es obligatorio");
        }
    }

    public void validateChecksum(String checksum) {
        if (checksum != null && !checksum.isBlank()
                && !checksum.matches("^[0-9a-fA-F]{64}$")) {
            throw new BusinessException("La huella del archivo no es válida");
        }
    }

    public long maxFileSize() {
        return MAX_FILE_SIZE;
    }

    public Set<String> allowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    private void validateKnownSignature(MultipartFile file, String extension) {
        try {
            byte[] header = file.getBytes();
            if ("pdf".equals(extension) && !startsWith(header, "%PDF-".getBytes())) {
                throw new BusinessException("El contenido no corresponde a un PDF");
            }
            if ("png".equals(extension) && !startsWith(header,
                    new byte[] {(byte) 0x89, 'P', 'N', 'G'})) {
                throw new BusinessException("El contenido no corresponde a una imagen PNG");
            }
            if (("jpg".equals(extension) || "jpeg".equals(extension))
                    && !startsWith(header, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
                throw new BusinessException("El contenido no corresponde a una imagen JPEG");
            }
        } catch (IOException ex) {
            throw new BusinessException("No se pudo validar el contenido del archivo");
        }
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }
}
