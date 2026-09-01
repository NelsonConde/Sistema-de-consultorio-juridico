package co.edu.ufps.legal_cases.audit.dto.log;

import java.time.Instant;

import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;

/** Filtros permitidos para consultar la bitácora probatoria. */
public record AuditLogFilter(
        String actorUsername,
        String action,
        String entityName,
        AuditOutcome outcome,
        String correlationId,
        Instant from,
        Instant to) {
}
