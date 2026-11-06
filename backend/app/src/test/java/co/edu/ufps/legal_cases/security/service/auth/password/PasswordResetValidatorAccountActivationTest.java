package co.edu.ufps.legal_cases.security.service.auth.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

class PasswordResetValidatorAccountActivationTest {

    private PerfilUsuarioResolverService perfilUsuarioResolverService;
    private PasswordResetValidator validator;

    @BeforeEach
    void setUp() {
        perfilUsuarioResolverService =
                mock(PerfilUsuarioResolverService.class);

        validator =
                new PasswordResetValidator(
                        perfilUsuarioResolverService);
    }

    @Test
    void cuentaPendienteNoPuedeUsarRecuperacion() {
        UsuarioSistema usuario =
                usuarioBase();

        usuario.setEstadoActivacion(
                EstadoActivacionCuenta.PENDIENTE);

        usuario.setPasswordHash(null);

        assertFalse(
                validator.usuarioPuedeRecuperarPassword(
                        usuario));
    }

    @Test
    void cuentaActivaSinPasswordNoPuedeUsarRecuperacion() {
        UsuarioSistema usuario =
                usuarioBase();

        usuario.setEstadoActivacion(
                EstadoActivacionCuenta.ACTIVA);

        usuario.setPasswordHash(null);

        assertFalse(
                validator.usuarioPuedeRecuperarPassword(
                        usuario));
    }

    @Test
    void cuentaActivaConfiguradaMantieneRecuperacion() {
        UsuarioSistema usuario =
                usuarioBase();

        usuario.setEstadoActivacion(
                EstadoActivacionCuenta.ACTIVA);

        usuario.setPasswordHash(
                "$2a$10$synthetic-hash");

        when(perfilUsuarioResolverService
                .tienePerfilActivo(usuario))
                .thenReturn(true);

        assertTrue(
                validator.usuarioPuedeRecuperarPassword(
                        usuario));
    }

    private UsuarioSistema usuarioBase() {
        Rol rol =
                new Rol();

        rol.setActivo(true);

        UsuarioSistema usuario =
                new UsuarioSistema();

        usuario.setId(1L);
        usuario.setUsername(
                "usuario@ufps.edu.co");

        usuario.setActivo(true);
        usuario.setRol(rol);

        return usuario;
    }
}