package co.edu.ufps.legal_cases.business.service.perfil.conciliador;

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

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorResumenDTO;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.ConciliadorAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class ConciliadorQueryServiceTest {

    private ConciliadorRepository conciliadorRepository;
    private ConciliadorAccessService conciliadorAccessService;
    private ConciliadorMapper conciliadorMapper;
    private ConciliadorQueryService conciliadorQueryService;

    @BeforeEach
    void setUp() {
        conciliadorRepository = mock(ConciliadorRepository.class);
        conciliadorAccessService = mock(ConciliadorAccessService.class);
        conciliadorMapper = mock(ConciliadorMapper.class);

        conciliadorQueryService = new ConciliadorQueryService(
                conciliadorRepository,
                conciliadorMapper,
                conciliadorAccessService);
    }

    @Test
    void buscarDebeUsarDefaultsValidosYValidarAcceso() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(conciliadorRepository.buscarResumenPaginado(isNull(), isNull(), isNull(), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        conciliadorQueryService.buscar(null, 1, 10, "id", "desc", null, null);

        verify(conciliadorAccessService).validarPuedeListarConciliadores();
        verify(conciliadorRepository).buscarResumenPaginado(isNull(), isNull(), isNull(), eq(esperado));
    }

    @Test
    void debeValidarPaginacionSearchSortYDirection() {
        assertThrows(BusinessException.class,
                () -> conciliadorQueryService.buscar(null, 0, 10, "id", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> conciliadorQueryService.buscar(null, 1, 0, "id", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> conciliadorQueryService.buscar(null, 1, 51, "id", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> conciliadorQueryService.buscar("x".repeat(101), 1, 10, "id", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> conciliadorQueryService.buscar(null, 1, 10, "telefono", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> conciliadorQueryService.buscar(null, 1, 10, "id", "lateral", null, null));

        verify(conciliadorRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(Pageable.class));
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        conciliadorQueryService.buscar("  juan   conciliador  ", 1, 10, "id", "desc", null, null);
        conciliadorQueryService.buscar("   ", 1, 10, "id", "desc", null, null);

        verify(conciliadorRepository).buscarResumenPaginado(
                eq("juan conciliador"), isNull(), isNull(), any(Pageable.class));
        verify(conciliadorRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void debeConstruirSortValidoYDirectionAscDesc() {
        stubEmpty();

        conciliadorQueryService.buscar(null, 1, 10, "nombre", "ASC", null, null);
        conciliadorQueryService.buscar(null, 1, 10, "sedeNombre", "desc", null, null);

        List<PageRequest> pageables = capturarDosPageables();
        List<Sort.Order> nombreAsc = pageables.get(0).getSort().stream().toList();
        assertEquals("nombre", nombreAsc.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, nombreAsc.get(0).getDirection());
        assertTrue(nombreAsc.get(0).isIgnoreCase());
        assertEquals("id", nombreAsc.get(1).getProperty());

        List<Sort.Order> sedeDesc = pageables.get(1).getSort().stream().toList();
        assertEquals("sede.nombre", sedeDesc.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, sedeDesc.get(0).getDirection());
        assertEquals("id", sedeDesc.get(1).getProperty());
    }

    @Test
    void debePropagarActivoTrueYFalseYTipoConciliador() {
        stubEmpty();
        
        when(conciliadorRepository.buscarResumenPaginado(any(), any(), eq(TipoConciliador.EXTERNO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        conciliadorQueryService.buscar(null, 1, 10, "id", "desc", true, null);
        conciliadorQueryService.buscar(null, 1, 10, "id", "desc", false, TipoConciliador.EXTERNO);

        verify(conciliadorRepository).buscarResumenPaginado(isNull(), eq(true), isNull(), any(Pageable.class));
        verify(conciliadorRepository).buscarResumenPaginado(isNull(), eq(false), eq(TipoConciliador.EXTERNO), any(Pageable.class));
    }

    @Test
    void debeConservarMetadataYMapearProjectionAResumen() {
        ConciliadorResumenProjection projection = mock(ConciliadorResumenProjection.class);
        ConciliadorResumenDTO dto = new ConciliadorResumenDTO();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));
        when(conciliadorRepository.buscarResumenPaginado(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 21));
        when(conciliadorMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<ConciliadorResumenDTO> resultado =
                conciliadorQueryService.buscar(null, 2, 10, "id", "desc", null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void mapperDebeConvertirProjectionAResumen() {
        ConciliadorMapper mapper = new ConciliadorMapper();
        ConciliadorResumenProjection projection = mock(ConciliadorResumenProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getNombre()).thenReturn("Conciliador A");
        when(projection.getDocumento()).thenReturn("DOC-1");
        when(projection.getEmail()).thenReturn("conciliador@example.test");
        when(projection.getUsuario()).thenReturn("conciliador.a");
        when(projection.getCodigo()).thenReturn("CON-1");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getTipoConciliador()).thenReturn(TipoConciliador.INTERNO);
        when(projection.getSedeId()).thenReturn(2L);
        when(projection.getSedeNombre()).thenReturn("Principal");

        ConciliadorResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(1L, dto.getId());
        assertEquals("Conciliador A", dto.getNombre());
        assertEquals("DOC-1", dto.getDocumento());
        assertEquals("conciliador@example.test", dto.getEmail());
        assertEquals("conciliador.a", dto.getUsuario());
        assertEquals("CON-1", dto.getCodigo());
        assertEquals(true, dto.getActivo());
        assertEquals(TipoConciliador.INTERNO, dto.getTipoConciliador());
        assertEquals(2L, dto.getSedeId());
        assertEquals("Principal", dto.getSedeNombre());
    }

    private void stubEmpty() {
        when(conciliadorRepository.buscarResumenPaginado(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private List<PageRequest> capturarDosPageables() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(conciliadorRepository, org.mockito.Mockito.times(2))
                .buscarResumenPaginado(any(), any(), any(), captor.capture());
        return captor.getAllValues().stream()
                .map(PageRequest.class::cast)
                .toList();
    }
}
