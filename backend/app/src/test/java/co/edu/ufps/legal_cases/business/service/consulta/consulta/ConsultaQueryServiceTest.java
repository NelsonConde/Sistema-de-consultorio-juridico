package co.edu.ufps.legal_cases.business.service.consulta.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class ConsultaQueryServiceTest {

    private ConsultaRepository consultaRepository;
    private ConsultaAccessService consultaAccessService;
    private ConsultaMapper consultaMapper;
    private ConsultaQueryService consultaQueryService;

    @BeforeEach
    void setUp() {
        consultaRepository = mock(ConsultaRepository.class);
        consultaAccessService = mock(ConsultaAccessService.class);
        consultaMapper = mock(ConsultaMapper.class);
        consultaQueryService = new ConsultaQueryService(
                consultaRepository,
                consultaAccessService,
                consultaMapper);

        when(consultaAccessService.usuarioEsAdministrador()).thenReturn(true);
    }

    @Test
    void debeConvertirPaginaPublicaUnoAPageRequestCeroYPropagarSize() {
        PageRequest esperado = PageRequest.of(0, 25, Sort.by(
                Sort.Order.desc("fecha"),
                Sort.Order.asc("id")));
        when(consultaRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 25, "fecha", "desc", null, null, null, null, null);

        verify(consultaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        ConsultaResumenProjection projection = mock(ConsultaResumenProjection.class);
        ConsultaBusquedaDTO dto = new ConsultaBusquedaDTO(
                4L,
                2L,
                "Consulta laboral",
                LocalDate.of(2026, 9, 3),
                "Ana",
                "Perez",
                "1090",
                EstadoConsulta.ACTIVO);
        PageRequest interno = PageRequest.of(1, 10, Sort.by(
                Sort.Order.desc("fecha"),
                Sort.Order.asc("id")));

        when(consultaRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(consultaMapper.convertirABusquedaDTO(projection)).thenReturn(dto);

        PageResponseDTO<ConsultaBusquedaDTO> resultado = consultaQueryService.buscarParaUsuarioActual(
                null, 2, 10, "fecha", "desc", null, null, null, null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void debeEnviarSearchNullAlRepositorio() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verify(consultaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void debeTratarSearchBlankComoNull() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                "   ", 1, 10, "fecha", "desc", null, null, null, null, null);

        verify(consultaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void debeNormalizarEspaciosRepetidosEnSearch() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                "  Juan   Perez  ", 1, 10, "fecha", "desc", null, null, null, null, null);

        verify(consultaRepository).buscarResumenPaginado(
                eq("Juan Perez"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void debeRechazarSearchMayorACienCaracteres() {
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        "x".repeat(101), 1, 10, "fecha", "desc", null, null, null, null, null));

        verify(consultaRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarPaginacionFueraDeRango() {
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 0, 10, "fecha", "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 0, "fecha", "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 51, "fecha", "desc", null, null, null, null, null));

        verify(consultaRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSortByInvalidoNuloOBlank() {
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "invalido", "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, null, "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "   ", "desc", null, null, null, null, null));

        verify(consultaRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarDirectionInvalidaNulaOBlank() {
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "fecha", "lateral", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "fecha", null, null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> consultaQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "fecha", "   ", null, null, null, null, null));

        verify(consultaRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeConstruirSortDefaultFechaDescConDesempateIdAsc() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("fecha", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.get(0).getDirection());
        assertFalse(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debeConstruirSortTextualNombreAscIgnoreCaseConDesempateIdAsc() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "nombre", "asc", null, null, null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("persona.nombres", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debePropagarFiltrosOpcionales() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc",
                7L, EstadoConsulta.CERRADO, 8L, 9L, 10L);

        verify(consultaRepository).buscarResumenPaginado(
                isNull(),
                eq(7L),
                eq(EstadoConsulta.CERRADO),
                eq(8L),
                eq(9L),
                eq(10L),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void administradorDebeUsarAlcanceGlobal() {
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verify(consultaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
        verify(consultaAccessService, never()).obtenerPerfilActual();
    }

    @Test
    void estudianteDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ESTUDIANTE, 11L);
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verificarScope("ESTUDIANTE", 11L);
    }

    @Test
    void asesorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ASESOR, 12L);
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verificarScope("ASESOR", 12L);
    }

    @Test
    void monitorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.MONITOR, 13L);
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verificarScope("MONITOR", 13L);
    }

    @Test
    void perfilNoSoportadoNoDebeTerminarComoAlcanceGlobal() {
        stubPerfil(TipoPerfilUsuario.ADMINISTRATIVO, 14L);
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verificarScope("ADMINISTRATIVO", 14L);
    }

    @Test
    void perfilAusenteDebeResolverFailClosedSinNullPointerException() {
        when(consultaAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(consultaAccessService.obtenerPerfilActual()).thenReturn(null);
        stubEmpty();

        consultaQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fecha", "desc", null, null, null, null, null);

        verify(consultaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void projectionDebeMapearADtoCorrecto() {
        ConsultaMapper mapper = new ConsultaMapper();
        ConsultaResumenProjection projection = mock(ConsultaResumenProjection.class);
        LocalDate fecha = LocalDate.of(2026, 9, 3);
        when(projection.getId()).thenReturn(30L);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getConsulta()).thenReturn("Consulta civil");
        when(projection.getFecha()).thenReturn(fecha);
        when(projection.getNombre()).thenReturn("Laura");
        when(projection.getApellido()).thenReturn("Gomez");
        when(projection.getCedula()).thenReturn("1002");
        when(projection.getEstado()).thenReturn(EstadoConsulta.ACTIVO);

        ConsultaBusquedaDTO dto = mapper.convertirABusquedaDTO(projection);

        assertEquals(30L, dto.getId());
        assertEquals(4L, dto.getVersion());
        assertEquals("Consulta civil", dto.getConsulta());
        assertEquals(fecha, dto.getFecha());
        assertEquals("Laura", dto.getNombre());
        assertEquals("Gomez", dto.getApellido());
        assertEquals("1002", dto.getCedula());
        assertEquals(EstadoConsulta.ACTIVO, dto.getEstado());
    }

    private void stubEmpty() {
        when(consultaRepository.buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private void stubPerfil(TipoPerfilUsuario tipoPerfil, Long perfilId) {
        when(consultaAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(consultaAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(perfilId, tipoPerfil));
    }

    private PageRequest capturarPageRequest() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(consultaRepository).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    private void verificarScope(String tipoPerfil, Long perfilId) {
        verify(consultaRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(tipoPerfil), eq(perfilId), eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }
}
