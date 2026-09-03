package co.edu.ufps.legal_cases.business.service.agenda;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONCILIACIONES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_SEGUIMIENTOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.agenda.AgendaEventDTO;
import co.edu.ufps.legal_cases.business.dto.seguimiento.SeguimientoResponseDTO;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.conciliacion.ConciliacionAlcanceService;
import co.edu.ufps.legal_cases.business.service.seguimiento.SeguimientoService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class AgendaQueryServiceTest {

    private SeguimientoService seguimientoService;
    private ReunionConciliacionRepository reunionRepository;
    private ConciliacionAccessService conciliacionAccessService;
    private ConciliacionAlcanceService conciliacionAlcanceService;
    private UsuarioActualService usuarioActualService;
    private ZoneId institutionalTimeZone;
    private AgendaQueryService agendaQueryService;

    @BeforeEach
    void setUp() {
        seguimientoService = mock(SeguimientoService.class);
        reunionRepository = mock(ReunionConciliacionRepository.class);
        conciliacionAccessService = mock(ConciliacionAccessService.class);
        conciliacionAlcanceService = mock(ConciliacionAlcanceService.class);
        usuarioActualService = mock(UsuarioActualService.class);
        institutionalTimeZone = ZoneId.of("America/Bogota");

        agendaQueryService = new AgendaQueryService(
                seguimientoService,
                reunionRepository,
                conciliacionAccessService,
                conciliacionAlcanceService,
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

    @Test
    void listaSeguimientosConPermisoDeSeguimientos() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(true);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(false);

        SeguimientoResponseDTO seg = new SeguimientoResponseDTO();
        seg.setId(10L);
        seg.setConsultaId(100L);
        seg.setDescripcion("Entrega de memorial");
        seg.setFechaEntrega(LocalDate.of(2026, 9, 15));

        when(seguimientoService.listarParaCalendario()).thenReturn(List.of(seg));

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

        verify(reunionRepository, never()).findAll();
    }

    @Test
    void evaluaOverdueCorrectamenteSegunZonaHoraria() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 1);

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(true);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(false);

        SeguimientoResponseDTO segPasado = new SeguimientoResponseDTO();
        segPasado.setId(1L);
        segPasado.setFechaEntrega(LocalDate.of(2026, 1, 10));

        when(seguimientoService.listarParaCalendario()).thenReturn(List.of(segPasado));

        List<AgendaEventDTO> eventos = agendaQueryService.listar(from, to);

        assertEquals(1, eventos.size());
        assertTrue(eventos.get(0).overdue());
    }

    @Test
    void filtraReunionesPorAlcanceYExcluyeConsultasArchivadas() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        when(usuarioActualService.tienePermiso(VER_SEGUIMIENTOS)).thenReturn(false);
        when(usuarioActualService.tienePermiso(VER_CONCILIACIONES)).thenReturn(true);

        Consulta consultaActiva = new Consulta();
        consultaActiva.setId(201L);
        consultaActiva.setEstado(EstadoConsulta.EN_PROCESO);

        Consulta consultaArchivada = new Consulta();
        consultaArchivada.setId(202L);
        consultaArchivada.setEstado(EstadoConsulta.ARCHIVADO);

        Conciliacion concVisible = new Conciliacion();
        concVisible.setId(1L);
        concVisible.setConsulta(consultaActiva);
        EstadoConciliacion estadoConc = new EstadoConciliacion();
        estadoConc.setCodigo("PROGRAMADA");
        concVisible.setEstado(estadoConc);

        Conciliacion concNoVisible = new Conciliacion();
        concNoVisible.setId(2L);
        concNoVisible.setConsulta(consultaActiva);

        Conciliacion concArchivada = new Conciliacion();
        concArchivada.setId(3L);
        concArchivada.setConsulta(consultaArchivada);

        ReunionConciliacion r1 = new ReunionConciliacion();
        r1.setConciliacionId(1L);
        r1.setConciliacion(concVisible);
        r1.setFechaReunion(LocalDateTime.of(2026, 9, 10, 10, 0));

        ReunionConciliacion r2 = new ReunionConciliacion();
        r2.setConciliacionId(2L);
        r2.setConciliacion(concNoVisible);
        r2.setFechaReunion(LocalDateTime.of(2026, 9, 12, 14, 0));

        ReunionConciliacion r3 = new ReunionConciliacion();
        r3.setConciliacionId(3L);
        r3.setConciliacion(concArchivada);
        r3.setFechaReunion(LocalDateTime.of(2026, 9, 14, 9, 0));

        when(reunionRepository.findAll()).thenReturn(List.of(r1, r2, r3));
        when(conciliacionAlcanceService.puedeVerConciliacion(concVisible)).thenReturn(true);
        when(conciliacionAlcanceService.puedeVerConciliacion(concNoVisible)).thenReturn(false);

        List<AgendaEventDTO> eventos = agendaQueryService.listar(from, to);

        assertEquals(1, eventos.size());
        AgendaEventDTO evento = eventos.get(0);
        assertEquals("reunion-1", evento.id());
        assertEquals("CONCILIATION_MEETING", evento.type());
        assertEquals("Reunión de conciliación", evento.title());
        assertEquals(ZoneOffset.ofHours(-5), evento.start().getOffset());
        assertEquals(LocalDateTime.of(2026, 9, 10, 10, 0), evento.start().toLocalDateTime());
        assertFalse(evento.allDay());
        assertEquals(1L, evento.resourceId());
        assertEquals(201L, evento.consultaId());
        assertEquals("PROGRAMADA", evento.status());

        verify(conciliacionAccessService).validarPuedeListarConciliaciones();
    }
}
