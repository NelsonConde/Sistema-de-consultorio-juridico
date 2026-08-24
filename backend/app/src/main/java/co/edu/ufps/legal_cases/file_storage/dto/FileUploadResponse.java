package co.edu.ufps.legal_cases.file_storage.dto;

import java.time.Instant;

public record FileUploadResponse(
        String uploadId,
        String uploadUrl,
        Instant expiresAt) {
}
