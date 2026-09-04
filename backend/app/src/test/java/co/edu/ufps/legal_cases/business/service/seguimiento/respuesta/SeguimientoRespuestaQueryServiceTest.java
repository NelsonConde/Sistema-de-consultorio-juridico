package co.edu.ufps.legal_cases.business.service.seguimiento.respuesta;

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

import co.edu.ufps.legal_cases.business.dto.seguimiento.respuesta.SeguimientoRespuestaResponseDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaPendienteProjection;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoRespuestaAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class SeguimientoRespuestaQueryServiceTest {

    private SeguimientoRespuestaRepository seguimientoRespuestaRepository;
    private SeguimientoRespuestaAccessService seguimientoRespuestaAccessService;
    private SeguimientoRespuestaMapper seguimientoRespuestaMapper;
    private SeguimientoRespuestaQueryService seguimientoRespuestaQueryService;

    @BeforeEach
    void setUp() {
        seguimientoRespuestaRepository = mock(SeguimientoRespuestaRepository.class);
        seguimientoRespuestaAccessService = mock(SeguimientoRespuestaAccessService.class);
        seguimientoRespuestaMapper = mock(SeguimientoRespuestaMapper.class);

        seguimientoRespuestaQueryService = new SeguimientoRespuestaQueryService(
                seguimientoRespuestaRepository,
                seguimientoRespuestaAccessService,
                seguimientoRespuestaMapper);

        when(seguimientoRespuestaAccessService.usuarioEsAdministrador()).thenReturn(true);
    }

    @Test
    void listarPendientesDebeUsarDefaultsValidosYConvertirPaginaPublicaUnoAPageRequestCero() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.asc("id")));
        when(seguimientoRespuestaRepository.buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verify(seguimientoRespuestaAccessService).validarPuedeListarRespuestasPendientes();
        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        SeguimientoRespuestaPendienteProjection projection = mock(SeguimientoRespuestaPendienteProjection.class);
        SeguimientoRespuestaResponseDTO dto = new SeguimientoRespuestaResponseDTO();
        PageRequest interno = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.asc("id")));

        when(seguimientoRespuestaRepository.buscarPendientesPaginado(
                isNull(), eq(EstadoRespuestaSeguimiento.PENDIENTE), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(seguimientoRespuestaMapper.convertirAResponseDTO(projection)).thenReturn(dto);

        PageResponseDTO<SeguimientoRespuestaResponseDTO> resultado =
                seguimientoRespuestaQueryService.listarPendientes(
                        null, 2, 10, "fechaCreacion", "desc", null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                "  respuesta   pendiente  ", 1, 10, "fechaCreacion", "desc", null, null);

        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                eq("respuesta pendiente"),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(Pageable.class));

        seguimientoRespuestaQueryService.listarPendientes(
                "   ", 1, 10, "fechaCreacion", "desc", null, null);

        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(Pageable.class));
    }

    @Test
    void debeRechazarSearchMayorACienCaracteres() {
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        "x".repeat(101), 1, 10, "fechaCreacion", "desc", null, null));

        verify(seguimientoRespuestaRepository, never()).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarPageInvalida() {
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 0, 10, "fechaCreacion", "desc", null, null));

        verify(seguimientoRespuestaRepository, never()).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSizeInvalido() {
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 0, "fechaCreacion", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 51, "fechaCreacion", "desc", null, null));

        verify(seguimientoRespuestaRepository, never()).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSortInvalido() {
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 10, "estado", "desc", null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 10, null, "desc", null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 10, "   ", "desc", null, null));

        verify(seguimientoRespuestaRepository, never()).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarDirectionInvalida() {
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 10, "fechaCreacion", "lateral", null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 10, "fechaCreacion", null, null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null, 1, 10, "fechaCreacion", "   ", null, null));

        verify(seguimientoRespuestaRepository, never()).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarRangoInvalido() {
        assertThrows(BusinessException.class,
                () -> seguimientoRespuestaQueryService.listarPendientes(
                        null,
                        1,
                        10,
                        "fechaCreacion",
                        "desc",
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1)));

        verify(seguimientoRespuestaRepository, never()).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debePropagarFechasSobreFechaCreacionConHastaExclusiva() {
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null,
                1,
                10,
                "fechaCreacion",
                "desc",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30));

        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(Pageable.class));
    }

    @Test
    void debeConstruirSortEstableParaCampoDistintoDeId() {
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "estudianteNombre", "ASC", null, null);

        List<Sort.Order> ordenes = capturarPageable().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("estudiante.nombre", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void sortPorIdNoDebeAgregarDesempateDuplicado() {
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "id", "desc", null, null);

        List<Sort.Order> ordenes = capturarPageable().getSort().stream().toList();
        assertEquals(1, ordenes.size());
        assertEquals("id", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.get(0).getDirection());
    }

    @Test
    void administradorDebeUsarAlcanceGlobal() {
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(Pageable.class));
        verify(seguimientoRespuestaAccessService, never()).obtenerPerfilActual();
    }

    @Test
    void asesorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ASESOR, 12L);
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verificarScope("ASESOR", 12L);
    }

    @Test
    void monitorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.MONITOR, 13L);
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verificarScope("MONITOR", 13L);
    }

    @Test
    void conciliadorDebeResolverFailClosedSiAccessServiceNoDeniegaAntes() {
        stubPerfil(TipoPerfilUsuario.CONCILIADOR, 15L);
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verificarScope("CONCILIADOR", 15L);
    }

    @Test
    void perfilNuloONoSoportadoDebeResolverFailClosed() {
        when(seguimientoRespuestaAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(seguimientoRespuestaAccessService.obtenerPerfilActual()).thenReturn(null);
        stubEmpty();

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(false),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(Pageable.class));

        stubPerfil(TipoPerfilUsuario.ADMINISTRATIVO, 16L);
        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verificarScope("ADMINISTRATIVO", 16L);
    }

    @Test
    void listadoPaginadoNoDebeAplicarPostFiltradoDeAutorizacion() {
        SeguimientoRespuestaPendienteProjection projection = mock(SeguimientoRespuestaPendienteProjection.class);
        SeguimientoRespuestaResponseDTO dto = new SeguimientoRespuestaResponseDTO();
        when(seguimientoRespuestaRepository.buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));
        when(seguimientoRespuestaMapper.convertirAResponseDTO(projection)).thenReturn(dto);

        seguimientoRespuestaQueryService.listarPendientes(
                null, 1, 10, "fechaCreacion", "desc", null, null);

        verify(seguimientoRespuestaAccessService, never()).puedeRevisarRespuesta(any(SeguimientoRespuesta.class));
    }

    @Test
    void projectionDebeMapearADtoCorrecto() {
        SeguimientoRespuestaMapper mapper = new SeguimientoRespuestaMapper();
        SeguimientoRespuestaPendienteProjection projection = mock(SeguimientoRespuestaPendienteProjection.class);
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 9, 4, 10, 30);
        LocalDateTime fechaActualizacion = LocalDateTime.of(2026, 9, 5, 11, 30);
        LocalDateTime fechaDecision = LocalDateTime.of(2026, 9, 6, 12, 30);
        when(projection.getId()).thenReturn(30L);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getSeguimientoId()).thenReturn(5L);
        when(projection.getConsultaId()).thenReturn(6L);
        when(projection.getEstudianteId()).thenReturn(7L);
        when(projection.getEstudianteNombre()).thenReturn("Estudiante A");
        when(projection.getContenido()).thenReturn("Respuesta pendiente");
        when(projection.getEstado()).thenReturn(EstadoRespuestaSeguimiento.PENDIENTE);
        when(projection.getFueraPlazo()).thenReturn(true);
        when(projection.getObservacionRevision()).thenReturn("Observacion");
        when(projection.getRevisadoPorId()).thenReturn(8L);
        when(projection.getRevisadoPorUsername()).thenReturn("revisor@example.test");
        when(projection.getActivo()).thenReturn(true);
        when(projection.getFechaCreacion()).thenReturn(fechaCreacion);
        when(projection.getFechaActualizacion()).thenReturn(fechaActualizacion);
        when(projection.getFechaDecision()).thenReturn(fechaDecision);

        SeguimientoRespuestaResponseDTO dto = mapper.convertirAResponseDTO(projection);

        assertEquals(30L, dto.getId());
        assertEquals(4L, dto.getVersion());
        assertEquals(5L, dto.getSeguimientoId());
        assertEquals(6L, dto.getConsultaId());
        assertEquals(7L, dto.getEstudianteId());
        assertEquals("Estudiante A", dto.getEstudianteNombre());
        assertEquals("Respuesta pendiente", dto.getContenido());
        assertEquals(EstadoRespuestaSeguimiento.PENDIENTE, dto.getEstado());
        assertEquals(true, dto.getFueraPlazo());
        assertEquals("Observacion", dto.getObservacionRevision());
        assertEquals(8L, dto.getRevisadoPorId());
        assertEquals("revisor@example.test", dto.getRevisadoPorUsername());
        assertEquals(true, dto.getActivo());
        assertEquals(fechaCreacion, dto.getFechaCreacion());
        assertEquals(fechaActualizacion, dto.getFechaActualizacion());
        assertEquals(fechaDecision, dto.getFechaDecision());
    }

    private void stubEmpty() {
        when(seguimientoRespuestaRepository.buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private void stubPerfil(TipoPerfilUsuario tipoPerfil, Long perfilId) {
        when(seguimientoRespuestaAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(seguimientoRespuestaAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(perfilId, tipoPerfil));
    }

    private PageRequest capturarPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), captor.capture());
        return (PageRequest) captor.getValue();
    }

    private void verificarScope(String tipoPerfil, Long perfilId) {
        verify(seguimientoRespuestaRepository).buscarPendientesPaginado(
                isNull(),
                eq(EstadoRespuestaSeguimiento.PENDIENTE),
                isNull(),
                isNull(),
                eq(false),
                eq(tipoPerfil),
                eq(perfilId),
                eq(EstadoConsulta.ARCHIVADO),
                any(Pageable.class));
    }
}
