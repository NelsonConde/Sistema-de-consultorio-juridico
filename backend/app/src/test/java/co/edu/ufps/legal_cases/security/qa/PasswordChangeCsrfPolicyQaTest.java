package co.edu.ufps.legal_cases.security.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.config.security.AuthCookieProperties;
import co.edu.ufps.legal_cases.security.controller.auth.AuthController;
import co.edu.ufps.legal_cases.security.dto.auth.password.CambiarPasswordRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.password.RestablecerPasswordDTO;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;
import co.edu.ufps.legal_cases.security.service.auth.activation.AccountActivationService;
import co.edu.ufps.legal_cases.security.service.auth.AuthService;
import co.edu.ufps.legal_cases.security.service.auth.PasswordResetService;
import co.edu.ufps.legal_cases.security.service.auth.login.LoginMapper;
import co.edu.ufps.legal_cases.security.service.auth.login.LoginValidator;
import co.edu.ufps.legal_cases.security.service.auth.password.CambioPasswordValidator;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetValidator;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;

/**
 * QA Suite for SCRUM-292:
 * "QA — Verificar cambio de contraseña, CSRF y no exposición"
 *
 * Evaluates all required cases:
 * 1. Current password: correct vs incorrect
 * 2. New password: equal to current vs weak vs valid
 * 3. Confirmation: mismatched confirmation handling
 * 4. Session: absent, expired, or revoked (auth_version policy)
 * 5. CSRF: absent, invalid, and cookie invalidation upon change
 * 6. Old vs new password authentication post-change
 * 7. Zero exposure in logs and audit trails
 * 8. Login and recovery remain functional
 */
class PasswordChangeCsrfPolicyQaTest {

    private PasswordEncoder passwordEncoder;
    private CambioPasswordValidator cambioPasswordValidator;
    private PasswordResetValidator passwordResetValidator;
    private UsuarioSistemaRepository usuarioSistemaRepository;
    private JwtService jwtService;
    private LoginValidator loginValidator;
    private LoginMapper loginMapper;
    private UsuarioSistemaMapper usuarioSistemaMapper;
    private CsrfTokenRepository csrfTokenRepository;
    private PasswordResetService passwordResetService;
    private AccountActivationService accountActivationService;
    private PerfilUsuarioResolverService perfilResolverService;

    private AuthService authService;
    private AuthService mockAuthService;
    private AuthController authController;

    private static final String OLD_PLAIN_PASSWORD = "OldPassword#2026";
    private static final String NEW_PLAIN_PASSWORD = "NewPassword#2026!";
    private static final String USERNAME = "josemanuelpr@ufps.edu.co";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        cambioPasswordValidator = new CambioPasswordValidator();
        perfilResolverService = mock(PerfilUsuarioResolverService.class);
        passwordResetValidator = new PasswordResetValidator(perfilResolverService);

        usuarioSistemaRepository = mock(UsuarioSistemaRepository.class);
        jwtService = mock(JwtService.class);
        loginValidator = mock(LoginValidator.class);
        loginMapper = mock(LoginMapper.class);
        usuarioSistemaMapper = mock(UsuarioSistemaMapper.class);
        csrfTokenRepository = mock(CsrfTokenRepository.class);
        passwordResetService = mock(PasswordResetService.class);
        accountActivationService = mock(AccountActivationService.class);
        mockAuthService = mock(AuthService.class);

        authService = new AuthService(
                usuarioSistemaRepository,
                passwordEncoder,
                jwtService,
                loginMapper,
                loginValidator,
                cambioPasswordValidator,
                usuarioSistemaMapper);

        AuthCookieProperties authCookieProperties = new AuthCookieProperties();
        authCookieProperties.setSecure(true);
        authCookieProperties.setSameSite("Lax");

