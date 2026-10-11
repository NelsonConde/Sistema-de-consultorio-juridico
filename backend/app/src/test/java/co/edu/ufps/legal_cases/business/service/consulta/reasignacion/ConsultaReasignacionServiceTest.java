package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaDTO;
import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionDTO;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.service.acceso.consulta.ConsultaAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaEstadoService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaMapper;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaRelacionService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaValidator;
import co.edu.ufps.legal_cases.common.concurrency.ConcurrenciaOptimistaValidator;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.common.exception.ConcurrenciaOptimistaException;
import co.edu.ufps.legal_cases.common.exception.ConsultaReasignacionNoDisponibleException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import jakarta.persistence.EntityManager;

class ConsultaReasignacionServiceTest {

    private ConsultaRepository consultaRepository;
    private ConsultaAccessService consultaAccessService;
    private ConsultaEstadoService consultaEstadoService;
    private ConsultaRelacionService consultaRelacionService;
    private ConsultaValidator consultaValidator;
    private ConsultaMapper consultaMapper;
    private UsuarioActualService usuarioActualService;
    private ConsultaReasignacionHistorialWriter historialWriter;
    private EntityManager entityManager;
    private ConsultaReasignacionService service;

    @BeforeEach
    void setUp() {
        consultaRepository = mock(ConsultaRepository.class);
        consultaAccessService = mock(ConsultaAccessService.class);
        consultaEstadoService = mock(ConsultaEstadoService.class);
        consultaRelacionService = mock(ConsultaRelacionService.class);
        consultaValidator = mock(ConsultaValidator.class);
        consultaMapper = mock(ConsultaMapper.class);
        usuarioActualService = mock(UsuarioActualService.class);
        historialWriter = mock(ConsultaReasignacionHistorialWriter.class);
        entityManager = mock(EntityManager.class);

        service = crearService(Optional.of(historialWriter));
    }

    @ParameterizedTest
    @EnumSource(TipoResponsableConsulta.class)
    void reasignaSoloElRolSolicitadoYRegistraHistorial(
            TipoResponsableConsulta tipoResponsable) {
        Consulta consulta = consultaConResponsables();
        Asesor asesorOriginal = consulta.getAsesor();
        Monitor monitorOriginal = consulta.getMonitor();
        Estudiante estudianteOriginal = consulta.getEstudiante();
        Persona personaOriginal = consulta.getPersona();
        var partesOriginales = consulta.getPartes();
        var contrapartesOriginales = consulta.getContrapartes();
        configurarOperacionExitosa(consulta);

        switch (tipoResponsable) {
            case ASESOR -> {
                Asesor nuevo = asesor(12L);
                when(consultaRelacionService.obtenerAsesor(12L)).thenReturn(nuevo);
            }
            case MONITOR -> {
                Monitor nuevo = monitor(13L);
                when(consultaRelacionService.obtenerMonitor(13L)).thenReturn(nuevo);
            }
            case ESTUDIANTE -> {
                Estudiante nuevo = estudiante(14L);
                when(consultaRelacionService.obtenerEstudiante(14L)).thenReturn(nuevo);
            }
        }

        Long nuevoId = switch (tipoResponsable) {
            case ASESOR -> 12L;
            case MONITOR -> 13L;
            case ESTUDIANTE -> 14L;
        };
        ConsultaDTO respuesta = service.reasignar(
                10L,
                solicitud(tipoResponsable, nuevoId, "  cambio   justificado  ", 3L));

        assertEquals(10L, respuesta.getId());
        assertAll(
                () -> assertSame(personaOriginal, consulta.getPersona()),
                () -> assertSame(partesOriginales, consulta.getPartes()),
                () -> assertSame(contrapartesOriginales, consulta.getContrapartes()),
                () -> assertEquals("Descripcion original", consulta.getDescripcion()),
                () -> assertEquals(EstadoConsulta.ACTIVO, consulta.getEstado()));
        switch (tipoResponsable) {
            case ASESOR -> assertAll(
                    () -> assertEquals(12L, consulta.getAsesor().getId()),
                    () -> assertSame(monitorOriginal, consulta.getMonitor()),
                    () -> assertSame(estudianteOriginal, consulta.getEstudiante()));
            case MONITOR -> assertAll(
                    () -> assertSame(asesorOriginal, consulta.getAsesor()),
                    () -> assertEquals(13L, consulta.getMonitor().getId()),
                    () -> assertSame(estudianteOriginal, consulta.getEstudiante()));
            case ESTUDIANTE -> assertAll(
                    () -> assertSame(asesorOriginal, consulta.getAsesor()),
                    () -> assertSame(monitorOriginal, consulta.getMonitor()),
                    () -> assertEquals(14L, consulta.getEstudiante().getId()));
        }

        ArgumentCaptor<ConsultaReasignacionHistorialWriter.RegistroReasignacion> captor =
                ArgumentCaptor.forClass(
                        ConsultaReasignacionHistorialWriter.RegistroReasignacion.class);
        verify(historialWriter).registrar(captor.capture());
        ConsultaReasignacionHistorialWriter.RegistroReasignacion registro = captor.getValue();

        assertAll(
                () -> assertEquals(10L, registro.consultaId()),
                () -> assertEquals(tipoResponsable, registro.tipoResponsable()),
                () -> assertEquals(idAnterior(tipoResponsable), registro.responsableAnteriorId()),
                () -> assertEquals(nuevoId, registro.responsableNuevoId()),
                () -> assertEquals("cambio justificado", registro.motivo()),
                () -> assertEquals(99L, registro.actorUsuarioId()));
        verify(consultaValidator).validarCompatibilidadResponsables(consulta);
        verify(consultaRepository).save(consulta);
        verify(entityManager).flush();
    }

