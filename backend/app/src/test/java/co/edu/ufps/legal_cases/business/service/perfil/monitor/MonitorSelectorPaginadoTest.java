package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorResumenDTO;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;

class MonitorSelectorPaginadoTest {

    private MonitorRepository monitorRepository;
    private AsesorMonitorAccessService accessService;
    private MonitorQueryService queryService;

    @BeforeEach
    void setUp() {
        monitorRepository = mock(MonitorRepository.class);
        accessService = mock(AsesorMonitorAccessService.class);
        queryService = new MonitorQueryService(monitorRepository, mock(MonitorMapper.class), accessService);
    }

    @Test
    void debeBuscarYPaginarSoloMonitoresActivos() {
        PageRequest interno = PageRequest.of(
                2,
                5,
                Sort.by(Sort.Order.desc("m.codigo").ignoreCase(), Sort.Order.asc("m.id")));
        when(monitorRepository.buscarResumenPaginado("M-10", true, interno))
                .thenReturn(new PageImpl<>(List.of(), interno, 11));

        PageResponseDTO<MonitorResumenDTO> resultado =
                queryService.listarActivosPaginados(" M-10 ", 3, 5, "codigo", "desc");

        assertEquals(List.of(), resultado.content());
        assertEquals(3, resultado.page());
        assertEquals(5, resultado.size());
        assertEquals(11, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(accessService).validarPuedeListarAsesoresYMonitoresActivos();
        verify(monitorRepository).buscarResumenPaginado("M-10", true, interno);
    }

    @Test
    void noDebeConsultarSinAutorizacion() {
        doThrow(new AccessDeniedException("denegado"))
                .when(accessService)
                .validarPuedeListarAsesoresYMonitoresActivos();

        assertThrows(
                AccessDeniedException.class,
                () -> queryService.listarActivosPaginados(null, 1, 10, "nombre", "asc"));

        verify(monitorRepository, never()).buscarResumenPaginado(any(), eq(true), any());
    }
}
