package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ConsultaResponsableOperacionServiceTest {

    private ConsultaRepository consultaRepository;
    private ConsultaResponsableOperacionService service;

    @BeforeEach
    void setUp() {
        consultaRepository = mock(ConsultaRepository.class);
        service = new ConsultaResponsableOperacionService(consultaRepository);
    }

    @Test
    void debeBloquearAsesorConConsultasOperativasDirectas() {
        when(consultaRepository.existsByAsesor_IdAndEstadoIn(eq(1L), anyList()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarAsesorSinConsultasOperativas(1L));

        assertEquals(
                "No se puede desactivar el asesor porque tiene consultas operativas asignadas o asociadas a sus estudiantes",
                exception.getMessage());
    }

    @Test
    void debeBloquearAsesorConConsultasOperativasPorEstudiantes() {
        when(consultaRepository.existsByEstudiante_Asesor_IdAndEstadoIn(eq(1L), anyList()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarAsesorSinConsultasOperativas(1L));

        assertEquals(
                "No se puede desactivar el asesor porque tiene consultas operativas asignadas o asociadas a sus estudiantes",
                exception.getMessage());
    }

    @Test
    void debePermitirAsesorSinConsultasOperativas() {
        assertDoesNotThrow(() -> service.validarAsesorSinConsultasOperativas(1L));
    }

    @Test
    void debeBloquearEstudianteConConsultasOperativas() {
        when(consultaRepository.existsByEstudiante_IdAndEstadoIn(eq(2L), anyList()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarEstudianteSinConsultasOperativas(2L));

        assertEquals(
                "No se puede desactivar el estudiante porque tiene consultas operativas asignadas",
                exception.getMessage());
    }

    @Test
    void debePermitirEstudianteSinConsultasOperativas() {
        assertDoesNotThrow(() -> service.validarEstudianteSinConsultasOperativas(2L));
    }

    @Test
    void debeBloquearMonitorConConsultasOperativas() {
        when(consultaRepository.existsByMonitor_IdAndEstadoIn(eq(3L), anyList()))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarMonitorSinConsultasOperativas(3L));

        assertEquals(
                "No se puede desactivar el monitor porque tiene consultas operativas asignadas",
                exception.getMessage());
    }

    @Test
    void debePermitirMonitorSinConsultasOperativas() {
        assertDoesNotThrow(() -> service.validarMonitorSinConsultasOperativas(3L));
    }

    @Test
    void debeRechazarAsesorIdNulo() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarAsesorSinConsultasOperativas(null));

        assertEquals("El id del asesor es obligatorio", exception.getMessage());
    }

    @Test
    void debeRechazarEstudianteIdNulo() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarEstudianteSinConsultasOperativas(null));

        assertEquals("El id del estudiante es obligatorio", exception.getMessage());
    }

    @Test
    void debeRechazarMonitorIdNulo() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.validarMonitorSinConsultasOperativas(null));

        assertEquals("El id del monitor es obligatorio", exception.getMessage());
    }
}