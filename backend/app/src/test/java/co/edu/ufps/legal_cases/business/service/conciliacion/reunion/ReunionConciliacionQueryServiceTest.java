package co.edu.ufps.legal_cases.business.service.conciliacion.reunion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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

import co.edu.ufps.legal_cases.business.dto.conciliacion.reunion.ReunionConciliacionResumenDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class ReunionConciliacionQueryServiceTest {

    private ReunionConciliacionRepository reunionRepository;
    private ConciliacionAccessService conciliacionAccessService;
    private ReunionConciliacionMapper reunionMapper;
    private ReunionConciliacionQueryService reunionQueryService;

    @BeforeEach
    void setUp() {
        reunionRepository = mock(ReunionConciliacionRepository.class);
        conciliacionAccessService = mock(ConciliacionAccessService.class);
        reunionMapper = mock(ReunionConciliacionMapper.class);
        reunionQueryService = new ReunionConciliacionQueryService(
                reunionRepository,
                conciliacionAccessService,
                reunionMapper);

        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(true);
    }

    @Test
    void debeConvertirPaginaPublicaUnoAPageRequestCeroYPropagarSize() {
        PageRequest esperado = PageRequest.of(0, 25, Sort.by(Sort.Order.desc("conciliacionId")));
        when(reunionRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 25, "id", "desc", null, null, null);

        verify(reunionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        ReunionConciliacionResumenProjection projection = mock(ReunionConciliacionResumenProjection.class);
        ReunionConciliacionResumenDTO dto = resumenDto(4L);
        PageRequest interno = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("conciliacionId")));

        when(reunionRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(reunionMapper.convertirAResumen(projection)).thenReturn(dto);

        PageResponseDTO<ReunionConciliacionResumenDTO> resultado = reunionQueryService.buscarParaUsuarioActual(
                null, 2, 10, "id", "desc", null, null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void debeAceptarSizeUnoYCincuenta() {
        stubEmpty();

        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 1, "id", "desc", null, null, null);
        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 50, "id", "desc", null, null, null);

        verify(reunionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("conciliacionId")))));
        verify(reunionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO),
                eq(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("conciliacionId")))));
    }

    @Test
    void debeNormalizarSearchYEstado() {
        stubEmpty();

        reunionQueryService.buscarParaUsuarioActual(
                "  consulta   civil  ", 1, 10, "id", "desc", "en espera", null, null);

        verify(reunionRepository).buscarResumenPaginado(
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
    void debeRechazarSearchMayorACienCaracteresYParametrosInvalidos() {
        assertThrows(BusinessException.class,
                () -> reunionQueryService.buscarParaUsuarioActual(
                        "x".repeat(101), 1, 10, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> reunionQueryService.buscarParaUsuarioActual(
                        null, 0, 10, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> reunionQueryService.buscarParaUsuarioActual(
                        null, 1, 51, "id", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> reunionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "consulta", "desc", null, null, null));
        assertThrows(BusinessException.class,
                () -> reunionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "lateral", null, null, null));
        assertThrows(BusinessException.class,
                () -> reunionQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "desc", null,
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 9, 4)));

        verify(reunionRepository, org.mockito.Mockito.never()).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeConstruirSortFechaReunionAscConDesempateConciliacionIdAsc() {
        stubEmpty();

        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "fechaReunion", "asc", null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("fechaReunion", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertEquals("conciliacionId", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void debeConstruirSortEstadoYSedeConDesempate() {
        stubEmpty();

        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "estado", "desc", null, null, null);

        List<Sort.Order> estado = capturarPageRequest().getSort().stream().toList();
        assertEquals("conciliacion.estado.codigo", estado.get(0).getProperty());
        assertEquals("conciliacionId", estado.get(1).getProperty());
    }

    @Test
    void debePropagarFiltrosOpcionalesConFechaHastaExclusiva() {
        stubEmpty();

        reunionQueryService.buscarParaUsuarioActual(
                null,
                1,
                10,
                "id",
                "desc",
                "reunion-programada",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));

        verify(reunionRepository).buscarResumenPaginado(
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

        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(reunionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
        verify(conciliacionAccessService, org.mockito.Mockito.never()).obtenerPerfilActual();
    }

    @Test
    void perfilRestringidoDebePropagarScope() {
        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(conciliacionAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(15L, TipoPerfilUsuario.CONCILIADOR));
        stubEmpty();

        reunionQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null);

        verify(reunionRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("CONCILIADOR"), eq(15L), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void projectionDebeMapearADtoCorrecto() {
        ReunionConciliacionMapper mapper = new ReunionConciliacionMapper();
        ReunionConciliacionResumenProjection projection = mock(ReunionConciliacionResumenProjection.class);
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 4, 10, 30);
        when(projection.getConciliacionId()).thenReturn(30L);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getConciliacionVersion()).thenReturn(5L);
        when(projection.getConsultaId()).thenReturn(2L);
        when(projection.getEstadoCodigo()).thenReturn("REUNION_PROGRAMADA");
        when(projection.getEstadoNombre()).thenReturn("Reunion programada");
        when(projection.getSedeId()).thenReturn(8L);
        when(projection.getSedeNombre()).thenReturn("Principal");
        when(projection.getFechaReunion()).thenReturn(fecha);
        when(projection.getObservaciones()).thenReturn("Audiencia");
        when(projection.getEstudianteId()).thenReturn(6L);
        when(projection.getEstudianteNombre()).thenReturn("Estudiante A");
        when(projection.getConciliadorId()).thenReturn(7L);
        when(projection.getConciliadorNombre()).thenReturn("Conciliador A");
        when(projection.getFechaCreacion()).thenReturn(fecha.minusDays(1));
        when(projection.getFechaActualizacion()).thenReturn(fecha.plusDays(1));

        ReunionConciliacionResumenDTO dto = mapper.convertirAResumen(projection);

        assertEquals(30L, dto.conciliacionId());
        assertEquals(4L, dto.version());
        assertEquals(5L, dto.conciliacionVersion());
        assertEquals(2L, dto.consultaId());
        assertEquals("REUNION_PROGRAMADA", dto.estadoCodigo());
        assertEquals("Principal", dto.sedeNombre());
        assertEquals(fecha, dto.fechaReunion());
        assertEquals("Audiencia", dto.observaciones());
        assertTrue(dto.fechaActualizacion().isAfter(dto.fechaCreacion()));
    }

    private void stubEmpty() {
        when(reunionRepository.buscarResumenPaginado(
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

    private PageRequest capturarPageRequest() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reunionRepository).buscarResumenPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), captor.capture());
        return (PageRequest) captor.getValue();
    }

    private ReunionConciliacionResumenDTO resumenDto(Long id) {
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 4, 10, 30);
        return new ReunionConciliacionResumenDTO(
                id,
                2L,
                3L,
                4L,
                "REUNION_PROGRAMADA",
                "Reunion programada",
                5L,
                "Principal",
                fecha,
                "Observacion",
                6L,
                "Estudiante",
                7L,
                "Conciliador",
                fecha.minusDays(1),
                fecha.plusDays(1));
    }
}
