package co.edu.ufps.legal_cases.security.service.account.perfil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.account.perfil.estado.PerfilEstadoHandler;
import co.edu.ufps.legal_cases.security.service.account.perfil.estado.PerfilEstadoHandlerRegistry;

class PerfilEstadoServiceTest {

    private PerfilEstadoHandlerRegistry perfilEstadoHandlerRegistry;
    private PerfilEstadoService service;

    @BeforeEach
    void setUp() {
        perfilEstadoHandlerRegistry = mock(PerfilEstadoHandlerRegistry.class);
        service = new PerfilEstadoService(perfilEstadoHandlerRegistry);
    }

    @Test
    void debeDelegarDesactivacionAlHandlerDelTipoDePerfil() {
        PerfilEstadoHandler handler = mock(PerfilEstadoHandler.class);

        when(perfilEstadoHandlerRegistry.obtenerHandler(TipoPerfilUsuario.ASESOR))
                .thenReturn(handler);

        service.desactivarPerfilActual(10L, TipoPerfilUsuario.ASESOR);

        verify(perfilEstadoHandlerRegistry).obtenerHandler(TipoPerfilUsuario.ASESOR);
        verify(handler).desactivarPerfilActual(10L);
    }

    @Test
    void debeRechazarUsuarioSistemaIdNulo() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.desactivarPerfilActual(null, TipoPerfilUsuario.ASESOR));

        assertEquals("El id del usuario del sistema es obligatorio", exception.getMessage());
    }

    @Test
    void debeRechazarTipoPerfilNulo() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.desactivarPerfilActual(10L, null));

        assertEquals("El tipo de perfil actual es obligatorio", exception.getMessage());
    }
}