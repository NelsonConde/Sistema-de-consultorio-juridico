package co.edu.ufps.legal_cases.business.service.proceso.proceso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ProcesoValidatorTest {

    private ProcesoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProcesoValidator();
    }

    @Test
    void debePermitirRadicadoVacioCuandoProcesoEstaPendiente() {
        String resultado = validator.normalizarNumeroRadicadoParaEstado(null, EstadoProceso.PENDIENTE);

        assertNull(resultado);
    }

    @Test
    void debePermitirTextoVacioComoRadicadoNuloCuandoProcesoEstaPendiente() {
        String resultado = validator.normalizarNumeroRadicadoParaEstado("   ", EstadoProceso.PENDIENTE);

        assertNull(resultado);
    }

    @Test
    void debeRechazarEstadoFinalSinRadicado() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.normalizarNumeroRadicadoParaEstado(null, EstadoProceso.SENTENCIA_FAVORABLE));

        assertEquals("No se puede finalizar el proceso sin número de radicado", exception.getMessage());
    }

    @Test
    void debeRechazarRadicadoConLongitudInvalida() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.normalizarNumeroRadicadoParaEstado("12345", EstadoProceso.PENDIENTE));

        assertEquals("El número de radicado debe tener exactamente 23 caracteres", exception.getMessage());
    }

    @Test
    void debeAceptarRadicadoValido() {
        String resultado = validator.normalizarNumeroRadicadoParaEstado(
                "12345678901234567890123",
                EstadoProceso.SENTENCIA_FAVORABLE);

        assertEquals("12345678901234567890123", resultado);
    }
}