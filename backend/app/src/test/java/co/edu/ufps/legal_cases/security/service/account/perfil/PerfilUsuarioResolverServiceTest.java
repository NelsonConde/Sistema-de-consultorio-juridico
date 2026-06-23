package co.edu.ufps.legal_cases.security.service.account.perfil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.resolver.PerfilUsuarioActivoResolver;
import co.edu.ufps.legal_cases.security.service.account.perfil.resolver.PerfilUsuarioActivoResolverRegistry;

class PerfilUsuarioResolverServiceTest {

    private PerfilUsuarioActivoResolverRegistry perfilUsuarioActivoResolverRegistry;
    private PerfilUsuarioResolverService service;

    @BeforeEach
    void setUp() {
        perfilUsuarioActivoResolverRegistry = mock(PerfilUsuarioActivoResolverRegistry.class);
        service = new PerfilUsuarioResolverService(perfilUsuarioActivoResolverRegistry);
    }

    @Test
    void debeDelegarResolucionAlResolverDelTipoDePerfil() {
        UsuarioSistema usuario = usuarioSistema(10L, TipoPerfilUsuario.ASESOR);
        PerfilUsuarioActivoResolver resolver = mock(PerfilUsuarioActivoResolver.class);
        PerfilUsuarioActual perfilEsperado = new PerfilUsuarioActual(5L, TipoPerfilUsuario.ASESOR);

        when(perfilUsuarioActivoResolverRegistry.obtenerResolver(TipoPerfilUsuario.ASESOR))
                .thenReturn(resolver);
        when(resolver.resolver(10L))
                .thenReturn(perfilEsperado);

        PerfilUsuarioActual resultado = service.obtenerPerfilActivoObligatorio(usuario);

        assertEquals(perfilEsperado, resultado);
        verify(perfilUsuarioActivoResolverRegistry).obtenerResolver(TipoPerfilUsuario.ASESOR);
        verify(resolver).resolver(10L);
    }

    @Test
    void debeRetornarTrueSiTienePerfilActivo() {
        UsuarioSistema usuario = usuarioSistema(10L, TipoPerfilUsuario.ESTUDIANTE);
        PerfilUsuarioActivoResolver resolver = mock(PerfilUsuarioActivoResolver.class);

        when(perfilUsuarioActivoResolverRegistry.obtenerResolver(TipoPerfilUsuario.ESTUDIANTE))
                .thenReturn(resolver);
        when(resolver.resolver(10L))
                .thenReturn(new PerfilUsuarioActual(7L, TipoPerfilUsuario.ESTUDIANTE));

        assertTrue(service.tienePerfilActivo(usuario));
    }

    @Test
    void debeRetornarFalseSiNoTienePerfilActivo() {
        UsuarioSistema usuario = usuarioSistema(10L, TipoPerfilUsuario.MONITOR);
        PerfilUsuarioActivoResolver resolver = mock(PerfilUsuarioActivoResolver.class);

        when(perfilUsuarioActivoResolverRegistry.obtenerResolver(TipoPerfilUsuario.MONITOR))
                .thenReturn(resolver);
        when(resolver.resolver(10L))
                .thenThrow(new BusinessException("El monitor asociado al usuario no existe o se encuentra inactivo"));

        assertFalse(service.tienePerfilActivo(usuario));
    }

    @Test
    void debeRechazarUsuarioNulo() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.obtenerPerfilActivoObligatorio(null));

        assertEquals("El usuario del sistema es obligatorio para resolver el perfil", exception.getMessage());
    }

    @Test
    void debeRechazarUsuarioSinId() {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setTipoPerfilActual(TipoPerfilUsuario.ASESOR);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.obtenerPerfilActivoObligatorio(usuario));

        assertEquals("El usuario del sistema es obligatorio para resolver el perfil", exception.getMessage());
    }

    @Test
    void debeRechazarUsuarioSinTipoPerfilActual() {
        UsuarioSistema usuario = usuarioSistema(10L, null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.obtenerPerfilActivoObligatorio(usuario));

        assertEquals("El usuario del sistema no tiene tipo de perfil actual definido", exception.getMessage());
    }

    private UsuarioSistema usuarioSistema(Long id, TipoPerfilUsuario tipoPerfil) {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(id);
        usuario.setTipoPerfilActual(tipoPerfil);
        return usuario;
    }
}