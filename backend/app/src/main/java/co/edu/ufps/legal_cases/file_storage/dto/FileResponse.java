package co.edu.ufps.legal_cases.file_storage.dto;

import java.time.LocalDateTime;

public record FileResponse(
        Long id,
        String fileName,
        long size,
        String contentType,
        String status,
        LocalDateTime createdAt) {
}
