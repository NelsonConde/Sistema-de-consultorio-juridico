package co.edu.ufps.legal_cases.file_storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
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

        // El bucket admite evidencias de cualquier formato. La autorización y
        // la clave física las controla el backend; no el tipo declarado por el
        // navegador. Conservamos sólo el límite de tamaño y un nombre válido.
    }

    /** Valida metadatos antes de emitir una URL firmada. */
    public void validateMetadata(String fileName, long size, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("El archivo debe tener un nombre");
        }
        if (size <= 0 || size > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo supera el límite de 10 MB");
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

    /** Regla de dominio para solicitud y acta de conciliación. */
    public void validatePdf(MultipartFile file) {
        validate(file);
        validatePdfMetadata(file.getOriginalFilename(), file.getContentType());
        try (InputStream input = file.getInputStream()) {
            validatePdfSignature(input);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo validar el contenido del PDF");
        }
    }

    /** Verifica el contenido de un PDF que llegó mediante una URL firmada. */
    public void validatePdfContent(String fileName, String contentType, InputStream input) {
        validatePdfMetadata(fileName, contentType);
        try {
            validatePdfSignature(input);
        } catch (IOException ex) {
            throw new BusinessException("No se pudo validar el contenido del PDF");
        }
    }

    public void validatePdfMetadata(String fileName, String contentType) {
        String normalizedName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (!normalizedName.endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(contentType)) {
            throw new BusinessException("La solicitud y el acta de conciliación deben ser archivos PDF");
        }
    }

    private static void validatePdfSignature(InputStream input) throws IOException {
        byte[] header = input.readNBytes(5);
        byte[] pdfHeader = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (header.length != pdfHeader.length) {
            throw new BusinessException("El contenido no corresponde a un PDF");
        }
        for (int index = 0; index < pdfHeader.length; index++) {
            if (header[index] != pdfHeader[index]) {
                throw new BusinessException("El contenido no corresponde a un PDF");
            }
        }
    }

}
