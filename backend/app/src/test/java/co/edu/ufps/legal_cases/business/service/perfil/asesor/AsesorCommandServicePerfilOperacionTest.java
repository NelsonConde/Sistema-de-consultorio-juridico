package co.edu.ufps.legal_cases.business.service.perfil.asesor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.ufps.legal_cases.business.dto.perfil.AsesorDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AsesorMonitorAccessService;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.PerfilOperacionConflictException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.OperacionEstadoPerfil;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilOperacionEstadoValidator;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;

@ExtendWith(MockitoExtension.class)
class AsesorCommandServicePerfilOperacionTest {

    @Mock private AsesorRepository asesorRepository;
    @Mock private TipoDocumentoRepository tipoDocumentoRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    @Mock private UsuarioSistemaRepository usuarioSistemaRepository;
    @Mock private PerfilOperacionEstadoValidator perfilOperacionEstadoValidator;
    @Mock private AsesorMonitorAccessService asesorMonitorAccessService;
    @Mock private AsesorValidator asesorValidator;
    @Mock private AsesorMapper asesorMapper;
    @Mock private ConsultaResponsableOperacionService consultaResponsableOperacionService;

    @InjectMocks private AsesorCommandService service;

    @Test
    void cambiarEstadoBloqueaCuentaAntesDeRecargarYValidar() {
        UsuarioSistema usuario = usuarioAsesorSuspendido();
        Asesor asesor = asesor(usuario, false);
        AsesorDTO esperado = new AsesorDTO();

        when(asesorRepository.findUsuarioSistemaIdById(10L))
                .thenReturn(Optional.of(20L));
        when(usuarioSistemaRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(usuario));
        when(asesorRepository.findById(10L)).thenReturn(Optional.of(asesor));
        when(asesorRepository.save(asesor)).thenReturn(asesor);
        when(asesorMapper.convertirADTO(asesor)).thenReturn(esperado);

        assertEquals(esperado, service.cambiarEstado(10L, true));

        InOrder orden = inOrder(
                asesorRepository,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                asesorValidator);
        orden.verify(asesorRepository).findUsuarioSistemaIdById(10L);
        orden.verify(usuarioSistemaRepository).findByIdForUpdate(20L);
        orden.verify(asesorRepository).findById(10L);
        orden.verify(perfilOperacionEstadoValidator).validarOperacionDirecta(
                usuario,
                TipoPerfilUsuario.ASESOR,
                OperacionEstadoPerfil.ACTIVAR);
        orden.verify(asesorValidator).validarCambioEstado(asesor, true);
        orden.verify(asesorRepository).save(asesor);

        assertFalse(usuario.getActivo());
        assertEquals(TipoPerfilUsuario.ASESOR, usuario.getTipoPerfilActual());
        verify(consultaResponsableOperacionService, never())
                .validarAsesorSinConsultasOperativas(10L);
    }

    @Test
    void eliminarRechazadoNoEjecutaValidacionesOperativasNiPersiste() {
        UsuarioSistema usuario = usuarioAsesorSuspendido();
        Asesor asesor = asesor(usuario, true);

        when(asesorRepository.findUsuarioSistemaIdById(10L))
                .thenReturn(Optional.of(20L));
        when(usuarioSistemaRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(usuario));
        when(asesorRepository.findById(10L)).thenReturn(Optional.of(asesor));
        org.mockito.Mockito.doThrow(new PerfilOperacionConflictException("conflicto"))
                .when(perfilOperacionEstadoValidator)
                .validarOperacionDirecta(
                        usuario,
                        TipoPerfilUsuario.ASESOR,
                        OperacionEstadoPerfil.DESACTIVAR);

        assertThrows(
                PerfilOperacionConflictException.class,
                () -> service.eliminar(10L));

        InOrder orden = inOrder(
                asesorRepository,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator);
        orden.verify(asesorRepository).findUsuarioSistemaIdById(10L);
        orden.verify(usuarioSistemaRepository).findByIdForUpdate(20L);
        orden.verify(asesorRepository).findById(10L);
        orden.verify(perfilOperacionEstadoValidator).validarOperacionDirecta(
                usuario,
                TipoPerfilUsuario.ASESOR,
                OperacionEstadoPerfil.DESACTIVAR);

        verify(asesorValidator, never()).validarCambioEstado(asesor, false);
        verify(consultaResponsableOperacionService, never())
                .validarAsesorSinConsultasOperativas(10L);
        verify(asesorRepository, never()).save(asesor);
    }

    private UsuarioSistema usuarioAsesorSuspendido() {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(20L);
        usuario.setActivo(false);
        usuario.setTipoPerfilActual(TipoPerfilUsuario.ASESOR);
        return usuario;
    }

    private Asesor asesor(UsuarioSistema usuario, boolean activo) {
        Asesor asesor = new Asesor();
        asesor.setId(10L);
        asesor.setUsuarioSistema(usuario);
        asesor.setActivo(activo);
        return asesor;
    }
}
