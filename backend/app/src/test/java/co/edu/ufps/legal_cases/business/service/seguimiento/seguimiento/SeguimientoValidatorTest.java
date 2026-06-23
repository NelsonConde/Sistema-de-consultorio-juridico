package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class SeguimientoValidatorTest {

    private SeguimientoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SeguimientoValidator();
    }

    @Test
    void debePermitirNoNotificarEstudianteSinConsulta() {
        assertDoesNotThrow(() -> validator.validarNotificarEstudianteConConsulta(false, null));
    }

    @Test
    void debeRechazarNotificarEstudianteSinConsulta() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarNotificarEstudianteConConsulta(true, null));

        assertEquals(
                "No se puede notificar al estudiante porque la consulta es obligatoria",
                exception.getMessage());
    }

    @Test
    void debeRechazarNotificarEstudianteSinEstudianteAsignado() {
        Consulta consulta = new Consulta();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarNotificarEstudianteConConsulta(true, consulta));

        assertEquals(
                "No se puede notificar al estudiante porque la consulta no tiene estudiante asignado",
                exception.getMessage());
    }

    @Test
    void debeRechazarNotificarEstudianteConEstudianteInactivo() {
        Consulta consulta = new Consulta();
        Estudiante estudiante = new Estudiante();
        estudiante.setActivo(false);
        consulta.setEstudiante(estudiante);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.validarNotificarEstudianteConConsulta(true, consulta));

        assertEquals(
                "No se puede notificar al estudiante porque el estudiante asignado está inactivo",
                exception.getMessage());
    }

    @Test
    void debePermitirNotificarEstudianteConEstudianteActivo() {
        Consulta consulta = new Consulta();
        Estudiante estudiante = new Estudiante();
        estudiante.setActivo(true);
        consulta.setEstudiante(estudiante);

        assertDoesNotThrow(() -> validator.validarNotificarEstudianteConConsulta(true, consulta));
    }
}