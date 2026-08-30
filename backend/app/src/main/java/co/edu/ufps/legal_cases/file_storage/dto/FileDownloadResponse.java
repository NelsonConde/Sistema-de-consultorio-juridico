package co.edu.ufps.legal_cases.file_storage.dto;

import java.time.Instant;

public record FileDownloadResponse(
        String fileName,
        String contentType,
        String downloadUrl,
        Instant expiresAt) {
}
