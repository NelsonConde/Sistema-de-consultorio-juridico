package co.edu.ufps.legal_cases.common.exception.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
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
}