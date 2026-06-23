package co.edu.ufps.legal_cases.security.service.account.usuario;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;

class UsuarioSistemaPerfilEstadoServiceTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private UsuarioSistemaPerfilEstadoService service;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository = mock(UsuarioSistemaRepository.class);
        service = new UsuarioSistemaPerfilEstadoService(usuarioSistemaRepository);
    }

    @Test
    void debeDesactivarUsuarioSistemaCuandoPerfilSeDesactiva() {
        UsuarioSistema usuarioSistema = usuarioSistema(true);

        service.sincronizarEstadoSiExiste(usuarioSistema, false);

        assertFalse(usuarioSistema.getActivo());
        verify(usuarioSistemaRepository).save(usuarioSistema);
    }

    @Test
    void debeReactivarUsuarioSistemaCuandoPerfilSeReactiva() {
        UsuarioSistema usuarioSistema = usuarioSistema(false);

        service.sincronizarEstadoSiExiste(usuarioSistema, true);

        assertTrue(usuarioSistema.getActivo());
        verify(usuarioSistemaRepository).save(usuarioSistema);
    }

    @Test
    void noDebeGuardarSiElEstadoYaEsElMismo() {
        UsuarioSistema usuarioSistema = usuarioSistema(true);

        service.sincronizarEstadoSiExiste(usuarioSistema, true);

        assertTrue(usuarioSistema.getActivo());
        verify(usuarioSistemaRepository, never()).save(usuarioSistema);
    }

    @Test
    void noDebeFallarSiNoHayUsuarioSistemaAsociado() {
        service.sincronizarEstadoSiExiste(null, false);

        verifyNoInteractions(usuarioSistemaRepository);
    }

    @Test
    void noDebeGuardarSiUsuarioSistemaNoTieneId() {
        UsuarioSistema usuarioSistema = new UsuarioSistema();
        usuarioSistema.setActivo(true);

        service.sincronizarEstadoSiExiste(usuarioSistema, false);

        assertTrue(usuarioSistema.getActivo());
        verifyNoInteractions(usuarioSistemaRepository);
    }

    @Test
    void noDebeGuardarSiEstadoNuevoEsNulo() {
        UsuarioSistema usuarioSistema = usuarioSistema(true);

        service.sincronizarEstadoSiExiste(usuarioSistema, null);

        assertTrue(usuarioSistema.getActivo());
        verifyNoInteractions(usuarioSistemaRepository);
    }

    private UsuarioSistema usuarioSistema(Boolean activo) {
        UsuarioSistema usuarioSistema = new UsuarioSistema();
        usuarioSistema.setId(1L);
        usuarioSistema.setActivo(activo);
        return usuarioSistema;
    }
}