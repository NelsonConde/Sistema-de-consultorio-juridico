package co.edu.ufps.legal_cases.audit.dto.log;

import java.time.Instant;
import java.util.Map;

import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.model.log.AuditSource;
import lombok.Builder;
import lombok.Value;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Data Transfer Object para enviar información de auditoría a los clientes
 * REST.
 */
@Value
@Builder
public class AuditLogDTO {
    Long id;
    String actorUsername;
    String action;
    String entityName;
    String entityId;
    AuditOutcome outcome;
    Instant occurredAt;
    AuditSource source;
    String correlationId;
    String ipAddress;
    String userAgent;
    String reasonCode;
    String reason;
    Map<String, String> beforeState;
    Map<String, String> afterState;
    Map<String, String> metadata;

    public static AuditLogDTO fromEntity(AuditLog auditLog, JsonMapper objectMapper) {
        if (auditLog == null) {
            return null;
        }
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .actorUsername(auditLog.getActorUsername())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .outcome(auditLog.getOutcome())
                .occurredAt(auditLog.getOccurredAt())
                .source(auditLog.getSource())
                .correlationId(auditLog.getCorrelationId())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .reasonCode(auditLog.getReasonCode())
                .reason(auditLog.getReason())
                .beforeState(readMetadata(auditLog.getBeforeStateJson(), objectMapper))
                .afterState(readMetadata(auditLog.getAfterStateJson(), objectMapper))
                .metadata(readMetadata(auditLog.getMetadataJson(), objectMapper))
                .build();
    }

    private static Map<String, String> readMetadata(String json, JsonMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
