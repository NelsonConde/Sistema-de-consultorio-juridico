package co.edu.ufps.legal_cases.file_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** Metadatos declarados antes de una carga directa al proveedor. */
public record FileUploadRequest(
        @NotBlank String fileName,
        @NotNull @Positive Long size,
        @NotBlank String contentType,
        String checksum,
        UUID documentoLogico,
        String tipoDocumental) {

    public FileUploadRequest(String fileName, Long size, String contentType, String checksum) {
        this(fileName, size, contentType, checksum, null, null);
    }
}
