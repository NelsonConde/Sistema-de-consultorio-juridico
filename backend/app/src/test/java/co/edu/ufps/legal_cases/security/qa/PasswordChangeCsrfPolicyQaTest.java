package co.edu.ufps.legal_cases.security.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.mockito.ArgumentCaptor;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import co.edu.ufps.legal_cases.audit.aop.log.Auditable;
import co.edu.ufps.legal_cases.audit.aop.log.AuditAspect;
import co.edu.ufps.legal_cases.audit.aop.log.AuditExpressionEvaluator;
import co.edu.ufps.legal_cases.audit.aop.log.AuditStateSnapshotService;
import co.edu.ufps.legal_cases.audit.model.log.AuditEvent;
import co.edu.ufps.legal_cases.audit.model.log.AuditSource;
import co.edu.ufps.legal_cases.audit.service.log.AuditLogService;
import co.edu.ufps.legal_cases.audit.service.log.AuditRequestContext;
import co.edu.ufps.legal_cases.config.security.CsrfConfig;
import co.edu.ufps.legal_cases.config.security.CsrfProperties;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.config.security.AuthCookieProperties;
import co.edu.ufps.legal_cases.security.controller.auth.AuthController;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResultDTO;
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
    private static final String USERNAME = "qa.scrum292@example.test";

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
    @DisplayName("QA-PWD-04: Rechazar contraseña de más de 100 caracteres")
    void testNuevaDemasiadoLarga_Rechaza() {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva("A".repeat(101));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cambioPasswordValidator.validarDatosCambioPassword(dto));

        assertTrue(exception.getMessage().contains("entre 8 y 100 caracteres"));
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


    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("QA-PWD-07: Una sesión ausente no puede cambiar la contraseña")
    void testSesionAusente_RechazaSinPersistir(String token) {
        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.cambiarPassword(token, dto));

        assertEquals("No hay sesión activa", exception.getMessage());
        verify(usuarioSistemaRepository, never()).save(any(UsuarioSistema.class));
    }

    @Test
    @DisplayName("QA-PWD-07: JWT expirado real no llega a persistencia")
    void testSesionExpirada_RechazaSinPersistir() {
        // Un JWT firmado que ya caducó prueba la validación real de expiración.
        JwtService expiredJwt = new JwtService(
                "0123456789abcdef0123456789abcdef", -60_000L);
        String expiredToken = expiredJwt.generarToken(USERNAME, 1L);
        AuthService service = new AuthService(
                usuarioSistemaRepository, passwordEncoder, expiredJwt,
                loginMapper, loginValidator, cambioPasswordValidator,
                usuarioSistemaMapper);

        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cambiarPassword(expiredToken, dto));

        assertEquals("Token inválido o expirado", exception.getMessage());
        verify(usuarioSistemaRepository, never()).save(any(UsuarioSistema.class));
    }

    // =========================================================================
    // CASE 6: CSRF ausente o inválido / invalidación tras cambio
    // =========================================================================


    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid"})
    @DisplayName("QA-PWD-08: El filtro CSRF bloquea PATCH sin header válido")
    void testCsrfPatchAusenteOInvalido_Rechaza(String caseName) throws Exception {
        CsrfProperties properties = new CsrfProperties();
        properties.setSecure(false);
        properties.setSameSite("Lax");
        CsrfTokenRepository repository = new CsrfConfig().csrfTokenRepository(properties);
        CsrfFilter filter = new CsrfFilter(repository);

        MockHttpServletRequest initial = new MockHttpServletRequest("GET", "/api/auth/csrf");
        MockHttpServletResponse initialResponse = new MockHttpServletResponse();
        filter.doFilter(initial, initialResponse, (request, response) -> {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            assertNotNull(token);
            token.getToken(); // Genera la cookie CSRF para simular una sesión existente.
        });

        Cookie csrfCookie = initialResponse.getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        MockHttpServletRequest patch = new MockHttpServletRequest(
                "PATCH", "/api/auth/cambiar-password");
        patch.setCookies(csrfCookie);
        if ("invalid".equals(caseName)) {
            patch.addHeader("X-XSRF-TOKEN", "invalid-csrf-token");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedController = new AtomicBoolean(false);

        filter.doFilter(patch, response,
                (request, servletResponse) -> reachedController.set(true));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertFalse(reachedController.get());
    }

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


    @Test
    @DisplayName("QA-PWD-11: El evento AOP de cambio de contraseña no contiene secretos")
    void testEventoAuditoriaNoContieneSecretos() throws Throwable {
        Method method = AuthService.class.getDeclaredMethod(
                "cambiarPassword", String.class, CambiarPasswordRequestDTO.class);
        Auditable annotation = method.getAnnotation(Auditable.class);

        AuditLogService logService = mock(AuditLogService.class);
        AuditRequestContext context = mock(AuditRequestContext.class);
        AuditStateSnapshotService snapshots = new AuditStateSnapshotService(
                mock(jakarta.persistence.EntityManager.class));
        AuditAspect aspect = new AuditAspect(
                logService, context, new AuditExpressionEvaluator(), snapshots);
        when(context.capture()).thenReturn(new AuditRequestContext.Snapshot(
                "qa-user", AuditSource.HTTP, "safe-correlation", "127.0.0.1", "test-agent"));

        CambiarPasswordRequestDTO dto = new CambiarPasswordRequestDTO();
        dto.setPasswordActual(OLD_PLAIN_PASSWORD);
        dto.setPasswordNueva(NEW_PLAIN_PASSWORD);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"old-jwt-secret", dto});
        when(joinPoint.proceed()).thenReturn("new-jwt-secret");

        aspect.audit(joinPoint, annotation);

        ArgumentCaptor<AuditEvent> captured = ArgumentCaptor.forClass(AuditEvent.class);
        verify(logService).recordSuccess(captured.capture());
        AuditEvent event = captured.getValue();
        assertTrue(event.getMetadata().isEmpty());
        assertTrue(event.getBeforeState().isEmpty());
        assertTrue(event.getAfterState().isEmpty());
        String serializedEvent = event.toString();
        for (String secret : new String[] {
                OLD_PLAIN_PASSWORD, NEW_PLAIN_PASSWORD, "old-jwt-secret", "new-jwt-secret"
        }) {
            assertFalse(serializedEvent.contains(secret));
        }
    }

    // =========================================================================
    // CASE 9: Login y recuperación continúan funcionales
    // =========================================================================

    @Test
    @DisplayName("QA-PWD-12: Cambio de contraseña permite login real de servicio con clave nueva")
    void testLoginYRecuperacionContinuanFuncionales() {
        UsuarioSistema user = createTestUser(100L, 2L, OLD_PLAIN_PASSWORD);
        PerfilUsuarioActual perfil = mock(PerfilUsuarioActual.class);
        LoginResponseDTO response = mock(LoginResponseDTO.class);
        LoginValidator actualLoginValidator = new LoginValidator(perfilResolverService);
        AuthService realLoginService = new AuthService(
                usuarioSistemaRepository, passwordEncoder, jwtService,
                loginMapper, actualLoginValidator, cambioPasswordValidator,
                usuarioSistemaMapper);

        when(perfilResolverService.obtenerPerfilActivoObligatorio(user)).thenReturn(perfil);
        when(perfilResolverService.tienePerfilActivo(user)).thenReturn(true);
        when(loginMapper.convertirAResponse(user, perfil)).thenReturn(response);
        when(usuarioSistemaRepository.findIdByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(100L));
        when(usuarioSistemaRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(user));
        when(usuarioSistemaRepository.findWithRolAndPermisosByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(user));
        when(jwtService.obtenerUsername("jwt-v2")).thenReturn(USERNAME);
        when(jwtService.obtenerAuthVersion("jwt-v2")).thenReturn(2L);
        when(jwtService.generarToken(USERNAME, 3L)).thenReturn("jwt-v3");

        CambiarPasswordRequestDTO change = new CambiarPasswordRequestDTO();
        change.setPasswordActual(OLD_PLAIN_PASSWORD);
        change.setPasswordNueva(NEW_PLAIN_PASSWORD);
        assertEquals("jwt-v3", realLoginService.cambiarPassword("jwt-v2", change));

        LoginRequestDTO newLogin = new LoginRequestDTO();
        newLogin.setUsername(USERNAME);
        newLogin.setPassword(NEW_PLAIN_PASSWORD);
        LoginResultDTO accepted = realLoginService.login(newLogin);
        assertSame(response, accepted.getResponse());
        assertEquals("jwt-v3", accepted.getToken());

        LoginRequestDTO oldLogin = new LoginRequestDTO();
        oldLogin.setUsername(USERNAME);
        oldLogin.setPassword(OLD_PLAIN_PASSWORD);
        BusinessException rejected = assertThrows(
                BusinessException.class,
                () -> realLoginService.login(oldLogin));
        assertEquals("Usuario o contraseña incorrectos", rejected.getMessage());

        assertTrue(passwordResetValidator.usuarioPuedeRecuperarPassword(user));
    }

}
