package co.edu.ufps.legal_cases.business.service.estadisticas.estadisticas;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class EstadisticasRangoQueryServiceTest {

    private ConsultaRepository consultaRepository;
    private ProcesoRepository procesoRepository;
    private ConciliacionRepository conciliacionRepository;
    private SeguimientoRepository seguimientoRepository;
    private EstudianteRepository estudianteRepository;
    private EstadisticasMapperService mapper;
    private EstadisticasRangoQueryService service;

    @BeforeEach
    void setUp() {
        consultaRepository = mock(ConsultaRepository.class);
        procesoRepository = mock(ProcesoRepository.class);
        conciliacionRepository = mock(ConciliacionRepository.class);
        seguimientoRepository = mock(SeguimientoRepository.class);
        estudianteRepository = mock(EstudianteRepository.class);
        mapper = mock(EstadisticasMapperService.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-08-30T12:00:00Z"),
                ZoneId.of("America/Bogota"));

        service = new EstadisticasRangoQueryService(
                consultaRepository,
                procesoRepository,
                conciliacionRepository,
                seguimientoRepository,
                estudianteRepository,
                mapper,
                clock);
    }

    @Test
    void debeRechazarFechaFinFuturaAntesDeConsultarRepositorios() {
        assertThrows(
                BusinessException.class,
                () -> service.obtenerPorRango(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31)));

        verificarRepositoriosSinInteracciones();
    }

    @Test
    void debeRechazarFechaInicioPosteriorALaFechaFin() {
        assertThrows(
                BusinessException.class,
                () -> service.obtenerPorRango(
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 10)));

        verificarRepositoriosSinInteracciones();
    }

    private void verificarRepositoriosSinInteracciones() {
        verifyNoInteractions(
                consultaRepository,
                procesoRepository,
                conciliacionRepository,
                seguimientoRepository,
                estudianteRepository,
                mapper);
    }
}
