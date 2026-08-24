package co.edu.ufps.legal_cases.file_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Metadatos declarados antes de una carga directa al proveedor. */
public record FileUploadRequest(
        @NotBlank String fileName,
        @NotNull @Positive Long size,
        @NotBlank String contentType,
        String checksum) {
}
