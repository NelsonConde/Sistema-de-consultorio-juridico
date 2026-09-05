package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

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

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoResumenDTO;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AdministrativoAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;

class AdministrativoQueryServiceTest {

    private AdministrativoRepository administrativoRepository;
    private AdministrativoAccessService administrativoAccessService;
    private AdministrativoMapper administrativoMapper;
    private AdministrativoQueryService administrativoQueryService;

    @BeforeEach
    void setUp() {
        administrativoRepository = mock(AdministrativoRepository.class);
        administrativoAccessService = mock(AdministrativoAccessService.class);
        administrativoMapper = mock(AdministrativoMapper.class);

        administrativoQueryService = new AdministrativoQueryService(
                administrativoRepository,
                administrativoAccessService,
                administrativoMapper);
    }

    @Test
    void buscarDebeUsarDefaultsValidosYValidarAcceso() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(administrativoRepository.buscarResumenPaginado(isNull(), isNull(), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        administrativoQueryService.buscar(null, 1, 10, "id", "desc", null);

        verify(administrativoAccessService).validarPuedeVerAdministradores();
        verify(administrativoRepository).buscarResumenPaginado(isNull(), isNull(), eq(esperado));
    }

    @Test
    void debeValidarPaginacionSearchSortYDirection() {
        assertThrows(BusinessException.class,
                () -> administrativoQueryService.buscar(null, 0, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> administrativoQueryService.buscar(null, 1, 0, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> administrativoQueryService.buscar(null, 1, 51, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> administrativoQueryService.buscar("x".repeat(101), 1, 10, "id", "desc", null));
        assertThrows(BusinessException.class,
                () -> administrativoQueryService.buscar(null, 1, 10, "telefono", "desc", null));
        assertThrows(BusinessException.class,
                () -> administrativoQueryService.buscar(null, 1, 10, "id", "lateral", null));

        verify(administrativoRepository, never()).buscarResumenPaginado(
                any(), any(), any(Pageable.class));
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        administrativoQueryService.buscar("  ana   directora  ", 1, 10, "id", "desc", null);
        administrativoQueryService.buscar("   ", 1, 10, "id", "desc", null);

        verify(administrativoRepository).buscarResumenPaginado(
                eq("ana directora"), isNull(), any(Pageable.class));
        verify(administrativoRepository).buscarResumenPaginado(
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void debeConstruirSortValidoYDirectionAscDesc() {
        stubEmpty();

        administrativoQueryService.buscar(null, 1, 10, "nombre", "ASC", null);
        administrativoQueryService.buscar(null, 1, 10, "directora", "desc", null);

        List<PageRequest> pageables = capturarDosPageables();
        List<Sort.Order> nombreAsc = pageables.get(0).getSort().stream().toList();
        assertEquals("nombre", nombreAsc.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, nombreAsc.get(0).getDirection());
        assertTrue(nombreAsc.get(0).isIgnoreCase());
        assertEquals("id", nombreAsc.get(1).getProperty());

        List<Sort.Order> directoraDesc = pageables.get(1).getSort().stream().toList();
        assertEquals("directora", directoraDesc.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, directoraDesc.get(0).getDirection());
        assertEquals("id", directoraDesc.get(1).getProperty());
    }

    @Test
    void debePropagarActivoTrueYFalse() {
        stubEmpty();

        administrativoQueryService.buscar(null, 1, 10, "id", "desc", true);
        administrativoQueryService.buscar(null, 1, 10, "id", "desc", false);

        verify(administrativoRepository).buscarResumenPaginado(isNull(), eq(true), any(Pageable.class));
        verify(administrativoRepository).buscarResumenPaginado(isNull(), eq(false), any(Pageable.class));
    }

    @Test
    void debeConservarMetadataYMapearProjectionAResumen() {
        AdministrativoResumenProjection projection = mock(AdministrativoResumenProjection.class);
        AdministrativoResumenDTO dto = new AdministrativoResumenDTO();
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));
        when(administrativoRepository.buscarResumenPaginado(isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 21));
        when(administrativoMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<AdministrativoResumenDTO> resultado =
                administrativoQueryService.buscar(null, 2, 10, "id", "desc", null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void mapperDebeConvertirProjectionAResumen() {
        AdministrativoMapper mapper = new AdministrativoMapper();
        AdministrativoResumenProjection projection = mock(AdministrativoResumenProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getNombre()).thenReturn("Administrativa A");
        when(projection.getDocumento()).thenReturn("DOC-1");
        when(projection.getEmail()).thenReturn("admin@example.test");
        when(projection.getUsuario()).thenReturn("admin.a");
        when(projection.getCodigo()).thenReturn("ADM-1");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getDirectora()).thenReturn(false);
        when(projection.getSedeId()).thenReturn(2L);
        when(projection.getSedeNombre()).thenReturn("Principal");

        AdministrativoResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(1L, dto.getId());
        assertEquals("Administrativa A", dto.getNombre());
        assertEquals("DOC-1", dto.getDocumento());
        assertEquals("admin@example.test", dto.getEmail());
        assertEquals("admin.a", dto.getUsuario());
        assertEquals("ADM-1", dto.getCodigo());
        assertEquals(true, dto.getActivo());
        assertEquals(false, dto.getDirectora());
        assertEquals(2L, dto.getSedeId());
        assertEquals("Principal", dto.getSedeNombre());
    }

    private void stubEmpty() {
        when(administrativoRepository.buscarResumenPaginado(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private List<PageRequest> capturarDosPageables() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(administrativoRepository, org.mockito.Mockito.times(2))
                .buscarResumenPaginado(any(), any(), captor.capture());
        return captor.getAllValues().stream()
                .map(PageRequest.class::cast)
                .toList();
    }
}
