package co.edu.ufps.legal_cases.business.service.proceso.proceso;

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
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.business.dto.proceso.ProcesoResumenDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.proceso.ProcesoAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class ProcesoQueryServiceTest {

    private ProcesoRepository procesoRepository;
    private ProcesoAccessService procesoAccessService;
    private ProcesoMapper procesoMapper;
    private ProcesoQueryService procesoQueryService;

    @BeforeEach
    void setUp() {
        procesoRepository = mock(ProcesoRepository.class);
        procesoAccessService = mock(ProcesoAccessService.class);
        procesoMapper = mock(ProcesoMapper.class);
        procesoQueryService = new ProcesoQueryService(
                procesoRepository,
                procesoAccessService,
                procesoMapper);

        when(procesoAccessService.usuarioEsAdministrador()).thenReturn(true);
    }

    @Test
    void debeConvertirPaginaPublicaUnoAPageRequestCeroYPropagarSize() {
        PageRequest esperado = PageRequest.of(0, 25, Sort.by(Sort.Order.desc("id")));
        when(procesoRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 25, "id", "desc", null, null, null);

        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        ProcesoResumenProjection projection = mock(ProcesoResumenProjection.class);
        ProcesoResumenDTO dto = new ProcesoResumenDTO(
                4L,
                2L,
                "RAD-2026-000000000001",
                5L,
                "Departamento",
                6L,
                "Consulta",
                7L,
                "Organo",
                8L,
                "Especialidad",
                EstadoProceso.PENDIENTE,
                true,
                LocalDateTime.of(2026, 9, 4, 10, 30));
        PageRequest interno = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));

        when(procesoRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(procesoMapper.convertirAResumen(projection)).thenReturn(dto);

        PageResponseDTO<ProcesoResumenDTO> resultado = procesoQueryService.buscarParaUsuarioActual(
                null, 2, 10, "id", "desc", null, null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(procesoAccessService, never()).puedeAccederAProceso(any(Proceso.class));
    }

    @Test
    void debeAceptarSizeUnoYCincuenta() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 1, "id", "desc", null, null, null);
        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 50, "id", "desc", null, null, null);

        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("id")))));
        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("id")))));
    }

    @Test
    void debeTratarSearchBlankComoNull() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                "   ", 1, 10, "id", "desc", null, null, null);

        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void debeNormalizarEspaciosRepetidosEnSearch() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                "  radicado   civil  ", 1, 10, "id", "desc", null, null, null);

        verify(procesoRepository).buscarResumenPaginado(
                eq("radicado civil"), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void debeRechazarSearchMayorACienCaracteres() {
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        "x".repeat(101), 1, 10, "id", "desc", null, null, null));

        verify(procesoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarPaginacionFueraDeRango() {
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 0, 10, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 0, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 51, "id", "desc", null, null, null));

        verify(procesoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarDirectionInvalidaNulaOBlank() {
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "lateral", null, null, null));
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", null, null, null, null));
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "   ", null, null, null));

        verify(procesoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSortByInvalidoNuloOBlank() {
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "consulta", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, null, "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "   ", "desc", null, null, null));

        verify(procesoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarRangoDeFechasInvalido() {
        assertThrows(BusinessException.class,
                () -> procesoQueryService.buscarParaUsuarioActual(
                        null,
                        1,
                        10,
                        "id",
                        "desc",
                        null,
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 9, 4)));

        verify(procesoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeConstruirSortDefaultIdDesc() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(1, ordenes.size());
        assertEquals("id", ordenes.getFirst().getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.getFirst().getDirection());
    }

    @Test
    void debeConstruirSortFechaCreacionDescConDesempateIdAsc() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fechaCreacion", "desc", null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("fechaCreacion", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.get(0).getDirection());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debeConstruirSortNumeroRadicadoAscIgnoreCaseConDesempateIdAsc() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "numeroRadicado", "asc", null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("numeroRadicado", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
    }

    @Test
    void debePropagarFiltrosOpcionales() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null,
                1,
                10,
                "id",
                "desc",
                EstadoProceso.SENTENCIA_FAVORABLE,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));

        verify(procesoRepository).buscarResumenPaginado(
                isNull(),
                eq(EstadoProceso.SENTENCIA_FAVORABLE),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void administradorDebeUsarAlcanceGlobal() {
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
        verify(procesoAccessService, never()).obtenerPerfilActual();
    }

    @Test
    void estudianteDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ESTUDIANTE, 11L);
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verificarScope("ESTUDIANTE", 11L);
    }

    @Test
    void asesorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ASESOR, 12L);
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verificarScope("ASESOR", 12L);
    }

    @Test
    void monitorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.MONITOR, 13L);
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verificarScope("MONITOR", 13L);
    }

    @Test
    void perfilAusenteDebeResolverFailClosedSinNullPointerException() {
        when(procesoAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(procesoAccessService.obtenerPerfilActual()).thenReturn(null);
        stubEmpty();

        procesoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void projectionDebeMapearADtoCorrecto() {
        ProcesoMapper mapper = new ProcesoMapper();
        ProcesoResumenProjection projection = mock(ProcesoResumenProjection.class);
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 4, 10, 30);
        when(projection.getId()).thenReturn(30L);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getNumeroRadicado()).thenReturn("RAD-2026-000000000001");
        when(projection.getDepartamentoId()).thenReturn(2L);
        when(projection.getDepartamentoNombre()).thenReturn("Norte");
        when(projection.getConsultaId()).thenReturn(3L);
        when(projection.getConsulta()).thenReturn("Consulta civil");
        when(projection.getOrganoControlId()).thenReturn(5L);
        when(projection.getOrganoControlNombre()).thenReturn("Juzgado");
        when(projection.getEspecialidadId()).thenReturn(6L);
        when(projection.getEspecialidadNombre()).thenReturn("Familia");
        when(projection.getEstado()).thenReturn(EstadoProceso.PENDIENTE);
        when(projection.getActivo()).thenReturn(true);
        when(projection.getFechaCreacion()).thenReturn(fecha);

        ProcesoResumenDTO dto = mapper.convertirAResumen(projection);

        assertEquals(30L, dto.id());
        assertEquals(4L, dto.version());
        assertEquals("RAD-2026-000000000001", dto.numeroRadicado());
        assertEquals(2L, dto.departamentoId());
        assertEquals("Norte", dto.departamentoNombre());
        assertEquals(3L, dto.consultaId());
        assertEquals("Consulta civil", dto.consulta());
        assertEquals(5L, dto.organoControlId());
        assertEquals("Juzgado", dto.organoControlNombre());
        assertEquals(6L, dto.especialidadId());
        assertEquals("Familia", dto.especialidadNombre());
        assertEquals(EstadoProceso.PENDIENTE, dto.estado());
        assertTrue(dto.activo());
        assertEquals(fecha, dto.fechaCreacion());
    }

    private void stubEmpty() {
        when(procesoRepository.buscarResumenPaginado(
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(),
                        invocation.getArgument(8),
                        0));
    }

    private void stubPerfil(TipoPerfilUsuario tipoPerfil, Long perfilId) {
        when(procesoAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(procesoAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(perfilId, tipoPerfil));
    }

    private void verificarScope(String tipoPerfil, Long perfilId) {
        verify(procesoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(tipoPerfil), eq(perfilId), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    private PageRequest capturarPageRequest() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(procesoRepository).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), captor.capture());
        return (PageRequest) captor.getValue();
    }
}
