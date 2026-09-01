package co.edu.ufps.legal_cases.common.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerOptimisticLockTest {

    @Test
    void responde409AnteConflictoOptimistaDeJpa() {
        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();

        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/procesos/1");

        ResponseEntity<ErrorResponseDTO> response =
                handler.manejarConflictoConcurrencia(
                        new OptimisticLockException(
                                "detalle interno que no debe exponerse"),
                        request);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                409,
                response.getBody().getEstado());

        assertEquals(
                "Conflicto de concurrencia",
                response.getBody().getError());

        assertEquals(
                "El recurso fue modificado por otro usuario. "
                        + "Recargue la información y revise sus cambios.",
                response.getBody().getMensaje());

        assertEquals(
                "/api/procesos/1",
                response.getBody().getRuta());
    }
}
