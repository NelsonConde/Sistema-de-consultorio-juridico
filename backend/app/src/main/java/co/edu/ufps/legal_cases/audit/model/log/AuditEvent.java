package co.edu.ufps.legal_cases.audit.model.log;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

/** Contrato interno inmutable para persistir un evento de auditoría. */
@Value
@Builder(toBuilder = true)
public class AuditEvent {
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
}
