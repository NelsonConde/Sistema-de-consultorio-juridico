package co.edu.ufps.legal_cases.security.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.auth.password.CambiarPasswordRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.auth.login.LoginMapper;
import co.edu.ufps.legal_cases.security.service.auth.login.LoginValidator;
import co.edu.ufps.legal_cases.security.service.auth.password.CambioPasswordValidator;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;

class AuthServicePasswordSessionTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private LoginMapper loginMapper;
    private LoginValidator loginValidator;
    private CambioPasswordValidator cambioPasswordValidator;
    private UsuarioSistemaMapper usuarioSistemaMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository = mock(UsuarioSistemaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        loginMapper = mock(LoginMapper.class);
        loginValidator = mock(LoginValidator.class);
        cambioPasswordValidator = mock(CambioPasswordValidator.class);
        usuarioSistemaMapper = mock(UsuarioSistemaMapper.class);

        authService = new AuthService(
                usuarioSistemaRepository,
                passwordEncoder,
                jwtService,
                loginMapper,
                loginValidator,
                cambioPasswordValidator,
                usuarioSistemaMapper);
    }

    @Test
    void cambiarPasswordIncrementaVersionYEmiteNuevoToken() {
        String username = "usuario@prueba.test";
        String tokenActual = "jwt-version-4";

        CambiarPasswordRequestDTO dto =
                new CambiarPasswordRequestDTO();

        dto.setPasswordActual("Actual123");
        dto.setPasswordNueva("NuevaClave123");

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(42L);
        usuario.setUsername(username);
        usuario.setPasswordHash("hash-anterior");
        usuario.setAuthVersion(4L);

        when(jwtService.obtenerUsername(tokenActual))
                .thenReturn(username);

        when(usuarioSistemaRepository
                .findIdByUsernameIgnoreCase(username))
                .thenReturn(Optional.of(42L));

        when(usuarioSistemaRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(usuario));

        when(jwtService.obtenerAuthVersion(tokenActual))
                .thenReturn(4L);

        when(passwordEncoder.encode("NuevaClave123"))
                .thenReturn("hash-nuevo");

        when(jwtService.generarToken(username, 5L))
                .thenReturn("jwt-version-5");

        String nuevoToken =
                authService.cambiarPassword(tokenActual, dto);

        assertEquals("jwt-version-5", nuevoToken);
        assertEquals("hash-nuevo", usuario.getPasswordHash());
        assertEquals(5L, usuario.getAuthVersion());

        verify(loginValidator)
                .validarUsuarioPuedeAutenticarse(usuario);

        verify(cambioPasswordValidator)
                .validarDatosCambioPassword(dto);

        verify(cambioPasswordValidator)
                .validarPasswordActual(
                        "Actual123",
                        usuario,
                        passwordEncoder);

        verify(cambioPasswordValidator)
                .validarPasswordNuevaDiferente(
                        "NuevaClave123",
                        usuario,
                        passwordEncoder);

        verify(usuarioSistemaRepository)
                .save(usuario);

        verify(jwtService)
                .generarToken(username, 5L);
    }

    @Test
    void cambiarPasswordRechazaTokenConVersionRevocada() {
        String username = "usuario@prueba.test";
        String tokenRevocado = "jwt-version-3";

        CambiarPasswordRequestDTO dto =
                new CambiarPasswordRequestDTO();

        dto.setPasswordActual("Actual123");
        dto.setPasswordNueva("NuevaClave123");

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(42L);
        usuario.setUsername(username);
        usuario.setPasswordHash("hash-actual");
        usuario.setAuthVersion(4L);

        when(jwtService.obtenerUsername(tokenRevocado))
                .thenReturn(username);

        when(usuarioSistemaRepository
                .findIdByUsernameIgnoreCase(username))
                .thenReturn(Optional.of(42L));

        when(usuarioSistemaRepository.findByIdForUpdate(42L))
                .thenReturn(Optional.of(usuario));

        when(jwtService.obtenerAuthVersion(tokenRevocado))
                .thenReturn(3L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.cambiarPassword(
                        tokenRevocado,
                        dto));

        assertEquals(
                "Token inválido o expirado",
                exception.getMessage());

        verify(cambioPasswordValidator, never())
                .validarDatosCambioPassword(
                        any(CambiarPasswordRequestDTO.class));

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(usuarioSistemaRepository, never())
                .save(any(UsuarioSistema.class));
    }

    @Test
    void cambiarPasswordTieneAuditoriaSinCamposSensibles()
            throws NoSuchMethodException {

        Method method = AuthService.class.getDeclaredMethod(
                "cambiarPassword",
                String.class,
                CambiarPasswordRequestDTO.class);

        Auditable auditable =
                method.getAnnotation(Auditable.class);

        assertNotNull(auditable);

        assertEquals(
                "CAMBIAR_PASSWORD_PROPIA",
                auditable.action());

        assertEquals(
                "UsuarioSistema",
                auditable.entityName());

        assertEquals(
                "'self'",
                auditable.entityId());

        assertEquals(0, auditable.metadata().length);
        assertEquals(0, auditable.trackedFields().length);
    }
}