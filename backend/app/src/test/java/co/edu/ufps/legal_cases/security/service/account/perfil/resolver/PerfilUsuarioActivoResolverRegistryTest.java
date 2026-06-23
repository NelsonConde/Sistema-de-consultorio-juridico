package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

class PerfilUsuarioActivoResolverRegistryTest {

    @Test
    void debeObtenerResolverRegistradoPorTipoPerfil() {
        PerfilUsuarioActivoResolver asesorResolver = new ResolverPrueba(TipoPerfilUsuario.ASESOR);
        PerfilUsuarioActivoResolver estudianteResolver = new ResolverPrueba(TipoPerfilUsuario.ESTUDIANTE);

        PerfilUsuarioActivoResolverRegistry registry = new PerfilUsuarioActivoResolverRegistry(
                List.of(asesorResolver, estudianteResolver));

        assertSame(asesorResolver, registry.obtenerResolver(TipoPerfilUsuario.ASESOR));
        assertSame(estudianteResolver, registry.obtenerResolver(TipoPerfilUsuario.ESTUDIANTE));
    }

    @Test
    void debeRechazarTipoPerfilNulo() {
        PerfilUsuarioActivoResolverRegistry registry = new PerfilUsuarioActivoResolverRegistry(
                List.of(new ResolverPrueba(TipoPerfilUsuario.ASESOR)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registry.obtenerResolver(null));

        assertEquals("El tipo de perfil es obligatorio", exception.getMessage());
    }

    @Test
    void debeRechazarTipoPerfilSinResolver() {
        PerfilUsuarioActivoResolverRegistry registry = new PerfilUsuarioActivoResolverRegistry(
                List.of(new ResolverPrueba(TipoPerfilUsuario.ASESOR)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registry.obtenerResolver(TipoPerfilUsuario.MONITOR));

        assertEquals(
                "No existe resolver para el perfil MONITOR",
                exception.getMessage());
    }

    @Test
    void debeRechazarResolversDuplicadosParaElMismoPerfil() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new PerfilUsuarioActivoResolverRegistry(
                        List.of(
                                new ResolverPrueba(TipoPerfilUsuario.ASESOR),
                                new ResolverPrueba(TipoPerfilUsuario.ASESOR))));

        assertEquals(
                "Hay más de un resolver registrado para el perfil ASESOR",
                exception.getMessage());
    }

    private static class ResolverPrueba implements PerfilUsuarioActivoResolver {

        private final TipoPerfilUsuario tipoPerfil;

        private ResolverPrueba(TipoPerfilUsuario tipoPerfil) {
            this.tipoPerfil = tipoPerfil;
        }

        @Override
        public TipoPerfilUsuario getTipoPerfil() {
            return tipoPerfil;
        }

        @Override
        public PerfilUsuarioActual resolver(Long usuarioSistemaId) {
            return new PerfilUsuarioActual(1L, tipoPerfil);
        }
    }
}