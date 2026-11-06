package co.edu.ufps.legal_cases.business.service.perfil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.ufps.legal_cases.business.dto.perfil.ConciliadorDTO;
import co.edu.ufps.legal_cases.business.dto.perfil.MonitorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.ConciliadorAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorMapper;
import co.edu.ufps.legal_cases.business.service.perfil.conciliador.ConciliadorValidator;
import co.edu.ufps.legal_cases.business.service.perfil.monitor.MonitorCommandService;
import co.edu.ufps.legal_cases.business.service.perfil.monitor.MonitorMapper;
import co.edu.ufps.legal_cases.business.service.perfil.monitor.MonitorValidator;
import co.edu.ufps.legal_cases.common.exception.PerfilOperacionConflictException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.OperacionEstadoPerfil;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilOperacionEstadoValidator;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@ExtendWith(MockitoExtension.class)
class PerfilOperacionMonitorConciliadorCommandServiceTest {

    @Mock private MonitorRepository monitorRepository;
    @Mock private ConciliadorRepository conciliadorRepository;
    @Mock private TipoDocumentoRepository tipoDocumentoRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    @Mock private UsuarioSistemaRepository usuarioSistemaRepository;
    @Mock private PerfilOperacionEstadoValidator perfilOperacionEstadoValidator;
    @Mock private AsesorMonitorAccessService asesorMonitorAccessService;
    @Mock private ConciliadorAccessService conciliadorAccessService;
    @Mock private MonitorValidator monitorValidator;
    @Mock private ConciliadorValidator conciliadorValidator;
    @Mock private MonitorMapper monitorMapper;
    @Mock private ConciliadorMapper conciliadorMapper;
    @Mock private ConsultaResponsableOperacionService consultaResponsableOperacionService;

