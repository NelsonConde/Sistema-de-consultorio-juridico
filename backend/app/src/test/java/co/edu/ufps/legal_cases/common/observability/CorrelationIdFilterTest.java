package co.edu.ufps.legal_cases.common.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void shouldGenerateUuidWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String correlationId = response.getHeader(CorrelationIdContext.HEADER_NAME);

        assertNotNull(correlationId);
        assertEquals(correlationId, request.getAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE));
        assertEquals(correlationId, UUID.fromString(correlationId).toString());
        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }

    @Test
    void shouldKeepValidUuidHeaderAndNormalizeIt() throws Exception {
        String incoming =
                "A0EBC999-9C0B-4EF8-BB6D-6BB9BD380A11";
        String normalized =
                UUID.fromString(incoming).toString();

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdContext.HEADER_NAME, incoming);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(normalized, response.getHeader(CorrelationIdContext.HEADER_NAME));
        assertEquals(normalized, request.getAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE));
        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }

    @Test
    void shouldGenerateUuidWhenHeaderIsInvalid() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdContext.HEADER_NAME, "texto-no-confiable");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(CorrelationIdContext.HEADER_NAME);

        assertNotNull(generated);
        assertNotEquals("texto-no-confiable", generated);
        assertEquals(generated, UUID.fromString(generated).toString());
        assertEquals(generated, request.getAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE));
    }

    @Test
    void shouldExposeCorrelationIdInMdcWhileChainRuns() throws Exception {
        String incoming =
                UUID.randomUUID().toString();
        AtomicReference<String> mdcValue =
                new AtomicReference<>();

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdContext.HEADER_NAME, incoming);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (servletRequest, servletResponse) ->
                        mdcValue.set(MDC.get(CorrelationIdContext.MDC_KEY)));

        assertEquals(incoming, mdcValue.get());
        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }

    @Test
    void shouldCleanMdcWhenChainThrowsException() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        assertThrows(
                ServletException.class,
                () -> filter.doFilter(
                        request,
                        response,
                        (servletRequest, servletResponse) -> {
                            throw new ServletException("fallo controlado");
                        }));

        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }

    @Test
    void shouldCleanMdcWhenChainThrowsIOException() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        assertThrows(
                IOException.class,
                () -> filter.doFilter(
                        request,
                        response,
                        (servletRequest, servletResponse) -> {
                            throw new IOException("fallo io");
                        }));

        assertNull(MDC.get(CorrelationIdContext.MDC_KEY));
    }
}
