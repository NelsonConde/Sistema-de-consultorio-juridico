package co.edu.ufps.legal_cases.business.service.agenda;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.agenda.AgendaEventDTO;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionAgendaProjection;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoAgendaProjection;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class AgendaQueryServiceTest {

    private SeguimientoService seguimientoService;
    private ReunionConciliacionRepository reunionRepository;
    private ConciliacionAccessService conciliacionAccessService;
    private UsuarioActualService usuarioActualService;
    private ZoneId institutionalTimeZone;
    private AgendaQueryService agendaQueryService;

    @BeforeEach
    void setUp() {
        seguimientoService = mock(SeguimientoService.class);
        reunionRepository = mock(ReunionConciliacionRepository.class);
        conciliacionAccessService = mock(ConciliacionAccessService.class);
        usuarioActualService = mock(UsuarioActualService.class);
        institutionalTimeZone = ZoneId.of("America/Bogota");

        agendaQueryService = new AgendaQueryService(
                seguimientoService,
                reunionRepository,
                conciliacionAccessService,
                usuarioActualService,
                institutionalTimeZone);
    }

    @Test
    void rechazaRangoNuloOInvertido() {
        LocalDate hoy = LocalDate.now();

        assertThrows(BusinessException.class, () -> agendaQueryService.listar(null, hoy));
        assertThrows(BusinessException.class, () -> agendaQueryService.listar(hoy, null));
        assertThrows(BusinessException.class, () -> agendaQueryService.listar(hoy, hoy));
        assertThrows(BusinessException.class, () -> agendaQueryService.listar(hoy.plusDays(1), hoy));
    }

    @Test
    void rechazaRangoMayorATresMeses() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 2);

        assertThrows(BusinessException.class, () -> agendaQueryService.listar(from, to));
    }

    @Test
    void rechazaAccesoSinPermisos() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(false);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> agendaQueryService.listar(from, to));
    }

    // Bloque B (SCRUM-269): SeguimientoService.buscarParaAgenda(from, to) retorna
    // SeguimientoAgendaProjection. El scope ya está resuelto dentro del servicio.
    @Test
    void listaSeguimientosConPermisoDeSeguimientos() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(true);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(false);

        SeguimientoAgendaProjection seg = seguimientoAgenda(
                10L,
                100L,
                "Entrega de memorial",
                EstadoSeguimiento.PENDIENTE,
                LocalDate.of(2026, 9, 15));

        when(seguimientoService.buscarParaAgenda(from, to)).thenReturn(List.of(seg));

        List<AgendaEventDTO> eventos = agendaQueryService.listar(from, to);

        assertEquals(1, eventos.size());
        AgendaEventDTO evento = eventos.get(0);
        assertEquals("seguimiento-10", evento.id());
        assertEquals("FOLLOW_UP", evento.type());
        assertEquals("Entrega de memorial", evento.title());
        assertEquals(ZoneOffset.ofHours(-5), evento.start().getOffset());
        assertTrue(evento.allDay());
        assertEquals(10L, evento.resourceId());
        assertEquals(100L, evento.consultaId());

        // Verifica que se llama la nueva API y NO la antigua listarParaCalendario
        verify(seguimientoService).buscarParaAgenda(from, to);
        verify(reunionRepository, never()).findAll();
        verify(reunionRepository, never()).buscarParaAgenda(
                any(), any(), anyBoolean(), any(), any(), any());
    }

    // Bloque B (SCRUM-269): overdue se evalúa comparando fechaEntrega con LocalDate.now()
    // dentro de AgendaQueryService — la projection solo expone fechaEntrega.
    @Test
    void evaluaOverdueCorrectamenteSegunZonaHoraria() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 1);

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(true);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(false);

        SeguimientoAgendaProjection segPasado = seguimientoAgenda(
                1L,
                50L,
                "Seguimiento pasado",
                EstadoSeguimiento.PENDIENTE,
                LocalDate.of(2026, 1, 10));

        when(seguimientoService.buscarParaAgenda(from, to)).thenReturn(List.of(segPasado));

        List<AgendaEventDTO> eventos = agendaQueryService.listar(from, to);

        assertEquals(1, eventos.size());
        assertTrue(eventos.get(0).overdue());
    }

    @Test
    void reunionesSeSolicitanAlRepositoryConRangoInclusivoExclusivoYAlcanceGlobal() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ReunionAgendaProjection reunion = reunionAgenda(
                1L,
                201L,
                "REUNION_PROGRAMADA",
                LocalDateTime.of(2026, 9, 10, 10, 0));

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(false);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(true);
        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(true);
        when(reunionRepository.buscarParaAgenda(
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 30, 0, 0)),
                eq(true),
                isNull(),
                isNull(),
                eq(EstadoConsulta.ARCHIVADO)))
                .thenReturn(List.of(reunion));

        List<AgendaEventDTO> eventos = agendaQueryService.listar(from, to);

        assertEquals(1, eventos.size());
        AgendaEventDTO evento = eventos.get(0);
        assertEquals("reunion-1", evento.id());
        assertEquals("CONCILIATION_MEETING", evento.type());
        assertFalse(evento.allDay());
        assertEquals(1L, evento.resourceId());
        assertEquals(201L, evento.consultaId());
        assertEquals("REUNION_PROGRAMADA", evento.status());
        assertEquals(LocalDateTime.of(2026, 9, 10, 10, 0), evento.start().toLocalDateTime());
        assertEquals(ZoneOffset.ofHours(-5), evento.start().getOffset());

        verify(conciliacionAccessService).validarPuedeListarConciliaciones();
        verify(reunionRepository, never()).findAll();
    }

    @Test
    void reunionesPropaganScopeRestringidoYSeMantieneOrdenPorStart() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        ReunionAgendaProjection reunionTardia = reunionAgenda(
                2L,
                202L,
                "REUNION_PROGRAMADA",
                LocalDateTime.of(2026, 9, 20, 14, 0));
        ReunionAgendaProjection reunionTemprana = reunionAgenda(
                1L,
                201L,
                "EN_ESPERA",
                LocalDateTime.of(2026, 9, 10, 10, 0));

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(false);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(true);
        when(conciliacionAccessService.usuarioEsAdministrador()).thenReturn(false);
        when(conciliacionAccessService.obtenerPerfilActual())
                .thenReturn(new PerfilUsuarioActual(77L, TipoPerfilUsuario.CONCILIADOR));
        when(reunionRepository.buscarParaAgenda(
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 30, 0, 0)),
                eq(false),
                eq("CONCILIADOR"),
                eq(77L),
                eq(EstadoConsulta.ARCHIVADO)))
                .thenReturn(List.of(reunionTardia, reunionTemprana));

        List<AgendaEventDTO> eventos = agendaQueryService.listar(from, to);

        assertEquals(List.of("reunion-1", "reunion-2"), eventos.stream()
                .map(AgendaEventDTO::id)
                .toList());
        verify(reunionRepository, never()).findAll();
    }

    // Helper: construye un SeguimientoAgendaProjection anónimo con los campos
    // que AgendaQueryService necesita para construir el AgendaEventDTO.
    private SeguimientoAgendaProjection seguimientoAgenda(
            Long id,
            Long consultaId,
            String descripcion,
            EstadoSeguimiento estado,
            LocalDate fechaEntrega) {
        return new SeguimientoAgendaProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public Long getConsultaId() {
                return consultaId;
            }

            @Override
            public String getDescripcion() {
                return descripcion;
            }

            @Override
            public EstadoSeguimiento getEstado() {
                return estado;
            }

            @Override
            public LocalDate getFechaEntrega() {
                return fechaEntrega;
            }
        };
    }

    private ReunionAgendaProjection reunionAgenda(
            Long conciliacionId,
            Long consultaId,
            String estadoCodigo,
            LocalDateTime fechaReunion) {
        return new ReunionAgendaProjection() {
            @Override
            public Long getConciliacionId() {
                return conciliacionId;
            }

            @Override
            public Long getConsultaId() {
                return consultaId;
            }

            @Override
            public String getEstadoCodigo() {
                return estadoCodigo;
            }

            @Override
            public LocalDateTime getFechaReunion() {
                return fechaReunion;
            }
        };
    }
}
