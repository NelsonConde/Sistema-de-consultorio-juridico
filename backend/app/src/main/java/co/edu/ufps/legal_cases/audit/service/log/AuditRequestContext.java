package co.edu.ufps.legal_cases.audit.service.log;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import co.edu.ufps.legal_cases.audit.model.log.AuditSource;
import jakarta.servlet.http.HttpServletRequest;

/** Captura únicamente datos de origen permitidos para la auditoría. */
@Component
public class AuditRequestContext {

    public static final String CORRELATION_ATTRIBUTE = "correlacionId";
    public static final String DENIAL_RECORDED_ATTRIBUTE = "auditDenialRecorded";

    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    public Snapshot capture() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !authentication.getName().isBlank()
                        ? limit(authentication.getName(), 150)
                        : "SYSTEM";

        ServletRequestAttributes attributes = currentRequestAttributes();
        if (attributes == null) {
            return new Snapshot(actor, AuditSource.SYSTEM, UUID.randomUUID().toString(), null, null);
        }
        return capture(attributes.getRequest(), actor);
    }

    public Snapshot capture(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !authentication.getName().isBlank()
                        ? limit(authentication.getName(), 150)
                        : "SYSTEM";
        return request == null
                ? new Snapshot(actor, AuditSource.SYSTEM, UUID.randomUUID().toString(), null, null)
                : capture(request, actor);
    }

    private Snapshot capture(HttpServletRequest request, String actor) {
        return new Snapshot(
                actor,
                AuditSource.HTTP,
                correlationId(request),
                limit(request.getRemoteAddr(), 45),
                sanitizeHeader(request.getHeader("User-Agent"), 512));
    }

    public HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = currentRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private ServletRequestAttributes currentRequestAttributes() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes
                : null;
    }

    private String correlationId(HttpServletRequest request) {
        Object current = request.getAttribute(CORRELATION_ATTRIBUTE);
        if (current != null && SAFE_CORRELATION_ID.matcher(current.toString()).matches()) {
            return current.toString();
        }

        String requested = request.getHeader("X-Request-ID");
        String correlationId = requested != null && SAFE_CORRELATION_ID.matcher(requested).matches()
                ? requested
                : UUID.randomUUID().toString();
        request.setAttribute(CORRELATION_ATTRIBUTE, correlationId);
        return correlationId;
    }

    private String sanitizeHeader(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return limit(value.replace('\r', ' ').replace('\n', ' ').trim(), maxLength);
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record Snapshot(
            String actorUsername,
            AuditSource source,
            String correlationId,
            String ipAddress,
            String userAgent) {
    }
}
