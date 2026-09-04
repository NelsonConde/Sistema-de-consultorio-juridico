package co.edu.ufps.legal_cases.business.service.perfil.asesor;

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

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorResumenDTO;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class AsesorQueryServiceTest {

    private AsesorRepository asesorRepository;
    private AsesorMonitorAccessService asesorMonitorAccessService;
    private AsesorMapper asesorMapper;
    private AsesorQueryService asesorQueryService;

    @BeforeEach
    void setUp() {
        asesorRepository = mock(AsesorRepository.class);
        asesorMapper = mock(AsesorMapper.class);
        asesorMonitorAccessService = mock(AsesorMonitorAccessService.class);

        asesorQueryService = new AsesorQueryService(
                asesorRepository,
                asesorMapper,
                asesorMonitorAccessService);
    }

    @Test
    void buscarDebeUsarDefaultsValidosYValidarAcceso() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(asesorRepository.buscarResumenPaginado(isNull(), isNull(), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        asesorQueryService.buscar(null, 1, 10, "id", "desc", null);

        verify(asesorMonitorAccessService).validarPuedeListarAsesoresYMonitores();
        verify(asesorRepository).buscarResumenPaginado(isNull(), isNull(), eq(esperado));
    }

    @Test
    void debeValidarPaginacionSearchSortYDirection() {
        assertThrows(BusinessException.class,
                () -> asesorQueryService.buscar(null, 0, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> asesorQueryService.buscar(null, 1, 0, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> asesorQueryService.buscar(null, 1, 51, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> asesorQueryService.buscar("x".repeat(101), 1, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> asesorQueryService.buscar(null, 1, 10, "telefono", "desc", null));
        assertThrows(BusinessException.class,
                () -> asesorQueryService.buscar(null, 1, 10, "id", "lateral", null));

        verify(asesorRepository, never()).buscarResumenPaginado(any(), any(), any(Pageable.class));
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        asesorQueryService.buscar("  asesor   civil  ", 1, 10, "id", "desc", null);
        asesorQueryService.buscar("   ", 1, 10, "id", "desc", null);

        verify(asesorRepository).buscarResumenPaginado(eq("asesor civil"), isNull(), any(Pageable.class));
        verify(asesorRepository).buscarResumenPaginado(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void debeConstruirSortValidoYDirectionAscDesc() {
        stubEmpty();

        asesorQueryService.buscar(null, 1, 10, "areaNombre", "ASC", null);
        asesorQueryService.buscar(null, 1, 10, "codigo", "desc", null);

        List<PageRequest> pageables = capturarDosPageables();
        List<Sort.Order> areaAsc = pageables.get(0).getSort().stream().toList();
        assertEquals("area.nombre", areaAsc.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, areaAsc.get(0).getDirection());
        assertTrue(areaAsc.get(0).isIgnoreCase());
        assertEquals("id", areaAsc.get(1).getProperty());

        List<Sort.Order> codigoDesc = pageables.get(1).getSort().stream().toList();
        assertEquals("codigo", codigoDesc.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, codigoDesc.get(0).getDirection());
        assertEquals("id", codigoDesc.get(1).getProperty());
    }

    @Test
    void debePropagarActivoTrueYFalse() {
        stubEmpty();

        asesorQueryService.buscar(null, 1, 10, "id", "desc", true);
        asesorQueryService.buscar(null, 1, 10, "id", "desc", false);

        verify(asesorRepository).buscarResumenPaginado(isNull(), eq(true), any(Pageable.class));
        verify(asesorRepository).buscarResumenPaginado(isNull(), eq(false), any(Pageable.class));
    }

    @Test
    void debeConservarMetadataYMapearProjectionAResumen() {
        AsesorResumenProjection projection = mock(AsesorResumenProjection.class);
        AsesorResumenDTO dto = new AsesorResumenDTO();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));
        when(asesorRepository.buscarResumenPaginado(isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 21));
        when(asesorMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<AsesorResumenDTO> resultado =
                asesorQueryService.buscar(null, 2, 10, "id", "desc", null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void mapperDebeConvertirProjectionAResumen() {
        AsesorMapper mapper = new AsesorMapper();
        AsesorResumenProjection projection = mock(AsesorResumenProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getNombre()).thenReturn("Asesor A");
        when(projection.getDocumento()).thenReturn("DOC-1");
        when(projection.getEmail()).thenReturn("asesor@example.test");
        when(projection.getUsuario()).thenReturn("asesor.a");
        when(projection.getCodigo()).thenReturn("ASE-1");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getAreaId()).thenReturn(2L);
        when(projection.getAreaNombre()).thenReturn("Civil");
        when(projection.getSedeId()).thenReturn(3L);
        when(projection.getSedeNombre()).thenReturn("Principal");

        AsesorResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(1L, dto.getId());
        assertEquals("Asesor A", dto.getNombre());
        assertEquals("DOC-1", dto.getDocumento());
        assertEquals("asesor@example.test", dto.getEmail());
        assertEquals("asesor.a", dto.getUsuario());
        assertEquals("ASE-1", dto.getCodigo());
        assertEquals(true, dto.getActivo());
        assertEquals(2L, dto.getAreaId());
        assertEquals("Civil", dto.getAreaNombre());
        assertEquals(3L, dto.getSedeId());
        assertEquals("Principal", dto.getSedeNombre());
    }

    private void stubEmpty() {
        when(asesorRepository.buscarResumenPaginado(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private List<PageRequest> capturarDosPageables() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(asesorRepository, org.mockito.Mockito.times(2))
                .buscarResumenPaginado(any(), any(), captor.capture());
        return captor.getAllValues().stream()
                .map(PageRequest.class::cast)
                .toList();
    }
}
