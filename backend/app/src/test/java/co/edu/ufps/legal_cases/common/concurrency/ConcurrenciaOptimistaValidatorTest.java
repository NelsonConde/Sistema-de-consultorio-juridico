package co.edu.ufps.legal_cases.common.concurrency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ConcurrenciaOptimistaException;

class ConcurrenciaOptimistaValidatorTest {

    private ConcurrenciaOptimistaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConcurrenciaOptimistaValidator();
    }

    @Test
    void permiteActualizarCuandoLaVersionCoincide() {
        assertDoesNotThrow(() ->
                validator.validarVersion(
                        4L,
                        4L,
                        "consulta"));
    }

    @Test
    void rechazaActualizarSinVersionEsperada() {
        assertThrows(
                BusinessException.class,
                () -> validator.validarVersion(
                        null,
                        4L,
                        "consulta"));
    }

    @Test
    void rechazaActualizarCuandoLaVersionEsAnterior() {
        assertThrows(
                ConcurrenciaOptimistaException.class,
                () -> validator.validarVersion(
                        3L,
                        4L,
                        "consulta"));
    }

    @Test
    void rechazaEnviarVersionEnCreacion() {
        assertThrows(
                BusinessException.class,
                () -> validator.validarVersionNoEnviadaEnCreacion(0L));
    }
}