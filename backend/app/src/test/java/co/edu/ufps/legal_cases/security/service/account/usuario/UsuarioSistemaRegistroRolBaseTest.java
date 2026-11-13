package co.edu.ufps.legal_cases.security.service.account.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.access.RolRepository;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

class UsuarioSistemaRegistroRolBaseTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private RolRepository rolRepository;
    private ApplicationEventPublisher eventPublisher;

    private UsuarioSistemaRegistroService service;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository =
                mock(UsuarioSistemaRepository.class);

        rolRepository =
                mock(RolRepository.class);

        eventPublisher =
                mock(ApplicationEventPublisher.class);

        service = new UsuarioSistemaRegistroService(
                usuarioSistemaRepository,
                rolRepository,
                eventPublisher);

        AtomicLong sequence = new AtomicLong(1L);

        when(usuarioSistemaRepository.save(any(UsuarioSistema.class)))
                .thenAnswer(invocation -> {
                    UsuarioSistema usuario = invocation.getArgument(0);
                    usuario.setId(sequence.getAndIncrement());
                    return usuario;
                });
    }

    @Test
    void creaAdministrativoConRolBaseRenombrado() {
        Rol rolRenombrado = new Rol();
        rolRenombrado.setId(1L);
        rolRenombrado.setNombre("Gestor institucional");
        rolRenombrado.setActivo(true);
        rolRenombrado.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        rolRenombrado.setCodigoBase(CodigoRolBase.ADMINISTRADOR);

        when(rolRepository.findByCodigoBaseAndActivoTrue(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.of(rolRenombrado));

        when(usuarioSistemaRepository.existsByUsernameIgnoreCase(
                "admin.prueba@ufps.edu.co"))
                .thenReturn(false);

        Administrativo administrativo =
                mock(Administrativo.class);

        when(administrativo.getId())
                .thenReturn(100L);

        when(administrativo.getEmail())
                .thenReturn("admin.prueba@ufps.edu.co");

        when(administrativo.getDocumento())
                .thenReturn("1090000000");

        when(administrativo.getUsuarioSistema())
                .thenReturn(null);

        UsuarioSistema creado =
                service.crearParaAdministrativo(administrativo);

        assertSame(
                rolRenombrado,
                creado.getRol());

        assertEquals(
                TipoPerfilUsuario.ADMINISTRATIVO,
                creado.getTipoPerfilActual());

        assertEquals(
                "admin.prueba@ufps.edu.co",
                creado.getUsername());

        assertNull(creado.getPasswordHash());

        assertEquals(
                EstadoActivacionCuenta.PENDIENTE,
                creado.getEstadoActivacion());

        assertTrue(
                creado.getActivo());

        verify(rolRepository)
                .findByCodigoBaseAndActivoTrue(
                        CodigoRolBase.ADMINISTRADOR);

        verify(usuarioSistemaRepository)
                .save(creado);

        verify(eventPublisher)
                .publishEvent(
                        new UsuarioSistemaRegistradoEvent(
                                creado.getId(),
                                creado.getUsername()));
    }

    @Test
    void publicaInvitacionInicialParaLosCincoTiposDePerfil() {
        configurarRol(CodigoRolBase.ESTUDIANTE);
        configurarRol(CodigoRolBase.ASESOR);
        configurarRol(CodigoRolBase.MONITOR);
        configurarRol(CodigoRolBase.ADMINISTRADOR);
        configurarRol(CodigoRolBase.CONCILIADOR);

        Estudiante estudiante = mock(Estudiante.class);
        when(estudiante.getId()).thenReturn(1L);
        when(estudiante.getEmail()).thenReturn("estudiante@ufps.edu.co");

        Asesor asesor = mock(Asesor.class);
        when(asesor.getId()).thenReturn(2L);
        when(asesor.getEmail()).thenReturn("asesor@ufps.edu.co");

        Monitor monitor = mock(Monitor.class);
        when(monitor.getId()).thenReturn(3L);
        when(monitor.getEmail()).thenReturn("monitor@ufps.edu.co");

        Administrativo administrativo = mock(Administrativo.class);
        when(administrativo.getId()).thenReturn(4L);
        when(administrativo.getEmail()).thenReturn("administrativo@ufps.edu.co");

        Conciliador conciliador = mock(Conciliador.class);
        when(conciliador.getId()).thenReturn(5L);
        when(conciliador.getEmail()).thenReturn("conciliador@ufps.edu.co");

        service.crearParaEstudiante(estudiante);
        service.crearParaAsesor(asesor);
        service.crearParaMonitor(monitor);
        service.crearParaAdministrativo(administrativo);
        service.crearParaConciliador(conciliador);

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(5))
                .publishEvent(events.capture());

        assertEquals(
                java.util.List.of(
                        "estudiante@ufps.edu.co",
                        "asesor@ufps.edu.co",
                        "monitor@ufps.edu.co",
                        "administrativo@ufps.edu.co",
                        "conciliador@ufps.edu.co"),
                events.getAllValues().stream()
                        .map(UsuarioSistemaRegistradoEvent.class::cast)
                        .map(UsuarioSistemaRegistradoEvent::email)
                        .toList());
    }

    @Test
    void rechazaRolBaseConTipoPerfilIncompatible() {
        Rol rolInconsistente = new Rol();
        rolInconsistente.setId(1L);
        rolInconsistente.setNombre("Gestor institucional");
        rolInconsistente.setActivo(true);
        rolInconsistente.setCodigoBase(
                CodigoRolBase.ADMINISTRADOR);

        /*
         * Estado inconsistente creado intencionalmente para comprobar
         * que la capa de negocio no confía únicamente en la base de datos.
         */
        rolInconsistente.setTipoPerfil(
                TipoPerfilUsuario.ASESOR);

        when(rolRepository.findByCodigoBaseAndActivoTrue(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.of(rolInconsistente));

        when(usuarioSistemaRepository.existsByUsernameIgnoreCase(
                "admin.prueba@ufps.edu.co"))
                .thenReturn(false);

        Administrativo administrativo =
                mock(Administrativo.class);

        when(administrativo.getId())
                .thenReturn(100L);

        when(administrativo.getEmail())
                .thenReturn("admin.prueba@ufps.edu.co");

        when(administrativo.getDocumento())
                .thenReturn("1090000000");

        when(administrativo.getUsuarioSistema())
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.crearParaAdministrativo(administrativo));

        assertTrue(
                exception.getMessage()
                        .contains("ADMINISTRADOR"));

        assertTrue(
                exception.getMessage()
                        .contains("ADMINISTRATIVO"));

        verify(usuarioSistemaRepository, never())
                .save(any(UsuarioSistema.class));
    }

    @Test
    void rechazaCreacionCuandoRolBaseNoExisteOEstaInactivo() {
        when(rolRepository.findByCodigoBaseAndActivoTrue(
                CodigoRolBase.ADMINISTRADOR))
                .thenReturn(Optional.empty());

        when(usuarioSistemaRepository.existsByUsernameIgnoreCase(
                "admin.prueba@ufps.edu.co"))
                .thenReturn(false);

        Administrativo administrativo =
                mock(Administrativo.class);

        when(administrativo.getId())
                .thenReturn(100L);

        when(administrativo.getEmail())
                .thenReturn("admin.prueba@ufps.edu.co");

        when(administrativo.getDocumento())
                .thenReturn("1090000000");

        when(administrativo.getUsuarioSistema())
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.crearParaAdministrativo(administrativo));

        assertTrue(
                exception.getMessage()
                        .contains("ADMINISTRADOR"));

        verify(usuarioSistemaRepository, never())
                .save(any(UsuarioSistema.class));
    }

    private void configurarRol(CodigoRolBase codigoRolBase) {
        Rol rol = new Rol();
        rol.setId((long) codigoRolBase.ordinal() + 1L);
        rol.setNombre(codigoRolBase.name());
        rol.setActivo(true);
        rol.setCodigoBase(codigoRolBase);
        rol.setTipoPerfil(codigoRolBase.getTipoPerfil());

        when(rolRepository.findByCodigoBaseAndActivoTrue(codigoRolBase))
                .thenReturn(Optional.of(rol));
    }
}
