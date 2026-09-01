package co.edu.ufps.legal_cases.audit.service.log;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditOutcome;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext.Snapshot;
import jakarta.servlet.http.HttpServletRequest;

/** Registra denegaciones producidas fuera de métodos anotados. */
@Service
public class AuditSecurityService {

    private static final Logger log = LoggerFactory.getLogger(AuditSecurityService.class);

    private final AuditLogService auditLogService;
    private final AuditRequestContext requestContext;

    public AuditSecurityService(AuditLogService auditLogService, AuditRequestContext requestContext) {
        this.auditLogService = auditLogService;
        this.requestContext = requestContext;
    }

    public void recordDenied(HttpServletRequest request, String action, String reasonCode) {
        if (request == null || Boolean.TRUE.equals(
                request.getAttribute(AuditRequestContext.DENIAL_RECORDED_ATTRIBUTE))) {
            return;
        }
        request.setAttribute(AuditRequestContext.DENIAL_RECORDED_ATTRIBUTE, Boolean.TRUE);
        Snapshot context = requestContext.capture(request);
        String target = limit(request.getMethod() + " " + request.getRequestURI(), 150);
        try {
            auditLogService.recordFailure(AuditEvent.builder()
                    .actorUsername(context.actorUsername())
                    .action(action)
                    .entityName("HttpEndpoint")
                    .entityId(target)
                    .outcome(AuditOutcome.DENIED)
                    .occurredAt(Instant.now())
                    .source(context.source())
                    .correlationId(context.correlationId())
                    .ipAddress(context.ipAddress())
                    .userAgent(context.userAgent())
                    .reasonCode(reasonCode)
                    .beforeState(Map.of())
                    .afterState(Map.of())
                    .metadata(Map.of())
                    .build());
        } catch (RuntimeException ex) {
            log.error("No fue posible persistir una denegación de acceso [{}]", context.correlationId(), ex);
        }
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
