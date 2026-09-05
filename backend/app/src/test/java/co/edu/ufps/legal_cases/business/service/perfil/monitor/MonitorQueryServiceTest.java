package co.edu.ufps.legal_cases.business.service.perfil.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.business.dto.perfil.MonitorResumenDTO;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class MonitorQueryServiceTest {

    private MonitorRepository monitorRepository;
    private AsesorMonitorAccessService asesorMonitorAccessService;
    private MonitorMapper monitorMapper;
    private MonitorQueryService monitorQueryService;

    @BeforeEach
    void setUp() {
        monitorRepository = mock(MonitorRepository.class);
        monitorMapper = mock(MonitorMapper.class);
        asesorMonitorAccessService = mock(AsesorMonitorAccessService.class);

        monitorQueryService = new MonitorQueryService(
                monitorRepository,
                monitorMapper,
                asesorMonitorAccessService);
    }

    @Test
    void buscarDebeUsarDefaultsValidosYValidarAcceso() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(monitorRepository.buscarResumenPaginado(isNull(), isNull(), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        monitorQueryService.buscar(null, 1, 10, "id", "desc", null);

        verify(asesorMonitorAccessService).validarPuedeListarAsesoresYMonitores();
        verify(monitorRepository).buscarResumenPaginado(isNull(), isNull(), eq(esperado));
    }

    @Test
    void debeValidarPaginacionSearchSortYDirection() {
        assertThrows(BusinessException.class,
                () -> monitorQueryService.buscar(null, 0, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> monitorQueryService.buscar(null, 1, 0, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> monitorQueryService.buscar(null, 1, 51, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> monitorQueryService.buscar("x".repeat(101), 1, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> monitorQueryService.buscar(null, 1, 10, "telefono", "desc", null));
        assertThrows(BusinessException.class,
                () -> monitorQueryService.buscar(null, 1, 10, "id", "lateral", null));

        verify(monitorRepository, never()).buscarResumenPaginado(any(), any(), any(Pageable.class));
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        monitorQueryService.buscar("  monitor   turno  ", 1, 10, "id", "desc", null);
        monitorQueryService.buscar("   ", 1, 10, "id", "desc", null);

        verify(monitorRepository).buscarResumenPaginado(eq("monitor turno"), isNull(), any(Pageable.class));
        verify(monitorRepository).buscarResumenPaginado(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void debeConstruirSortValidoYDirectionAscDesc() {
        stubEmpty();

        monitorQueryService.buscar(null, 1, 10, "sedeNombre", "ASC", null);
        monitorQueryService.buscar(null, 1, 10, "codigo", "desc", null);

        List<PageRequest> pageables = capturarDosPageables();
        List<Sort.Order> sedeAsc = pageables.get(0).getSort().stream().toList();
        assertEquals("sede.nombre", sedeAsc.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, sedeAsc.get(0).getDirection());
        assertTrue(sedeAsc.get(0).isIgnoreCase());
        assertEquals("id", sedeAsc.get(1).getProperty());

        List<Sort.Order> codigoDesc = pageables.get(1).getSort().stream().toList();
        assertEquals("codigo", codigoDesc.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, codigoDesc.get(0).getDirection());
        assertEquals("id", codigoDesc.get(1).getProperty());
    }

    @Test
    void debePropagarActivoTrueYFalse() {
        stubEmpty();

        monitorQueryService.buscar(null, 1, 10, "id", "desc", true);
        monitorQueryService.buscar(null, 1, 10, "id", "desc", false);

        verify(monitorRepository).buscarResumenPaginado(isNull(), eq(true), any(Pageable.class));
        verify(monitorRepository).buscarResumenPaginado(isNull(), eq(false), any(Pageable.class));
    }

    @Test
    void debeConservarMetadataYMapearProjectionAResumen() {
        MonitorResumenProjection projection = mock(MonitorResumenProjection.class);
        MonitorResumenDTO dto = new MonitorResumenDTO();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));
        when(monitorRepository.buscarResumenPaginado(isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 21));
        when(monitorMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<MonitorResumenDTO> resultado =
                monitorQueryService.buscar(null, 2, 10, "id", "desc", null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void mapperDebeConvertirProjectionAResumen() {
        MonitorMapper mapper = new MonitorMapper();
        MonitorResumenProjection projection = mock(MonitorResumenProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getNombre()).thenReturn("Monitor A");
        when(projection.getDocumento()).thenReturn("DOC-1");
        when(projection.getEmail()).thenReturn("monitor@example.test");
        when(projection.getUsuario()).thenReturn("monitor.a");
        when(projection.getCodigo()).thenReturn("MON-1");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getSedeId()).thenReturn(2L);
        when(projection.getSedeNombre()).thenReturn("Principal");

        MonitorResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(1L, dto.getId());
        assertEquals("Monitor A", dto.getNombre());
        assertEquals("DOC-1", dto.getDocumento());
        assertEquals("monitor@example.test", dto.getEmail());
        assertEquals("monitor.a", dto.getUsuario());
        assertEquals("MON-1", dto.getCodigo());
        assertEquals(true, dto.getActivo());
        assertEquals(2L, dto.getSedeId());
        assertEquals("Principal", dto.getSedeNombre());
    }

    private void stubEmpty() {
        when(monitorRepository.buscarResumenPaginado(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private List<PageRequest> capturarDosPageables() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(monitorRepository, org.mockito.Mockito.times(2))
                .buscarResumenPaginado(any(), any(), captor.capture());
        return captor.getAllValues().stream()
                .map(PageRequest.class::cast)
                .toList();
    }
}
