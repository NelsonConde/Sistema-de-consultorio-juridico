package co.edu.ufps.legal_cases.business.service.perfil.administrativo;

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

import co.edu.ufps.legal_cases.business.dto.perfil.AdministrativoDTO;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.service.acceso.perfil.AdministrativoAccessService;
import co.edu.ufps.legal_cases.common.exception.PerfilOperacionConflictException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.OperacionEstadoPerfil;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilOperacionEstadoValidator;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaRegistroService;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantService;

@ExtendWith(MockitoExtension.class)
class AdministrativoCommandServicePerfilOperacionTest {

    @Mock private AdministrativoRepository administrativoRepository;
    @Mock private TipoDocumentoRepository tipoDocumentoRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioSistemaRegistroService usuarioSistemaRegistroService;
    @Mock private UsuarioSistemaRepository usuarioSistemaRepository;
    @Mock private PerfilOperacionEstadoValidator perfilOperacionEstadoValidator;
    @Mock private AdministrativoAccessService administrativoAccessService;
    @Mock private AdministrativoValidator administrativoValidator;
    @Mock private AdministrativoMapper administrativoMapper;
    @Mock private AdministracionInvariantService administracionInvariantService;

    private AdministrativoCommandService service;

    @BeforeEach
    void setUp() {
        service = new AdministrativoCommandService(
                administrativoRepository,
                tipoDocumentoRepository,
                sedeRepository,
                usuarioSistemaRegistroService,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                administrativoAccessService,
                administrativoValidator,
                administrativoMapper,
                administracionInvariantService);
    }

    @Test
    void activacionBloqueaCuentaAntesDeRecargarYValidarSinHabilitarla() {
        UsuarioSistema usuario = usuarioSuspendido();
        Administrativo administrativo = administrativo(usuario, false);
        AdministrativoDTO esperado = new AdministrativoDTO();

        when(administrativoRepository.findUsuarioSistemaIdById(10L))
                .thenReturn(Optional.of(20L));
        when(usuarioSistemaRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(usuario));
        when(administrativoRepository.findById(10L))
                .thenReturn(Optional.of(administrativo));
        when(administrativoRepository.save(administrativo))
                .thenReturn(administrativo);
        when(administrativoMapper.convertirADTO(administrativo))
                .thenReturn(esperado);

        assertEquals(esperado, service.cambiarEstado(10L, true));

        InOrder orden = inOrder(
                administrativoRepository,
                usuarioSistemaRepository,
                perfilOperacionEstadoValidator,
                administrativoValidator);
        orden.verify(administrativoRepository).findUsuarioSistemaIdById(10L);
        orden.verify(usuarioSistemaRepository).findByIdForUpdate(20L);
        orden.verify(administrativoRepository).findById(10L);
        orden.verify(perfilOperacionEstadoValidator).validarOperacionDirecta(
                usuario,
                TipoPerfilUsuario.ADMINISTRATIVO,
                OperacionEstadoPerfil.ACTIVAR);
        orden.verify(administrativoValidator)
                .validarCambioEstado(administrativo, true);
        orden.verify(administrativoRepository).save(administrativo);

        assertFalse(usuario.getActivo());
        assertEquals(TipoPerfilUsuario.ADMINISTRATIVO,
                usuario.getTipoPerfilActual());
        verify(administracionInvariantService, never())
                .validarCambioEstadoAdministrativo(10L, true);
    }

    @Test
    void deleteVinculadoRechazadoNoAdquiereBloqueoDeContinuidadNiPersiste() {
        UsuarioSistema usuario = usuarioSuspendido();
        Administrativo administrativo = administrativo(usuario, true);

        when(administrativoRepository.findUsuarioSistemaIdById(10L))
                .thenReturn(Optional.of(20L));
        when(usuarioSistemaRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(usuario));
        when(administrativoRepository.findById(10L))
                .thenReturn(Optional.of(administrativo));
        org.mockito.Mockito.doThrow(
                        new PerfilOperacionConflictException("conflicto"))
                .when(perfilOperacionEstadoValidator)
                .validarOperacionDirecta(
                        usuario,
                        TipoPerfilUsuario.ADMINISTRATIVO,
                        OperacionEstadoPerfil.DESACTIVAR);

        assertThrows(
                PerfilOperacionConflictException.class,
                () -> service.eliminar(10L));

        verify(administracionInvariantService, never())
                .validarEliminacionAdministrativo(10L);
        verify(administrativoValidator, never())
                .validarCambioEstado(administrativo, false);
        verify(administrativoRepository, never()).save(administrativo);
    }

    @Test
    void registroSinCuentaConservaDesactivacionPreviaBajoGuardAdministrativo() {
        Administrativo administrativo = administrativo(null, true);
        AdministrativoDTO esperado = new AdministrativoDTO();

        when(administrativoRepository.findUsuarioSistemaIdById(10L))
                .thenReturn(Optional.empty());
        when(administrativoRepository.findById(10L))
                .thenReturn(Optional.of(administrativo));
        when(administrativoRepository.save(administrativo))
                .thenReturn(administrativo);
        when(administrativoMapper.convertirADTO(administrativo))
                .thenReturn(esperado);

        assertEquals(esperado, service.cambiarEstado(10L, false));

        InOrder orden = inOrder(
                administrativoRepository,
                administracionInvariantService,
                administrativoValidator);
        orden.verify(administrativoRepository).findUsuarioSistemaIdById(10L);
        orden.verify(administracionInvariantService)
                .validarCambioEstadoAdministrativo(10L, false);
        orden.verify(administrativoRepository).findById(10L);
        orden.verify(administrativoValidator)
                .validarCambioEstado(administrativo, false);
        orden.verify(administrativoRepository).save(administrativo);

        verify(usuarioSistemaRepository, never()).findByIdForUpdate(20L);
        verify(perfilOperacionEstadoValidator, never())
                .validarOperacionDirecta(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        assertFalse(administrativo.getActivo());
    }

    private UsuarioSistema usuarioSuspendido() {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(20L);
        usuario.setActivo(false);
        usuario.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        return usuario;
    }

    private Administrativo administrativo(
            UsuarioSistema usuario,
            boolean activo) {
        Administrativo administrativo = new Administrativo();
        administrativo.setId(10L);
        administrativo.setUsuarioSistema(usuario);
        administrativo.setActivo(activo);
        return administrativo;
    }
}
