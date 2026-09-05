package co.edu.ufps.legal_cases.file_storage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** DTO seguro para documentos agregados del expediente; no expone bucket ni objectKey. */
public record ExpedienteDocumentoResponse(
        Long id,
        UUID documentoLogico,
        Integer version,
        String tipoDocumental,
        String origen,
        Long referenciaAnteriorId,
        String resourceType,
        Long resourceId,
        String fileName,
        long size,
        String contentType,
        String status,
        Long autorId,
        String autorUsername,
        LocalDateTime createdAt) {
}
