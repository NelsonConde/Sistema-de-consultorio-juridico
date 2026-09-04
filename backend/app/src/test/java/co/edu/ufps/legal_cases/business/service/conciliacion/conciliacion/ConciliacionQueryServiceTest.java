package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import co.edu.ufps.legal_cases.business.dto.conciliacion.ConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionResumenProjection;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAlcanceService;
import co.edu.ufps.legal_cases.business.service.conciliacion.reunion.ReunionConciliacionMapper;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class ConciliacionQueryServiceTest {

    private ConciliacionRepository conciliacionRepository;
    private ConciliacionAccessService conciliacionAccessService;
    private ConciliacionAlcanceService conciliacionAlcanceService;
    private ConciliacionMapper conciliacionMapper;
    private ConciliacionQueryService conciliacionQueryService;

    @BeforeEach
    void setUp() {
        conciliacionRepository = mock(ConciliacionRepository.class);
        conciliacionAccessService = mock(ConciliacionAccessService.class);
        conciliacionAlcanceService = mock(ConciliacionAlcanceService.class);
        conciliacionMapper = mock(ConciliacionMapper.class);
        conciliacionQueryService = new ConciliacionQueryService(
                conciliacionRepository,
                mock(ConsultaRepository.class),
                mock(ReunionConciliacionRepository.class),
                conciliacionAccessService,
                conciliacionAlcanceService,
                conciliacionMapper,
                mock(ReunionConciliacionMapper.class));

        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(true);
    }

    @Test
    void debeConvertirPaginaPublicaUnoAPageRequestCeroYPropagarSize() {
        PageRequest esperado = PageRequest.of(0, 25, Sort.by(Sort.Order.desc("id")));
        when(conciliacionRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 25, "id", "desc", null, null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        ConciliacionResumenProjection projection = mock(ConciliacionResumenProjection.class);
        ConciliacionResumenDTO dto = resumenDto(4L);
        PageRequest interno = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));

        when(conciliacionRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(conciliacionMapper.convertirAResumen(projection)).thenReturn(dto);

        PageResponseDTO<ConciliacionResumenDTO> resultado = conciliacionQueryService.buscarParaUsuarioActual(
                null, 2, 10, "id", "desc", null, null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
        verify(conciliacionAlcanceService, never()).puedeVerConciliacion(any(Conciliacion.class));
    }

    @Test
    void debeAceptarSizeUnoYCincuenta() {
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 1, "id", "desc", null, null, null);
        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 50, "id", "desc", null, null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("id")))));
        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("id")))));
    }

    @Test
    void debeNormalizarSearchYEstado() {
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                "  consulta   civil  ", 1, 10, "id", "desc", "en espera", null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                eq("consulta civil"),
                eq("EN_ESPERA"),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void debeTratarSearchBlankComoNull() {
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                "   ", 1, 10, "id", "desc", "   ", null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void debeRechazarSearchMayorACienCaracteres() {
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        "x".repeat(101), 1, 10, "id", "desc", null, null, null));

        verify(conciliacionRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarPaginacionFueraDeRango() {
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 0, 10, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 0, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 51, "id", "desc", null, null, null));

        verify(conciliacionRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarDirectionInvalidaNulaOBlank() {
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "lateral", null, null, null));
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", null, null, null, null));
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "   ", null, null, null));

        verify(conciliacionRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSortByInvalidoNuloOBlank() {
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "consulta", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, null, "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "   ", "desc", null, null, null));

        verify(conciliacionRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarRangoDeFechasInvalido() {
        assertThrows(BusinessException.class,
                () -> conciliacionQueryService.buscarParaUsuarioActual(
                        null,
                        1,
                        10,
                        "id",
                        "desc",
                        null,
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 9, 4)));

        verify(conciliacionRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeConstruirSortDefaultIdDesc() {
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(1, ordenes.size());
        assertEquals("id", ordenes.getFirst().getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.getFirst().getDirection());
    }

    @Test
    void debeConstruirSortFechaConciliacionAscConDesempateIdAsc() {
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fechaConciliacion", "asc", null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("fechaConciliacion", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debePropagarFiltrosOpcionalesConFechaHastaExclusiva() {
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                null,
                1,
                10,
                "id",
                "desc",
                "reunion-programada",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(),
                eq("REUNION_PROGRAMADA"),
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

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
        verify(conciliacionAccessService, never()).obtenerPerfilActual();
    }

    @Test
    void perfilesRestringidosDebenPropagarScope() {
        verificarScope(TipoPerfilUsuario.ASESOR, 11L, "ASESOR");
        verificarScope(TipoPerfilUsuario.MONITOR, 12L, "MONITOR");
        verificarScope(TipoPerfilUsuario.CONCILIADOR, 13L, "CONCILIADOR");
        verificarScope(TipoPerfilUsuario.ESTUDIANTE, 14L, "ESTUDIANTE");
    }

    @Test
    void perfilAusenteDebeResolverFailClosedSinNullPointerException() {
        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(conciliacionAccessService.obtenerPerfilActual()).thenReturn(null);
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void projectionDebeMapearADtoCorrecto() {
        ConciliacionMapper mapper = new ConciliacionMapper();
        ConciliacionResumenProjection projection = mock(ConciliacionResumenProjection.class);
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 4, 10, 30);
        when(projection.getId()).thenReturn(30L);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getConsultaId()).thenReturn(2L);
        when(projection.getConsulta()).thenReturn("Consulta civil");
        when(projection.getEstadoCodigo()).thenReturn("EN_ESPERA");
        when(projection.getEstadoNombre()).thenReturn("En espera");
        when(projection.getEstudianteId()).thenReturn(5L);
        when(projection.getEstudianteNombre()).thenReturn("Estudiante A");
        when(projection.getConciliadorId()).thenReturn(6L);
        when(projection.getConciliadorNombre()).thenReturn("Conciliador A");
        when(projection.getFechaCreacion()).thenReturn(fecha);
        when(projection.getFechaConciliacion()).thenReturn(fecha.plusDays(2));
        when(projection.getFechaFinalizacion()).thenReturn(fecha.plusDays(3));
        when(projection.getActivo()).thenReturn(true);

        ConciliacionResumenDTO dto = mapper.convertirAResumen(projection);

        assertEquals(30L, dto.id());
        assertEquals(4L, dto.version());
        assertEquals(2L, dto.consultaId());
        assertEquals("Consulta civil", dto.consulta());
        assertEquals("EN_ESPERA", dto.estadoCodigo());
        assertEquals("En espera", dto.estadoNombre());
        assertEquals(5L, dto.estudianteId());
        assertEquals("Estudiante A", dto.estudianteNombre());
        assertEquals(6L, dto.conciliadorId());
        assertEquals("Conciliador A", dto.conciliadorNombre());
        assertEquals(fecha, dto.fechaCreacion());
        assertEquals(fecha.plusDays(2), dto.fechaConciliacion());
        assertEquals(fecha.plusDays(3), dto.fechaFinalizacion());
        assertTrue(dto.activo());
    }

    private void stubEmpty() {
        when(conciliacionRepository.buscarResumenPaginado(
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

    private void verificarScope(TipoPerfilUsuario tipoPerfil, Long perfilId, String tipoPerfilEsperado) {
        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(conciliacionAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(perfilId, tipoPerfil));
        stubEmpty();

        conciliacionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(conciliacionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(tipoPerfilEsperado), eq(perfilId), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    private PageRequest capturarPageRequest() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(conciliacionRepository).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), captor.capture());
        return (PageRequest) captor.getValue();
    }

    private ConciliacionResumenDTO resumenDto(Long id) {
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 4, 10, 30);
        return new ConciliacionResumenDTO(
                id,
                2L,
                3L,
                "Consulta",
                "EN_ESPERA",
                "En espera",
                4L,
                "Estudiante",
                5L,
                "Conciliador",
                fecha,
                fecha.plusDays(1),
                null,
                true);
    }
}
