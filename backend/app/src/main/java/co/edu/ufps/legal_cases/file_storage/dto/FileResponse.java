package co.edu.ufps.legal_cases.file_storage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileResponse(
        Long id,
        UUID documentoLogico,
        Integer version,
        String tipoDocumental,
        String origen,
        Long referenciaAnteriorId,
        String fileName,
        long size,
        String contentType,
        String status,
        LocalDateTime createdAt) {

    public FileResponse(
            Long id,
            String fileName,
            long size,
            String contentType,
            String status,
            LocalDateTime createdAt) {
        this(id, null, 1, "GENERAL", "SISTEMA", null, fileName, size, contentType, status, createdAt);
    }
}
