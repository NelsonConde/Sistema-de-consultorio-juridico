package co.edu.ufps.legal_cases.audit.service.log;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import co.edu.ufps.legal_cases.audit.repository.log.AuditLogRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Frontera transaccional aislada para la escritura append-only. */
@Component
public class AuditLogWriter {

    private final AuditLogRepository repository;
    private final JsonMapper objectMapper;

    public AuditLogWriter(AuditLogRepository repository, JsonMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void persist(AuditEvent event) {
        repository.save(toEntity(event));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistIndependent(AuditEvent event) {
        repository.save(toEntity(event));
    }

    private AuditLog toEntity(AuditEvent event) {
        return AuditLog.builder()
                .actorUsername(event.getActorUsername())
                .action(event.getAction())
                .entityName(event.getEntityName())
                .entityId(event.getEntityId())
                .outcome(event.getOutcome())
                .occurredAt(event.getOccurredAt())
                .source(event.getSource())
                .correlationId(event.getCorrelationId())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .reasonCode(event.getReasonCode())
                .reason(event.getReason())
                .beforeStateJson(writeMetadata(event.getBeforeState()))
                .afterStateJson(writeMetadata(event.getAfterState()))
                .metadataJson(writeMetadata(event.getMetadata()))
                .build();
    }

    private String writeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Los metadatos de auditoría no son serializables", ex);
        }
    }
}
