package co.edu.ufps.legal_cases.audit.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.model.log.AuditSource;
import co.edu.ufps.legal_cases.audit.repository.log.AuditLogRepository;
import tools.jackson.databind.json.JsonMapper;

class AuditLogServiceTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditLogWriter writer = mock(AuditLogWriter.class);
    private final AuditLogService service = new AuditLogService(repository, writer, JsonMapper.shared());

    @AfterEach
    void cleanTransactionContext() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    @Test
    void joinsWriteTransactionAndCreatesIndependentEvidenceOnRollback() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        TransactionSynchronizationManager.initSynchronization();
        AuditEvent event = event(AuditOutcome.SUCCESS);

        service.recordSuccess(event);

        verify(writer).persist(event);
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        TransactionSynchronization synchronization =
                TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(writer).persistIndependent(argThat(rollback ->
                rollback.getOutcome() == AuditOutcome.FAILURE
                        && "TRANSACTION_ROLLED_BACK".equals(rollback.getReasonCode())));
    }

    @Test
    void persistsReadEvidenceOutsideReadOnlyTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        AuditEvent event = event(AuditOutcome.SUCCESS);

        service.recordSuccess(event);

        verify(writer).persistIndependent(event);
    }

    @Test
    void persistsFailureIndependently() {
        AuditEvent event = event(AuditOutcome.DENIED);

        service.recordFailure(event);

        verify(writer).persistIndependent(event);
    }

    private AuditEvent event(AuditOutcome outcome) {
        return AuditEvent.builder()
                .actorUsername("actor")
                .action("ACTION")
                .entityName("Entity")
                .entityId("1")
                .outcome(outcome)
                .occurredAt(Instant.now())
                .source(AuditSource.SYSTEM)
                .correlationId("correlation")
                .beforeState(Map.of())
                .afterState(Map.of())
                .metadata(Map.of())
                .build();
    }
}
