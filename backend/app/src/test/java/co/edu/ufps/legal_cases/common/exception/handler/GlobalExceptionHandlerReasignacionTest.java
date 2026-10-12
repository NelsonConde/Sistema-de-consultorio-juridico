package co.edu.ufps.legal_cases.common.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import co.edu.ufps.legal_cases.common.exception.ConsultaReasignacionNoDisponibleException;
import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerReasignacionTest {

    @Test
    void respondeServiceUnavailableCuandoElServicioNoEstaIntegrado() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/consultas/10/responsable");

        var response = handler.manejarReasignacionNoDisponible(
                new ConsultaReasignacionNoDisponibleException(),
                request);

        ErrorResponseDTO body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, body.getEstado());
        assertEquals("Reasignacion no disponible", body.getError());
        assertEquals("La reasignacion no puede completarse en este momento", body.getMensaje());
    }
}
