package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class PerfilEstadoHandlerRegistryTest {

    @Test
    void debeObtenerHandlerRegistradoPorTipoPerfil() {
        PerfilEstadoHandler asesorHandler = new HandlerPrueba(TipoPerfilUsuario.ASESOR);
        PerfilEstadoHandler monitorHandler = new HandlerPrueba(TipoPerfilUsuario.MONITOR);

        PerfilEstadoHandlerRegistry registry = new PerfilEstadoHandlerRegistry(
                List.of(asesorHandler, monitorHandler));

        assertSame(asesorHandler, registry.obtenerHandler(TipoPerfilUsuario.ASESOR));
        assertSame(monitorHandler, registry.obtenerHandler(TipoPerfilUsuario.MONITOR));
    }

    @Test
    void debeRechazarTipoPerfilNulo() {
        PerfilEstadoHandlerRegistry registry = new PerfilEstadoHandlerRegistry(
                List.of(new HandlerPrueba(TipoPerfilUsuario.ASESOR)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registry.obtenerHandler(null));

        assertEquals("El tipo de perfil es obligatorio", exception.getMessage());
    }

    @Test
    void debeRechazarTipoPerfilSinHandler() {
        PerfilEstadoHandlerRegistry registry = new PerfilEstadoHandlerRegistry(
                List.of(new HandlerPrueba(TipoPerfilUsuario.ASESOR)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registry.obtenerHandler(TipoPerfilUsuario.ESTUDIANTE));

        assertEquals(
                "No existe handler de estado para el perfil ESTUDIANTE",
                exception.getMessage());
    }

    @Test
    void debeRechazarHandlersDuplicadosParaElMismoPerfil() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new PerfilEstadoHandlerRegistry(
                        List.of(
                                new HandlerPrueba(TipoPerfilUsuario.ASESOR),
                                new HandlerPrueba(TipoPerfilUsuario.ASESOR))));

        assertEquals(
                "Hay más de un handler de estado registrado para el perfil ASESOR",
                exception.getMessage());
    }

    private static class HandlerPrueba implements PerfilEstadoHandler {

        private final TipoPerfilUsuario tipoPerfil;

        private HandlerPrueba(TipoPerfilUsuario tipoPerfil) {
            this.tipoPerfil = tipoPerfil;
        }

        @Override
        public TipoPerfilUsuario getTipoPerfil() {
            return tipoPerfil;
        }

        @Override
        public void desactivarPerfilActual(Long usuarioSistemaId) {
            // Handler de prueba.
        }
    }
}