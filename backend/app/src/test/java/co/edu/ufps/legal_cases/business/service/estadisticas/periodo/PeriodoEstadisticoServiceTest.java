package co.edu.ufps.legal_cases.business.service.estadisticas.periodo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import co.edu.ufps.legal_cases.business.model.estadisticas.PeriodoAcademico;
import co.edu.ufps.legal_cases.business.repository.estadisticas.PeriodoAcademicoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class PeriodoEstadisticoServiceTest {

    @Mock
    private PeriodoAcademicoRepository periodoAcademicoRepository;

    private PeriodoEstadisticoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Clock clock = Clock.fixed(
                Instant.parse("2026-08-30T12:00:00Z"),
                ZoneId.of("America/Bogota"));

        service = new PeriodoEstadisticoService(
                periodoAcademicoRepository,
                clock);
    }

    @Test
    void debeUsarLasFechasConfiguradasDelPeriodoAcademico() {
        PeriodoAcademico periodoAcademico =
                periodo(
                        2026,
                        2,
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 12, 11));

        when(periodoAcademicoRepository
                .findByAnioAndSemestreAndActivoTrue(2026, 2))
                .thenReturn(Optional.of(periodoAcademico));

        PeriodoEstadistico periodo =
                service.obtener(2026, 2);

        assertEquals(2026, periodo.año());
        assertEquals(2, periodo.semestre());
        assertEquals(
                LocalDate.of(2026, 8, 3),
                periodo.inicio());
        assertEquals(
                LocalDate.of(2026, 12, 11),
                periodo.fin());
    }

    @Test
    void debeRechazarSemestreFueraDelDominio() {
        assertThrows(
                BusinessException.class,
                () -> service.obtener(2026, 3));
    }

    @Test
    void debeRechazarPeriodoAcademicoInexistente() {
        when(periodoAcademicoRepository
                .findByAnioAndSemestreAndActivoTrue(2026, 1))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> service.obtener(2026, 1));
    }

    @Test
    void debeRechazarPeriodoQueAunNoHaComenzado() {
        PeriodoAcademico periodoAcademico =
                periodo(
                        2027,
                        1,
                        LocalDate.of(2027, 1, 15),
                        LocalDate.of(2027, 6, 15));

        when(periodoAcademicoRepository
                .findByAnioAndSemestreAndActivoTrue(2027, 1))
                .thenReturn(Optional.of(periodoAcademico));

        assertThrows(
                BusinessException.class,
                () -> service.obtener(2027, 1));
    }

    @Test
    void debeRechazarPeriodoConFechasInvertidas() {
        PeriodoAcademico periodoAcademico =
                periodo(
                        2026,
                        2,
                        LocalDate.of(2026, 12, 11),
                        LocalDate.of(2026, 8, 3));

        when(periodoAcademicoRepository
                .findByAnioAndSemestreAndActivoTrue(2026, 2))
                .thenReturn(Optional.of(periodoAcademico));

        assertThrows(
                BusinessException.class,
                () -> service.obtener(2026, 2));
    }

    @Test
    void debeListarSoloPeriodosActivosQueYaComenzaron() {
        PeriodoAcademico periodo2024 =
                periodo(
                        2024,
                        1,
                        LocalDate.of(2024, 2, 1),
                        LocalDate.of(2024, 6, 20));

        PeriodoAcademico periodoActual =
                periodo(
                        2026,
                        2,
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 12, 11));

        PeriodoAcademico periodoFuturo =
                periodo(
                        2027,
                        1,
                        LocalDate.of(2027, 1, 15),
                        LocalDate.of(2027, 6, 15));

        when(periodoAcademicoRepository
                .findByActivoTrueOrderByAnioAscSemestreAsc())
                .thenReturn(List.of(
                        periodo2024,
                        periodoActual,
                        periodoFuturo));

        List<PeriodoEstadistico> periodos =
                service.listarDisponibles();

        assertEquals(2, periodos.size());

        assertEquals(
                LocalDate.of(2024, 2, 1),
                periodos.getFirst().inicio());

        assertEquals(
                LocalDate.of(2026, 8, 3),
                periodos.getLast().inicio());
    }

    private PeriodoAcademico periodo(
            int anio,
            int semestre,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        PeriodoAcademico periodoAcademico =
                new PeriodoAcademico();

        periodoAcademico.setAnio(anio);
        periodoAcademico.setSemestre(semestre);
        periodoAcademico.setFechaInicio(fechaInicio);
        periodoAcademico.setFechaFin(fechaFin);
        periodoAcademico.setActivo(true);

        return periodoAcademico;
    }
}