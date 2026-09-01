package co.edu.ufps.legal_cases.audit.aop.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.model.log.AuditSource;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext;

class AuditAspectTest {

    private AuditLogService logService;
    private AuditRequestContext requestContext;
    private AuditExpressionEvaluator evaluator;
    private AuditStateSnapshotService snapshotService;
    private AuditAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private Auditable auditable;

    @BeforeEach
    void setUp() throws Exception {
        logService = mock(AuditLogService.class);
        requestContext = mock(AuditRequestContext.class);
        evaluator = mock(AuditExpressionEvaluator.class);
        snapshotService = mock(AuditStateSnapshotService.class);
        aspect = new AuditAspect(logService, requestContext, evaluator, snapshotService);

        Method method = Fixture.class.getDeclaredMethod("execute", Long.class);
        auditable = method.getAnnotation(Auditable.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {7L});
        when(requestContext.capture()).thenReturn(new AuditRequestContext.Snapshot(
                "actor", AuditSource.HTTP, "correlation", "127.0.0.1", "agent"));
        when(evaluator.evaluateText(eq("#id"), any(), any(), any())).thenReturn("7");
        when(evaluator.evaluateMetadata(any(), any(), any(), any())).thenReturn(Map.of());
        when(snapshotService.captureEntity(anyString(), anyString(), any())).thenReturn(Map.of());
        when(snapshotService.captureResult(any(), any())).thenReturn(Map.of());
    }

    @Test
    void recordsSuccessfulOutcome() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.audit(joinPoint, auditable));

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(logService).recordSuccess(event.capture());
        assertEquals(AuditOutcome.SUCCESS, event.getValue().getOutcome());
        assertEquals("actor", event.getValue().getActorUsername());
        assertEquals("7", event.getValue().getEntityId());
    }

    @Test
    void recordsAuthorizationDenialWithoutSwallowingOriginalException() throws Throwable {
        AccessDeniedException denied = new AccessDeniedException("internal message");
        when(joinPoint.proceed()).thenThrow(denied);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(requestContext.currentRequest()).thenReturn(request);

        assertThrows(AccessDeniedException.class, () -> aspect.audit(joinPoint, auditable));

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(logService).recordFailure(event.capture());
        assertEquals(AuditOutcome.DENIED, event.getValue().getOutcome());
        assertEquals("AccessDeniedException", event.getValue().getReasonCode());
        assertEquals(null, event.getValue().getReason());
        assertEquals(Boolean.TRUE, request.getAttribute(AuditRequestContext.DENIAL_RECORDED_ATTRIBUTE));
    }

    static class Fixture {
        @Auditable(action = "TEST_ACTION", entityName = "TestEntity", entityId = "#id")
        String execute(Long id) {
            return id.toString();
        }
    }
}