    private MonitorCommandService monitorService;
    private ConciliadorCommandService conciliadorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorCommandService(
                monitorRepository,
                tipoDocumentoRepository,
                sedeRepository,
                usuarioSistemaRegistroService,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                asesorMonitorAccessService,
                monitorValidator,
                monitorMapper,
                consultaResponsableOperacionService);
        conciliadorService = new ConciliadorCommandService(
                conciliadorRepository,
                tipoDocumentoRepository,
                sedeRepository,
                usuarioSistemaRegistroService,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                conciliadorAccessService,
                conciliadorValidator,
                conciliadorMapper);
    }

    @Test
    void monitorBloqueaCuentaAntesDeRecargarYValidar() {
        UsuarioSistema usuario = usuario(20L, TipoPerfilUsuario.MONITOR, false);
        Monitor monitor = monitor(usuario, false);
        MonitorDTO esperado = new MonitorDTO();
        when(monitorRepository.findUsuarioSistemaIdById(10L)).thenReturn(Optional.of(20L));
        when(usuarioSistemaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(usuario));
        when(monitorRepository.findById(10L)).thenReturn(Optional.of(monitor));
        when(monitorRepository.save(monitor)).thenReturn(monitor);
        when(monitorMapper.convertirADTO(monitor)).thenReturn(esperado);

        assertEquals(esperado, monitorService.cambiarEstado(10L, true));

        InOrder orden = inOrder(
                monitorRepository,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                monitorValidator);
        orden.verify(monitorRepository).findUsuarioSistemaIdById(10L);
        orden.verify(usuarioSistemaRepository).findByIdForUpdate(20L);
        orden.verify(monitorRepository).findById(10L);
        orden.verify(perfilOperacionEstadoValidator).validarOperacionDirecta(
                usuario, TipoPerfilUsuario.MONITOR, OperacionEstadoPerfil.ACTIVAR);
        orden.verify(monitorValidator).validarCambioEstado(monitor, true);
        orden.verify(monitorRepository).save(monitor);
        assertFalse(usuario.getActivo());
    }

    @Test
    void conciliadorBloqueaCuentaAntesDeRecargarYValidar() {
        UsuarioSistema usuario = usuario(30L, TipoPerfilUsuario.CONCILIADOR, false);
        Conciliador conciliador = conciliador(usuario, false);
        ConciliadorDTO esperado = new ConciliadorDTO();
        when(conciliadorRepository.findUsuarioSistemaIdById(11L)).thenReturn(Optional.of(30L));
        when(usuarioSistemaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(usuario));
        when(conciliadorRepository.findById(11L)).thenReturn(Optional.of(conciliador));
        when(conciliadorRepository.save(conciliador)).thenReturn(conciliador);
        when(conciliadorMapper.convertirADTO(conciliador)).thenReturn(esperado);

        assertEquals(esperado, conciliadorService.cambiarEstado(11L, true));

        InOrder orden = inOrder(
                conciliadorRepository,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                conciliadorValidator);
        orden.verify(conciliadorRepository).findUsuarioSistemaIdById(11L);
        orden.verify(usuarioSistemaRepository).findByIdForUpdate(30L);
        orden.verify(conciliadorRepository).findById(11L);
        orden.verify(perfilOperacionEstadoValidator).validarOperacionDirecta(
                usuario, TipoPerfilUsuario.CONCILIADOR, OperacionEstadoPerfil.ACTIVAR);
        orden.verify(conciliadorValidator).validarCambioEstado(conciliador, true);
        orden.verify(conciliadorRepository).save(conciliador);
        assertFalse(usuario.getActivo());
    }

    @Test
    void eliminarMonitorRechazadoNoValidaConsultasNiPersiste() {
        UsuarioSistema usuario = usuario(20L, TipoPerfilUsuario.MONITOR, true);
        Monitor monitor = monitor(usuario, true);
        when(monitorRepository.findUsuarioSistemaIdById(10L)).thenReturn(Optional.of(20L));
        when(usuarioSistemaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(usuario));
        when(monitorRepository.findById(10L)).thenReturn(Optional.of(monitor));
        rechazar(usuario, TipoPerfilUsuario.MONITOR);

        assertThrows(PerfilOperacionConflictException.class, () -> monitorService.eliminar(10L));

        verify(monitorValidator, never()).validarCambioEstado(monitor, false);
        verify(consultaResponsableOperacionService, never())
                .validarMonitorSinConsultasOperativas(10L);
        verify(monitorRepository, never()).save(monitor);
    }

    @Test
    void eliminarConciliadorRechazadoNoPersiste() {
        UsuarioSistema usuario = usuario(30L, TipoPerfilUsuario.CONCILIADOR, true);
        Conciliador conciliador = conciliador(usuario, true);
        when(conciliadorRepository.findUsuarioSistemaIdById(11L)).thenReturn(Optional.of(30L));
        when(usuarioSistemaRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(usuario));
        when(conciliadorRepository.findById(11L)).thenReturn(Optional.of(conciliador));
        rechazar(usuario, TipoPerfilUsuario.CONCILIADOR);

        assertThrows(
                PerfilOperacionConflictException.class,
                () -> conciliadorService.eliminar(11L));

        verify(conciliadorValidator, never()).validarCambioEstado(conciliador, false);
        verify(conciliadorRepository, never()).save(conciliador);
    }

    private void rechazar(UsuarioSistema usuario, TipoPerfilUsuario tipoPerfil) {
        org.mockito.Mockito.doThrow(new PerfilOperacionConflictException("conflicto"))
                .when(perfilOperacionEstadoValidator)
                .validarOperacionDirecta(
                        usuario,
                        tipoPerfil,
                        OperacionEstadoPerfil.DESACTIVAR);
    }

    private UsuarioSistema usuario(Long id, TipoPerfilUsuario tipoPerfil, boolean activo) {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(id);
        usuario.setTipoPerfilActual(tipoPerfil);
        usuario.setActivo(activo);
        return usuario;
    }

    private Monitor monitor(UsuarioSistema usuario, boolean activo) {
        Monitor monitor = new Monitor();
        monitor.setId(10L);
        monitor.setUsuarioSistema(usuario);
        monitor.setActivo(activo);
        return monitor;
    }

    private Conciliador conciliador(UsuarioSistema usuario, boolean activo) {
        Conciliador conciliador = new Conciliador();
        conciliador.setId(11L);
        conciliador.setUsuarioSistema(usuario);
        conciliador.setActivo(activo);
        return conciliador;
    }
}
