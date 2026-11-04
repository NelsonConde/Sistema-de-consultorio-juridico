package co.edu.ufps.legal_cases.security.service.auth.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

class LoginValidatorAccountActivationTest {

    private PerfilUsuarioResolverService perfilUsuarioResolverService;
    private LoginValidator validator;

    @BeforeEach
    void setUp() {
        perfilUsuarioResolverService =
                mock(PerfilUsuarioResolverService.class);

        validator =
                new LoginValidator(
                        perfilUsuarioResolverService);
    }

    @Test
    void cuentaPendienteNoPuedeAutenticarse() {
        UsuarioSistema usuario =
                usuarioBase();

        usuario.setEstadoActivacion(
                EstadoActivacionCuenta.PENDIENTE);

        usuario.setPasswordHash(null);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validator
                                .validarUsuarioPuedeAutenticarse(
                                        usuario));

        assertEquals(
                "Usuario o contraseña incorrectos",
                exception.getMessage());

        verify(
                perfilUsuarioResolverService,
                never())
                .obtenerPerfilActivoObligatorio(
                        usuario);
    }

    @Test
    void cuentaActivaSinPasswordNoPuedeAutenticarse() {
        UsuarioSistema usuario =
                usuarioBase();

        usuario.setEstadoActivacion(
                EstadoActivacionCuenta.ACTIVA);

        usuario.setPasswordHash(null);

        assertThrows(
                BusinessException.class,
                () -> validator
                        .validarUsuarioPuedeAutenticarse(
                                usuario));

        verify(
                perfilUsuarioResolverService,
                never())
                .obtenerPerfilActivoObligatorio(
                        usuario);
    }

    private UsuarioSistema usuarioBase() {
        Rol rol =
                new Rol();

        rol.setActivo(true);

        UsuarioSistema usuario =
                new UsuarioSistema();

        usuario.setId(1L);
        usuario.setUsername(
                "pendiente@ufps.edu.co");

        usuario.setActivo(true);
        usuario.setRol(rol);

        return usuario;
    }
}