package co.edu.ufps.legal_cases.common.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import co.edu.ufps.legal_cases.common.exception.PerfilOperacionConflictException;
import co.edu.ufps.legal_cases.common.exception.dto.ErrorResponseDTO;
import co.edu.ufps.legal_cases.common.observability.CorrelationIdContext;

class GlobalExceptionHandlerPerfilOperacionTest {

    @Test
    void responde409ConCodigoFuncionalDiferenteDeConcurrencia() {
        String correlationId = "6402581d-520e-47f3-b390-e8dc88de1492";
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request =
                new MockHttpServletRequest("PATCH", "/api/estudiantes/10/activo");
        request.setAttribute(
                CorrelationIdContext.REQUEST_ATTRIBUTE,
                correlationId);

        ResponseEntity<ErrorResponseDTO> response =
                handler.manejarPerfilOperacionConflictException(
                        new PerfilOperacionConflictException(
                                "Operacion incompatible con el perfil actual"),
                        request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getEstado());
        assertEquals(
                "Conflicto de operacion de perfil",
                response.getBody().getError());
        assertEquals(
                "Operacion incompatible con el perfil actual",
                response.getBody().getMensaje());
        assertEquals(
                PerfilOperacionConflictException.CODIGO,
                response.getBody().getDetalles().get("codigo"));
        assertEquals(
                correlationId,
                response.getBody().getCorrelacionId());
        assertEquals(
                correlationId,
                response.getHeaders().getFirst(
                        CorrelationIdContext.HEADER_NAME));
    }
}
