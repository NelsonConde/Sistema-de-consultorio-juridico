package co.edu.ufps.legal_cases.common.observability;

import java.util.UUID;

import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;

public final class CorrelationIdContext {

    public static final String HEADER_NAME = "X-Request-ID";
    public static final String REQUEST_ATTRIBUTE = "correlacionId";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIdContext() {
    }

    public static String resolve(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return generate();
        }

        try {
            return UUID.fromString(headerValue.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return generate();
        }
    }

    public static String getOrCreate(HttpServletRequest request) {
        Object current = request.getAttribute(REQUEST_ATTRIBUTE);
        if (current instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }

        String fromMdc = MDC.get(MDC_KEY);
        if (fromMdc != null && !fromMdc.isBlank()) {
            request.setAttribute(REQUEST_ATTRIBUTE, fromMdc);
            return fromMdc;
        }

        String generated = generate();
        request.setAttribute(REQUEST_ATTRIBUTE, generated);
        return generated;
    }

    public static void bind(HttpServletRequest request, String correlationId) {
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        MDC.put(MDC_KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    private static String generate() {
        return UUID.randomUUID().toString();
    }
}
