package co.edu.ufps.legal_cases.common.exception.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import co.edu.ufps.legal_cases.common.observability.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerInvariantTest {

    @Test
    void respondeConflictConContratoExistente() {
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/roles/1");

        ResponseEntity<ErrorResponseDTO> response =
                handler.manejarAdministracionInvariantException(
                        new AdministracionInvariantException(
                                "La operación dejaría el sistema sin administradores operativos"),
                        request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getEstado());
        assertEquals(
                "Conflicto de administración",
                response.getBody().getError());
        assertEquals(
                "/api/roles/1",
                response.getBody().getRuta());
    }

    @Test
    void usaCorrelationIdTransversalEnBodyYHeader() {
        String correlationId =
                "6d1966e2-830b-439c-a9f7-7d5d28c56e93";
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/roles/1");
        request.setAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE, correlationId);

        ResponseEntity<ErrorResponseDTO> response =
                handler.manejarAdministracionInvariantException(
                        new AdministracionInvariantException("Conflicto controlado"),
                        request);

        assertNotNull(response.getBody());
        assertEquals(correlationId, response.getBody().getCorrelacionId());
        assertEquals(correlationId, response.getHeaders().getFirst(CorrelationIdContext.HEADER_NAME));
    }
}