    @Test
    void bloqueaLaOperacionAntesDeModificarCuandoPb34NoEstaIntegrado() {
        service = crearService(Optional.empty());

        assertThrows(
                ConsultaReasignacionNoDisponibleException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.ASESOR, 12L, "Cambio requerido", 3L)));

        verify(consultaAccessService).validarPuedeReasignarResponsable(10L);
        verifyNoInteractions(consultaRepository, consultaRelacionService, historialWriter, entityManager);
    }

    @Test
    void rechazaActorSinPermisoOAlcanceAntesDeConsultarResponsable() {
        var solicitud = solicitud(TipoResponsableConsulta.MONITOR, 13L, "Cambio requerido", 3L);
        org.mockito.Mockito.doThrow(new AccessDeniedException("Sin alcance"))
                .when(consultaAccessService)
                .validarPuedeReasignarResponsable(10L);

        assertThrows(AccessDeniedException.class, () -> service.reasignar(10L, solicitud));

        verifyNoInteractions(consultaRepository, consultaRelacionService, historialWriter, entityManager);
    }

    @Test
    void rechazaMotivoVacioEnElServicio() {
        assertThrows(
                BusinessException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.ASESOR, 12L, "   ", 3L)));

        verifyNoInteractions(consultaAccessService, consultaRepository, historialWriter);
    }

    @Test
    void rechazaVersionDesactualizadaAntesDeResolverElNuevoResponsable() {
        Consulta consulta = consultaConResponsables();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertThrows(
                ConcurrenciaOptimistaException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.ASESOR, 12L, "Cambio requerido", 2L)));

        verifyNoInteractions(consultaRelacionService, historialWriter, entityManager);
    }

    @Test
    void rechazaConsultaEnEstadoNoOperativo() {
        Consulta consulta = consultaConResponsables();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        org.mockito.Mockito.doThrow(new BusinessException("Consulta cerrada"))
                .when(consultaEstadoService)
                .validarPermiteOperacionOperativa(consulta);

        assertThrows(
                BusinessException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.MONITOR, 13L, "Cambio requerido", 3L)));

        verifyNoInteractions(consultaRelacionService, historialWriter, entityManager);
        verify(consultaRepository, never()).save(any());
    }

    @Test
    void rechazaResponsableInexistenteOInactivo() {
        Consulta consulta = consultaConResponsables();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRelacionService.obtenerEstudiante(14L))
                .thenThrow(new BusinessException("Estudiante no encontrado o inactivo"));

        assertThrows(
                BusinessException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.ESTUDIANTE, 14L, "Cambio requerido", 3L)));

        verifyNoInteractions(historialWriter, entityManager);
        verify(consultaRepository, never()).save(any());
    }

    @Test
    void rechazaResponsableIncompatibleSinEscribirHistorial() {
        Consulta consulta = consultaConResponsables();
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRelacionService.obtenerAsesor(12L)).thenReturn(asesor(12L));
        org.mockito.Mockito.doThrow(new BusinessException("Asesor incompatible"))
                .when(consultaValidator)
                .validarCompatibilidadResponsables(consulta);

        assertThrows(
                BusinessException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.ASESOR, 12L, "Cambio requerido", 3L)));

        verifyNoInteractions(historialWriter, entityManager);
        verify(consultaRepository, never()).save(any());
    }

    @Test
    void propagaFalloDelHistorialAntesDelFlush() {
        Consulta consulta = consultaConResponsables();
        configurarOperacionExitosa(consulta);
        when(consultaRelacionService.obtenerMonitor(13L)).thenReturn(monitor(13L));
        org.mockito.Mockito.doThrow(new IllegalStateException("Fallo de persistencia historica"))
                .when(historialWriter)
                .registrar(any());

        assertThrows(
                IllegalStateException.class,
                () -> service.reasignar(
                        10L,
                        solicitud(TipoResponsableConsulta.MONITOR, 13L, "Cambio requerido", 3L)));

        verify(consultaRepository).save(consulta);
        verify(entityManager, never()).flush();
    }

    private ConsultaReasignacionService crearService(
            Optional<ConsultaReasignacionHistorialWriter> writer) {
        return new ConsultaReasignacionService(
                consultaRepository,
                consultaAccessService,
                consultaEstadoService,
                consultaRelacionService,
                consultaValidator,
                consultaMapper,
                new ConcurrenciaOptimistaValidator(),
                usuarioActualService,
                writer,
                entityManager);
    }

    private void configurarOperacionExitosa(Consulta consulta) {
        UsuarioSistema actor = new UsuarioSistema();
        actor.setId(99L);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.save(consulta)).thenReturn(consulta);
        when(usuarioActualService.obtenerUsuarioActual()).thenReturn(actor);
        when(consultaMapper.convertirADTO(consulta)).thenReturn(respuestaEsperada());
    }

    private Consulta consultaConResponsables() {
        Consulta consulta = new Consulta();
        consulta.setId(10L);
        consulta.setVersion(3L);
        consulta.setEstado(EstadoConsulta.ACTIVO);
        consulta.setDescripcion("Descripcion original");
        consulta.setPersona(new Persona());
        consulta.getPartes().add(new Persona());
        consulta.getContrapartes().add(new Persona());
        consulta.setAsesor(asesor(1L));
        consulta.setMonitor(monitor(2L));
        consulta.setEstudiante(estudiante(3L));
        return consulta;
    }

    private Asesor asesor(Long id) {
        Asesor asesor = new Asesor();
        asesor.setId(id);
        return asesor;
    }

    private Monitor monitor(Long id) {
        Monitor monitor = new Monitor();
        monitor.setId(id);
        return monitor;
    }

    private Estudiante estudiante(Long id) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(id);
        return estudiante;
    }

    private ConsultaReasignacionDTO solicitud(
            TipoResponsableConsulta tipo,
            Long nuevoId,
            String motivo,
            Long version) {
        ConsultaReasignacionDTO solicitud = new ConsultaReasignacionDTO();
        solicitud.setTipoResponsable(tipo);
        solicitud.setNuevoResponsableId(nuevoId);
        solicitud.setMotivo(motivo);
        solicitud.setVersion(version);
        return solicitud;
    }

    private Long idAnterior(TipoResponsableConsulta tipo) {
        return switch (tipo) {
            case ASESOR -> 1L;
            case MONITOR -> 2L;
            case ESTUDIANTE -> 3L;
        };
    }

    private ConsultaDTO respuestaEsperada() {
        ConsultaDTO respuesta = new ConsultaDTO();
        respuesta.setId(10L);
        return respuesta;
    }
}
