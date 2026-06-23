package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaDecisionDTO;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class SeguimientoRespuestaValidatorTest {

    private SeguimientoRespuestaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SeguimientoRespuestaValidator();
    }

    @Test
    void debeRechazarRespuestaRechazadaSinObservacion() {
        SeguimientoRespuestaDecisionDTO dto = new SeguimientoRespuestaDecisionDTO();
        dto.setEstado(EstadoRespuestaSeguimiento.RECHAZADA);
        dto.setObservacionRevision("   ");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarDecision(dto));

        assertEquals(
                "La observación de revisión es obligatoria al rechazar una respuesta",
                exception.getMessage());
    }

    @Test
    void debePermitirRespuestaRechazadaConObservacion() {
        SeguimientoRespuestaDecisionDTO dto = new SeguimientoRespuestaDecisionDTO();
        dto.setEstado(EstadoRespuestaSeguimiento.RECHAZADA);
        dto.setObservacionRevision("Debe complementar la respuesta con los documentos solicitados.");

        assertDoesNotThrow(() -> validator.validarDecision(dto));
    }

    @Test
    void debePermitirRespuestaAprobadaSinObservacion() {
        SeguimientoRespuestaDecisionDTO dto = new SeguimientoRespuestaDecisionDTO();
        dto.setEstado(EstadoRespuestaSeguimiento.APROBADA);
        dto.setObservacionRevision("");

        assertDoesNotThrow(() -> validator.validarDecision(dto));
    }

    @Test
    void debeRechazarDecisionPendiente() {
        SeguimientoRespuestaDecisionDTO dto = new SeguimientoRespuestaDecisionDTO();
        dto.setEstado(EstadoRespuestaSeguimiento.PENDIENTE);
        dto.setObservacionRevision("No debe permitir PENDIENTE como decisión.");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarDecision(dto));

        assertEquals("La decisión debe ser APROBADA o RECHAZADA", exception.getMessage());
    }

    @Test
    void debeRechazarObservacionMayorAQuinientosCaracteres() {
        SeguimientoRespuestaDecisionDTO dto = new SeguimientoRespuestaDecisionDTO();
        dto.setEstado(EstadoRespuestaSeguimiento.APROBADA);
        dto.setObservacionRevision("a".repeat(501));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarDecision(dto));

        assertEquals("La observación de revisión no puede superar 500 caracteres", exception.getMessage());
    }
}