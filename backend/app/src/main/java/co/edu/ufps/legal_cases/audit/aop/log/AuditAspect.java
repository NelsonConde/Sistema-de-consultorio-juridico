package co.edu.ufps.legal_cases.audit.aop.log;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext.Snapshot;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Convierte una operación anotada en un evento estructurado. Nunca serializa los
 * argumentos completos ni usa la representación {@code toString()} de objetos.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final AuditRequestContext requestContext;
    private final AuditExpressionEvaluator expressionEvaluator;
    private final AuditStateSnapshotService snapshotService;

    public AuditAspect(
            AuditLogService auditLogService,
            AuditRequestContext requestContext,
            AuditExpressionEvaluator expressionEvaluator,
            AuditStateSnapshotService snapshotService) {
        this.auditLogService = auditLogService;
        this.requestContext = requestContext;
        this.expressionEvaluator = expressionEvaluator;
        this.snapshotService = snapshotService;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] arguments = joinPoint.getArgs();
        Snapshot context = requestContext.capture();
        String attemptedEntityId = evaluateOptional(auditable.entityId(), method, arguments, null);
        Map<String, String> beforeState = snapshotService.captureEntity(
                auditable.entityName(), attemptedEntityId, auditable.trackedFields());

        try {
            Object result = joinPoint.proceed();
            String entityId = evaluateOptional(auditable.entityId(), method, arguments, result);
            if (entityId == null) {
                entityId = attemptedEntityId;
            }
            Map<String, String> afterState = snapshotService.captureResult(result, auditable.trackedFields());
            if (afterState.isEmpty()) {
                afterState = snapshotService.captureEntity(
                        auditable.entityName(), entityId, auditable.trackedFields());
            }
            AuditEvent event = buildEvent(
                    auditable,
                    method,
                    arguments,
                    result,
                    context,
                    entityId,
                    beforeState,
                    afterState,
                    AuditOutcome.SUCCESS,
                    null);
            auditLogService.recordSuccess(event);
            return result;
        } catch (Throwable original) {
            AuditOutcome outcome = original instanceof AccessDeniedException
                    ? AuditOutcome.DENIED
                    : AuditOutcome.FAILURE;
            if (outcome == AuditOutcome.DENIED) {
                markDenialRecorded();
            }
            try {
                auditLogService.recordFailure(buildEvent(
                        auditable,
                        method,
                        arguments,
                        null,
                        context,
                        attemptedEntityId,
                        beforeState,
                        Map.of(),
                        outcome,
                        original.getClass().getSimpleName()));
            } catch (RuntimeException auditFailure) {
                original.addSuppressed(auditFailure);
            }
            throw original;
        }
    }

    private AuditEvent buildEvent(
            Auditable auditable,
            Method method,
            Object[] arguments,
            Object result,
            Snapshot context,
            String entityId,
            Map<String, String> beforeState,
            Map<String, String> afterState,
            AuditOutcome outcome,
            String reasonCode) {
        String reason = evaluateOptional(auditable.reason(), method, arguments, result);
        Map<String, String> metadata = expressionEvaluator.evaluateMetadata(
                auditable.metadata(), method, arguments, result);

        return AuditEvent.builder()
                .actorUsername(context.actorUsername())
                .action(auditable.action())
                .entityName(auditable.entityName())
                .entityId(entityId)
                .outcome(outcome)
                .occurredAt(Instant.now())
                .source(context.source())
                .correlationId(context.correlationId())
                .ipAddress(context.ipAddress())
                .userAgent(context.userAgent())
                .reasonCode(reasonCode)
                .reason(reason)
                .beforeState(beforeState)
                .afterState(afterState)
                .metadata(metadata)
                .build();
    }

    private String evaluateOptional(
            String expression,
            Method method,
            Object[] arguments,
            Object result) {
        try {
            return expressionEvaluator.evaluateText(expression, method, arguments, result);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void markDenialRecorded() {
        HttpServletRequest request = requestContext.currentRequest();
        if (request != null) {
            request.setAttribute(AuditRequestContext.DENIAL_RECORDED_ATTRIBUTE, Boolean.TRUE);
        }
    }
}
