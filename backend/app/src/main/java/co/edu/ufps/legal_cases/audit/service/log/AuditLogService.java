package co.edu.ufps.legal_cases.audit.service.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.ufps.legal_cases.audit.dto.log.AuditLogDTO;
import co.edu.ufps.legal_cases.audit.dto.log.AuditLogFilter;
import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditLog;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.repository.log.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import tools.jackson.databind.json.JsonMapper;

/** Coordina persistencia probatoria y consulta de la bitácora. */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;
    private final AuditLogWriter writer;
    private final JsonMapper objectMapper;

    public AuditLogService(
            AuditLogRepository repository,
            AuditLogWriter writer,
            JsonMapper objectMapper) {
        this.repository = repository;
        this.writer = writer;
        this.objectMapper = objectMapper;
    }

    /**
     * En una transacción de escritura, el evento participa en el mismo commit. Si
     * el commit termina en rollback, se registra de forma independiente el fallo.
     */
    public void recordSuccess(AuditEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            writer.persist(event);
            registerRollbackEvidence(event);
            return;
        }
        writer.persistIndependent(event);
    }

    /** Los fallos deben sobrevivir al rollback del caso de uso. */
    public void recordFailure(AuditEvent event) {
        writer.persistIndependent(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(AuditLogFilter filter, Pageable pageable) {
        return repository.findAll(specification(filter), pageable)
                .map(log -> AuditLogDTO.fromEntity(log, objectMapper));
    }

    private void registerRollbackEvidence(AuditEvent successEvent) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        writer.persistIndependent(successEvent.toBuilder()
                                .outcome(AuditOutcome.FAILURE)
                                .occurredAt(Instant.now())
                                .reasonCode("TRANSACTION_ROLLED_BACK")
                                .reason(null)
                                .build());
                    } catch (RuntimeException ex) {
                        log.error(
                                "No fue posible persistir el rollback de auditoría [{}]",
                                successEvent.getCorrelationId(),
                                ex);
                    }
                }
            }
        });
    }

    private Specification<AuditLog> specification(AuditLogFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return builder.conjunction();
            }
            if (StringUtils.hasText(filter.actorUsername())) {
                predicates.add(builder.like(
                        builder.lower(root.get("actorUsername")),
                        "%" + filter.actorUsername().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(filter.action())) {
                predicates.add(builder.equal(root.get("action"), filter.action().trim()));
            }
            if (StringUtils.hasText(filter.entityName())) {
                predicates.add(builder.equal(root.get("entityName"), filter.entityName().trim()));
            }
            if (filter.outcome() != null) {
                predicates.add(builder.equal(root.get("outcome"), filter.outcome()));
            }
            if (StringUtils.hasText(filter.correlationId())) {
                predicates.add(builder.equal(root.get("correlationId"), filter.correlationId().trim()));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), filter.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
