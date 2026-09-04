package co.edu.ufps.legal_cases.business.service.seguimiento.seguimiento;

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
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResumenDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoResumenProjection;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.AlcanceAlertasDisciplinarias;
import co.edu.ufps.legal_cases.business.service.acceso.seguimiento.SeguimientoAccessService;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class SeguimientoQueryServiceTest {

    private SeguimientoRepository seguimientoRepository;
    private SeguimientoAccessService seguimientoAccessService;
    private SeguimientoMapper seguimientoMapper;
    private SeguimientoValidator seguimientoValidator;
    private SeguimientoQueryService seguimientoQueryService;

    @BeforeEach
    void setUp() {
        seguimientoRepository = mock(SeguimientoRepository.class);
        seguimientoAccessService = mock(SeguimientoAccessService.class);
        seguimientoMapper = mock(SeguimientoMapper.class);
        seguimientoValidator = mock(SeguimientoValidator.class);

        seguimientoQueryService = new SeguimientoQueryService(
                seguimientoRepository,
                seguimientoAccessService,
                seguimientoMapper,
                seguimientoValidator);

        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(true);
    }

    @Test
    void buscarDebeUsarDefaultsValidosYConvertirPaginaPublicaUnoAPageRequestCero() {
        PageRequest esperado = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
        when(seguimientoRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado)))
                .thenReturn(new PageImpl<>(List.of(), esperado, 0));

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verify(seguimientoAccessService).validarTienePermisoVerSeguimientos();
        verify(seguimientoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(esperado));
    }

    @Test
    void debeConservarMetadatosDePageResponseDTOYMapearContenido() {
        SeguimientoResumenProjection projection = mock(SeguimientoResumenProjection.class);
        SeguimientoResumenDTO dto = new SeguimientoResumenDTO();
        PageRequest interno = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("id")));

        when(seguimientoRepository.buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), eq(interno)))
                .thenReturn(new PageImpl<>(List.of(projection), interno, 21));
        when(seguimientoMapper.convertirAResumenDTO(projection)).thenReturn(dto);

        PageResponseDTO<SeguimientoResumenDTO> resultado = seguimientoQueryService.buscarParaUsuarioActual(
                null, 2, 10, "id", "desc", null, null, null, null, null);

        assertEquals(List.of(dto), resultado.content());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
        assertEquals(21, resultado.totalElements());
        assertEquals(3, resultado.totalPages());
    }

    @Test
    void debeNormalizarSearchYTratarBlankComoNull() {
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                "  clave   seguimiento  ", 1, 10, "id", "desc", null, null, null, null, null);

        verify(seguimientoRepository).buscarResumenPaginado(
                eq("clave seguimiento"), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));

        seguimientoQueryService.buscarParaUsuarioActual(
                "   ", 1, 10, "id", "desc", null, null, null, null, null);

        verify(seguimientoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }

    @Test
    void debeRechazarSearchMayorACienCaracteres() {
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        "x".repeat(101), 1, 10, "id", "desc", null, null, null, null, null));

        verify(seguimientoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarPageInvalida() {
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 0, 10, "id", "desc", null, null, null, null, null));

        verify(seguimientoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSizeInvalido() {
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 0, "id", "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 51, "id", "desc", null, null, null, null, null));

        verify(seguimientoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarSortInvalido() {
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "campoInvalido", "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, null, "desc", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "   ", "desc", null, null, null, null, null));

        verify(seguimientoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarDirectionInvalida() {
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "lateral", null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", null, null, null, null, null, null));
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "   ", null, null, null, null, null));

        verify(seguimientoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debeRechazarRangoInvalido() {
        assertThrows(BusinessException.class,
                () -> seguimientoQueryService.buscarParaUsuarioActual(
                        null, 1, 10, "id", "desc", null,
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1),
                        null,
                        null));

        verify(seguimientoRepository, never()).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void debePropagarFiltrosOpcionalesYFechasSobreFechaCreacion() {
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null,
                1,
                10,
                "id",
                "desc",
                EstadoSeguimiento.PENDIENTE,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                7L,
                8L);

        verify(seguimientoRepository).buscarResumenPaginado(
                isNull(),
                eq(EstadoSeguimiento.PENDIENTE),
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                eq(7L),
                eq(8L),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO),
                any(PageRequest.class));
    }

    @Test
    void debeConstruirSortEstableParaCampoDistintoDeId() {
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "categoria", "ASC", null, null, null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(2, ordenes.size());
        assertEquals("categoriaSeguimiento.nombre", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(0).getDirection());
        assertTrue(ordenes.get(0).isIgnoreCase());
        assertEquals("id", ordenes.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, ordenes.get(1).getDirection());
    }

    @Test
    void sortPorIdNoDebeAgregarDesempateDuplicado() {
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        List<Sort.Order> ordenes = capturarPageRequest().getSort().stream().toList();
        assertEquals(1, ordenes.size());
        assertEquals("id", ordenes.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, ordenes.get(0).getDirection());
    }

    @Test
    void administradorDebeUsarAlcanceGlobal() {
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verify(seguimientoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
        verify(seguimientoAccessService, never()).obtenerPerfilActual();
    }

    @Test
    void asesorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ASESOR, 12L);
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verificarScope("ASESOR", 12L);
    }

    @Test
    void monitorDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.MONITOR, 13L);
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verificarScope("MONITOR", 13L);
    }

    @Test
    void estudianteDebePropagarScopeRestringido() {
        stubPerfil(TipoPerfilUsuario.ESTUDIANTE, 14L);
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verificarScope("ESTUDIANTE", 14L);
    }

    @Test
    void conciliadorDebeResolverFailClosed() {
        stubPerfil(TipoPerfilUsuario.CONCILIADOR, 15L);
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verificarScope("CONCILIADOR", 15L);
    }

    @Test
    void perfilNuloONoSoportadoDebeResolverFailClosed() {
        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(seguimientoAccessService.obtenerPerfilActual()).thenReturn(null);
        stubEmpty();

        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verify(seguimientoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), isNull(), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));

        stubPerfil(TipoPerfilUsuario.ADMINISTRATIVO, 16L);
        seguimientoQueryService.buscarParaUsuarioActual(
                null, 1, 10, "id", "desc", null, null, null, null, null);

        verificarScope("ADMINISTRATIVO", 16L);
    }

    @Test
    void projectionDebeMapearADtoCorrecto() {
        SeguimientoMapper mapper = new SeguimientoMapper();
        SeguimientoResumenProjection projection = mock(SeguimientoResumenProjection.class);
        LocalDate fechaEntrega = LocalDate.of(2026, 9, 20);
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 9, 4, 10, 30);
        LocalDateTime fechaActualizacion = LocalDateTime.of(2026, 9, 5, 11, 30);
        when(projection.getId()).thenReturn(30L);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getDescripcion()).thenReturn("Seguimiento civil");
        when(projection.getFechaEntrega()).thenReturn(fechaEntrega);
        when(projection.getDiasNotificacion()).thenReturn(3);
        when(projection.getNotificarPartes()).thenReturn(true);
        when(projection.getNotificarEstudiante()).thenReturn(false);
        when(projection.getAlertaDisciplinaria()).thenReturn(true);
        when(projection.getEstado()).thenReturn(EstadoSeguimiento.PENDIENTE);
        when(projection.getCategoriaSeguimientoId()).thenReturn(5L);
        when(projection.getCategoriaSeguimientoNombre()).thenReturn("Audiencia");
        when(projection.getConsultaId()).thenReturn(6L);
        when(projection.getAutorId()).thenReturn(7L);
        when(projection.getAutorUsername()).thenReturn("asesor@example.test");
        when(projection.getFechaCreacion()).thenReturn(fechaCreacion);
        when(projection.getFechaActualizacion()).thenReturn(fechaActualizacion);

        SeguimientoResumenDTO dto = mapper.convertirAResumenDTO(projection);

        assertEquals(30L, dto.getId());
        assertEquals(4L, dto.getVersion());
        assertEquals("Seguimiento civil", dto.getDescripcion());
        assertEquals(fechaEntrega, dto.getFechaEntrega());
        assertEquals(3, dto.getDiasNotificacion());
        assertTrue(dto.getNotificarPartes());
        assertFalse(dto.getNotificarEstudiante());
        assertTrue(dto.getAlertaDisciplinaria());
        assertEquals(EstadoSeguimiento.PENDIENTE, dto.getEstado());
        assertEquals(5L, dto.getCategoriaSeguimientoId());
        assertEquals("Audiencia", dto.getCategoriaSeguimientoNombre());
        assertEquals(6L, dto.getConsultaId());
        assertEquals(7L, dto.getAutorId());
        assertEquals("asesor@example.test", dto.getAutorUsername());
        assertEquals(fechaCreacion, dto.getFechaCreacion());
        assertEquals(fechaActualizacion, dto.getFechaActualizacion());
    }

    @Test
    void debeUsarConsultaGlobalParaAdministrador() {
        when(seguimientoAccessService.resolverAlcanceAlertasDisciplinarias())
                .thenReturn(AlcanceAlertasDisciplinarias.global());

        when(seguimientoRepository
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarAlertasDisciplinarias();

        verify(seguimientoRepository)
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByAsesorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByMonitorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debeUsarConsultaRestringidaParaAsesor() {
        when(seguimientoAccessService.resolverAlcanceAlertasDisciplinarias())
                .thenReturn(AlcanceAlertasDisciplinarias.asesor(10L));

        when(seguimientoRepository.findAlertasDisciplinariasByAsesorScope(
                10L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarAlertasDisciplinarias();

        verify(seguimientoRepository)
                .findAlertasDisciplinariasByAsesorScope(
                        10L,
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByMonitorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void debeUsarConsultaRestringidaParaMonitor() {
        when(seguimientoAccessService.resolverAlcanceAlertasDisciplinarias())
                .thenReturn(AlcanceAlertasDisciplinarias.monitor(20L));

        when(seguimientoRepository.findAlertasDisciplinariasByMonitorScope(
                20L,
                EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarAlertasDisciplinarias();

        verify(seguimientoRepository)
                .findAlertasDisciplinariasByMonitorScope(
                        20L,
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findByAlertaDisciplinariaTrueAndActivoTrueAndConsulta_EstadoNotOrderByFechaCreacionDesc(
                        EstadoConsulta.ARCHIVADO);

        verify(seguimientoRepository, never())
                .findAlertasDisciplinariasByAsesorScope(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listarCalendarioPorRangoValidoPropagaRepository() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(true);
        when(seguimientoRepository.buscarParaCalendarioPorRangoConScope(
                from, to, true, null, null, EstadoConsulta.ARCHIVADO))
                .thenReturn(List.of());

        seguimientoQueryService.listarCalendarioPorRango(from, to);

        verify(seguimientoRepository).buscarParaCalendarioPorRangoConScope(
                from, to, true, null, null, EstadoConsulta.ARCHIVADO);
        verify(seguimientoRepository, never()).findAll();
    }

    @Test
    void listarCalendarioPorRangoNullFromLanzaExcepcion() {
        LocalDate to = LocalDate.of(2026, 9, 30);
        assertThrows(BusinessException.class, () -> seguimientoQueryService.listarCalendarioPorRango(null, to));
    }

    @Test
    void listarCalendarioPorRangoNullToLanzaExcepcion() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        assertThrows(BusinessException.class, () -> seguimientoQueryService.listarCalendarioPorRango(from, null));
    }

    @Test
    void listarCalendarioPorRangoFromMayorIgualToLanzaExcepcion() {
        LocalDate from = LocalDate.of(2026, 9, 30);
        LocalDate to = LocalDate.of(2026, 9, 1);
        assertThrows(BusinessException.class, () -> seguimientoQueryService.listarCalendarioPorRango(from, to));
        assertThrows(BusinessException.class, () -> seguimientoQueryService.listarCalendarioPorRango(from, from));
    }

    @Test
    void listarCalendarioPorRangoMayor3MesesLanzaExcepcion() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 2);
        assertThrows(BusinessException.class, () -> seguimientoQueryService.listarCalendarioPorRango(from, to));
    }

    @Test
    void listarCalendarioPorRangoPerfilNuloDevuelveVacioSinDelegar() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(seguimientoAccessService.obtenerPerfilActual()).thenReturn(null);

        var result = seguimientoQueryService.listarCalendarioPorRango(from, to);
        assertTrue(result.isEmpty());
        verify(seguimientoRepository, never()).buscarParaCalendarioPorRangoConScope(any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void listarCalendarioPorRangoScopeEstudiantePropagaRepository() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        stubPerfil(TipoPerfilUsuario.ESTUDIANTE, 55L);

        seguimientoQueryService.listarCalendarioPorRango(from, to);

        verify(seguimientoRepository).buscarParaCalendarioPorRangoConScope(
                from, to, false, "ESTUDIANTE", 55L, EstadoConsulta.ARCHIVADO);
    }

    @Test
    void buscarParaAgendaPropagaGlobal() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(true);
        seguimientoQueryService.buscarParaAgenda(from, to);

        verify(seguimientoRepository).buscarParaAgenda(
                from, to, true, null, null, EstadoConsulta.ARCHIVADO);
    }

    @Test
    void buscarParaAgendaPropagaRestringido() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        stubPerfil(TipoPerfilUsuario.ASESOR, 10L);
        seguimientoQueryService.buscarParaAgenda(from, to);

        verify(seguimientoRepository).buscarParaAgenda(
                from, to, false, "ASESOR", 10L, EstadoConsulta.ARCHIVADO);
    }

    @Test
    void buscarParaAgendaPerfilNuloFailClosed() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(seguimientoAccessService.obtenerPerfilActual()).thenReturn(null);

        var result = seguimientoQueryService.buscarParaAgenda(from, to);
        assertTrue(result.isEmpty());
        verify(seguimientoRepository, never()).buscarParaAgenda(any(), any(), anyBoolean(), any(), any(), any());
    }

    private void stubEmpty() {
        when(seguimientoRepository.buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    private void stubPerfil(TipoPerfilUsuario tipoPerfil, Long perfilId) {
        when(seguimientoAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(seguimientoAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(perfilId, tipoPerfil));
    }

    private PageRequest capturarPageRequest() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(seguimientoRepository).buscarResumenPaginado(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    private void verificarScope(String tipoPerfil, Long perfilId) {
        verify(seguimientoRepository).buscarResumenPaginado(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq(tipoPerfil), eq(perfilId), eq(EstadoConsulta.ARCHIVADO), any(PageRequest.class));
    }
}
