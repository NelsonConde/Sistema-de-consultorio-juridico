package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.AlcanceAlertasDisciplinarias;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;

class SeguimientoQueryServiceTest {

    private SeguimientoRepository seguimientoRepository;
    private SeguimientoAccessService seguimientoAccessService;
    private SeguimientoMapper seguimientoMapper;
    private SeguimientoValidator seguimientoValidator;
    private SeguimientoQueryService seguimientoQueryService;

    @BeforeEach
    void setUp() {
        seguimientoRepository = mock(SeguimientoRepository.class);
        seguimientoAccessService = mock(SeguimientoAccessService.class);
        seguimientoMapper = mock(SeguimientoMapper.class);
        seguimientoValidator = mock(SeguimientoValidator.class);

        seguimientoQueryService = new SeguimientoQueryService(
                seguimientoRepository,
                seguimientoAccessService,
                seguimientoMapper,
                seguimientoValidator);
    }

    @Test
    void debeUsarConsultaGlobalParaAdministrador() {
        when(seguimientoAccessService.resolverAlcanceAlertasDisciplinarias())
                .thenReturn(AlcanceAlertasDisciplinarias.global());

        when(seguimientoRepository
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarAlertasDisciplinarias();

        verify(seguimientoRepository)
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByAsesorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByMonitorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debeUsarConsultaRestringidaParaAsesor() {
        when(seguimientoAccessService.resolverAlcanceAlertasDisciplinarias())
                .thenReturn(AlcanceAlertasDisciplinarias.asesor(10L));

        when(seguimientoRepository.findAlertasDisciplinariasByAsesorScope(
                10L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarAlertasDisciplinarias();

        verify(seguimientoRepository)
                .findAlertasDisciplinariasByAsesorScope(
                        10L,
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByMonitorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debeUsarConsultaRestringidaParaMonitor() {
        when(seguimientoAccessService.resolverAlcanceAlertasDisciplinarias())
                .thenReturn(AlcanceAlertasDisciplinarias.monitor(20L));

        when(seguimientoRepository.findAlertasDisciplinariasByMonitorScope(
                20L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarAlertasDisciplinarias();

        verify(seguimientoRepository)
                .findAlertasDisciplinariasByMonitorScope(
                        20L,
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByAsesorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }
}