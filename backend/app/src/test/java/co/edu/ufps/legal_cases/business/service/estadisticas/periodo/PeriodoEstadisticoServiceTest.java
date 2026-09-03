package co.edu.ufps.legal_cases.business.service.estadisticas.periodo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;

class PeriodoEstadisticoServiceTest {

    private PeriodoEstadisticoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-30T12:00:00Z"),
                ZoneId.of("America/Bogota"));

        service = new PeriodoEstadisticoService(clock);
    }

    @Test
    void debeConstruirPrimerSemestreConLimitesFijos() {
        PeriodoEstadistico periodo =
                service.obtener(2026, 1);

        assertEquals(
                LocalDate.of(2026, 1, 1),
                periodo.inicio());

        assertEquals(
                LocalDate.of(2026, 6, 30),
                periodo.fin());
    }

    @Test
    void debeConstruirSegundoSemestreConLimitesFijos() {
        PeriodoEstadistico periodo =
                service.obtener(2026, 2);

        assertEquals(
                LocalDate.of(2026, 7, 1),
                periodo.inicio());

        assertEquals(
                LocalDate.of(2026, 12, 31),
                periodo.fin());
    }

    @Test
    void debeRechazarSemestreFueraDelDominio() {
        assertThrows(
                BusinessException.class,
                () -> service.obtener(2026, 3));
    }

    @Test
    void debeRechazarAñoAnteriorAlInicioDeDatos() {
        assertThrows(
                BusinessException.class,
                () -> service.obtener(2023, 2));
    }

    @Test
    void debeRechazarPeriodoQueAunNoHaComenzado() {
        assertThrows(
                BusinessException.class,
                () -> service.obtener(2027, 1));
    }

    @Test
    void debeListarSoloPeriodosQueYaComenzaron() {
        List<PeriodoEstadistico> periodos =
                service.listarDisponibles();

        assertEquals(6, periodos.size());

        assertEquals(
                new PeriodoEstadistico(
                        2024,
                        1,
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30)),
                periodos.getFirst());

        assertEquals(
                new PeriodoEstadistico(
                        2026,
                        2,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 12, 31)),
                periodos.getLast());
    }
}