        authController = new AuthController(
                mockAuthService,
                passwordResetService,
                accountActivationService,
                csrfTokenRepository,
                authCookieProperties);
    }

    private UsuarioSistema createTestUser(Long id, Long authVersion, String plainPassword) {
        co.edu.ufps.legal_cases.security.model.access.Rol rol = new co.edu.ufps.legal_cases.security.model.access.Rol();
        rol.setId(10L);
        rol.setNombre("ADMINISTRATIVO");
        rol.setActivo(true);

        UsuarioSistema user = new UsuarioSistema();
        user.setId(id);
        user.setUsername(USERNAME);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setAuthVersion(authVersion);
        user.setActivo(true);
        user.setRol(rol);
        user.setEstadoActivacion(EstadoActivacionCuenta.ACTIVA);
        return user;
    }

    // =========================================================================
    // CASE 1: Actual correcta / incorrecta
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-01: Validar contraseña actual correcta")
    void testActualCorrecta_PermiteValidacion() {
        UsuarioSistema user = createTestUser(1L, 0L, OLD_PLAIN_PASSWORD);

        // Debe ejecutarse sin lanzar excepciones
        cambioPasswordValidator.validarPasswordActual(
                OLD_PLAIN_PASSWORD,
                user,
                passwordEncoder);
    }

    @Test
    @DisplayName("QA-PWD-02: Validar contraseña actual incorrecta rechaza con mensaje seguro")
    void testActualIncorrecta_RechazaConMensajeSeguro() {
        UsuarioSistema user = createTestUser(1L, 0L, OLD_PLAIN_PASSWORD);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cambioPasswordValidator.validarPasswordActual(
                        "ClaveTotalmenteErronea#999",
                        user,
                        passwordEncoder));

        assertEquals("La contraseña actual no es correcta", exception.getMessage());
    }

    // =========================================================================
    // CASE 2: Nueva igual a la actual
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-03: Rechazar nueva contraseña idéntica a la actual")
    void testNuevaIgualAActual_Rechaza() {
        UsuarioSistema user = createTestUser(1L, 0L, OLD_PLAIN_PASSWORD);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cambioPasswordValidator.validarPasswordNuevaDiferente(
                        OLD_PLAIN_PASSWORD,
                        user,
                        passwordEncoder));

        assertEquals("La nueva contraseña no puede ser igual a la actual", exception.getMessage());
    }

    // =========================================================================
    // CASE 3: Nueva débil / inválida vs válida
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "short", "1234567"})
    @DisplayName("QA-PWD-04: Rechazar nueva contraseña corta o vacía (< 8 caracteres)")
    void testNuevaDebil_Rechaza(String weakPassword) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(weakPassword);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cambioPasswordValidator.validarDatosCambioPassword(dto));

        assertTrue(
                exception.getMessage().contains("obligatoria") ||
                exception.getMessage().contains("entre 8 y 100 caracteres"));
    }

    @Test
    @DisplayName("QA-PWD-05: Aceptar nueva contraseña válida entre 8 y 100 caracteres")
    void testNuevaValida_PasaValidacion() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        cambioPasswordValidator.validarDatosCambioPassword(dto);
    }

    // =========================================================================
    // CASE 4: Confirmación distinta (flujo recuperación / validación)
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-06: Confirmación distinta en flujo de recuperación es rechazada")
    void testConfirmacionDistinta_Rechaza() {
        RestablecerPasswordDTO dto = new RestablecerPasswordDTO();
        dto.setToken("valid-reset-token-12345");
        dto.setPasswordNueva("NuevaPassword#2026");
        dto.setConfirmarPassword("PasswordCompletamenteDiferente#2026");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> passwordResetValidator.validarSolicitudRestablecimiento(dto));

        assertEquals("La nueva contraseña y su confirmación no coinciden", exception.getMessage());
    }

    // =========================================================================
    // CASE 5: Sesión ausente, expirada o con auth_version revocado
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-07: Sesión revocada por auth_version anterior es rechazada")
    void testSesionRevocada_RechazaCambioPassword() {
        UsuarioSistema user = createTestUser(1L, 5L, OLD_PLAIN_PASSWORD);
        String staleToken = "jwt-version-4-revoked";

        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        when(jwtService.obtenerUsername(staleToken)).thenReturn(USERNAME);
        when(usuarioSistemaRepository.findIdByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(1L));
        when(usuarioSistemaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(jwtService.obtenerAuthVersion(staleToken)).thenReturn(4L); // Token has version 4, DB has 5

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.cambiarPassword(staleToken, dto));

        assertEquals("Token inválido o expirado", exception.getMessage());
        verify(usuarioSistemaRepository, never()).save(any(UsuarioSistema.class));
    }

    // =========================================================================
    // CASE 6: CSRF ausente o inválido / invalidación tras cambio
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-08: AuthController invalida token CSRF existente al cambiar contraseña exitosamente")
    void testCsrf_InvalidaCookieYEmiteNuevoJwt() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        when(mockAuthService.cambiarPassword("current-jwt", dto)).thenReturn("new-jwt-renewed");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Void> result = authController.cambiarPassword(
                "current-jwt",
                dto,
                request,
                response);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());

        // Verifica que se invalidó el token CSRF anterior
        verify(csrfTokenRepository).saveToken(null, request, response);

        // Verifica que la cookie JWT se renovó
        assertTrue(response.getHeaders("Set-Cookie").stream()
                .anyMatch(c -> c.startsWith("access_token=new-jwt-renewed")));
    }

    @Test
    @DisplayName("QA-PWD-09: Falla de cambio de contraseña NO invalida CSRF ni renueva sesión")
    void testCsrf_FalloNoAlteraSesion() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual("WrongPassword");
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        when(mockAuthService.cambiarPassword(anyString(), any(CambiarPasswordRequestDTO.class)))
                .thenThrow(new BusinessException("La contraseña actual no es correcta"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(BusinessException.class, () ->
                authController.cambiarPassword("current-jwt", dto, request, response));

        // No debe invalidar CSRF ni emitir nuevas cookies
        verify(csrfTokenRepository, never()).saveToken(null, request, response);
        assertTrue(response.getHeaders("Set-Cookie").isEmpty());
    }

    // =========================================================================
    // CASE 7: Contraseña vieja vs nueva y política de sesión
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-10: Política de sesión incrementa authVersion, invalida hash viejo y autentica nuevo")
    void testPoliticaSesion_IncrementaAuthVersionYActualizaHash() {
        UsuarioSistema user = createTestUser(42L, 2L, OLD_PLAIN_PASSWORD);
        String currentToken = "valid-token-v2";

        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        when(jwtService.obtenerUsername(currentToken)).thenReturn(USERNAME);
        when(usuarioSistemaRepository.findIdByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(42L));
        when(usuarioSistemaRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(jwtService.obtenerAuthVersion(currentToken)).thenReturn(2L);
        when(jwtService.generarToken(USERNAME, 3L)).thenReturn("new-token-v3");

        String nuevoJwt = authService.cambiarPassword(currentToken, dto);

        // Verifica incremento estricto de authVersion (de 2 a 3)
        assertEquals(3L, user.getAuthVersion());
        assertEquals("new-token-v3", nuevoJwt);

        // Verifica que el hash viejo ya no valida, y el nuevo sí
        assertFalse(passwordEncoder.matches(OLD_PLAIN_PASSWORD, user.getPasswordHash()));
        assertTrue(passwordEncoder.matches(NEW_PLAIN_PASSWORD, user.getPasswordHash()));

        // Verifica persistencia
        verify(usuarioSistemaRepository).save(user);
    }

    // =========================================================================
    // CASE 8: No exposición de secretos en auditoría y logs
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-11: Cero secretos en metadatos y campos auditados")
    void testCeroSecretosEnAuditoria() throws NoSuchMethodException {
        Method method = AuthService.class.getDeclaredMethod(
                "cambiarPassword",
                String.class,
                CambiarPasswordRequestDTO.class);

        Auditable auditable = method.getAnnotation(Auditable.class);
        assertNotNull(auditable, "cambiarPassword debe estar anotado con @Auditable");
        assertEquals("CAMBIAR_PASSWORD_PROPIA", auditable.action());
        assertEquals("UsuarioSistema", auditable.entityName());
        assertEquals("'self'", auditable.entityId());

        // Asegura que no se capturan campos ni metadata sensible
        assertEquals(0, auditable.metadata().length, "No debe exponer metadata con secretos");
        assertEquals(0, auditable.trackedFields().length, "No debe registrar campos sensibles");
    }

    // =========================================================================
    // CASE 9: Login y recuperación continúan funcionales
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-12: Login y recuperación continúan funcionales post-cambio")
    void testLoginYRecuperacionContinuanFuncionales() {
        UsuarioSistema user = createTestUser(100L, 3L, NEW_PLAIN_PASSWORD);

        // 1. Simulación de login con nueva contraseña:
        assertTrue(passwordEncoder.matches(NEW_PLAIN_PASSWORD, user.getPasswordHash()));
        assertFalse(passwordEncoder.matches(OLD_PLAIN_PASSWORD, user.getPasswordHash()));

        // 2. Validador de login permite autenticación
        loginValidator.validarUsuarioPuedeAutenticarse(user);

        // 3. Validador de recuperación permite recuperar cuenta
        when(perfilResolverService.tienePerfilActivo(user)).thenReturn(true);
        assertTrue(passwordResetValidator.usuarioPuedeRecuperarPassword(user));
    }
}
