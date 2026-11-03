package co.edu.ufps.legal_cases.security.service.auth.activation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.ufps.legal_cases.security.dto.auth.AuthMessageResponseDTO;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.auth.activation.AccountActivationTokenService.TokenActivacion;

class AccountActivationServiceManagementTest {

    private AccountActivationTokenService tokenService;
    private AccountActivationValidator validator;
    private UsuarioSistemaRepository usuarioSistemaRepository;
    private PasswordEncoder passwordEncoder;
    private AccountActivationEmailService emailService;

    private AccountActivationService activationService;

    @BeforeEach
    void setUp() {
        tokenService = mock(AccountActivationTokenService.class);
        validator = mock(AccountActivationValidator.class);
        usuarioSistemaRepository = mock(UsuarioSistemaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(AccountActivationEmailService.class);

        activationService = new AccountActivationService(
                tokenService,
                validator,
                usuarioSistemaRepository,
                passwordEncoder,
                emailService);

        activationService.limpiarCooldownsParaPruebas();
    }

    @Test
    @DisplayName("reenviarInvitacion: invalida token anterior, despacha correo y retorna mensaje genérico")
    void reenviarInvitacion_PendingUser_InvalidatesPreviousAndDispatchesEmail() {
        UsuarioSistema usuario = crearUsuario(1L, "estudiante@ufps.edu.co", EstadoActivacionCuenta.PENDIENTE, true);
        when(usuarioSistemaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(usuario));

        TokenActivacion tokenNuevo = new TokenActivacion("raw-token-seguro-123", LocalDateTime.now().plusHours(48));
        when(tokenService.crearTokenParaUsuario(usuario)).thenReturn(tokenNuevo);

        AuthMessageResponseDTO response = activationService.reenviarInvitacion(1L);

        assertNotNull(response);
        assertEquals(
                "Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación",
                response.getMensaje());

        verify(tokenService).crearTokenParaUsuario(usuario);
        verify(emailService).enviarInvitacionActivacion("estudiante@ufps.edu.co", "raw-token-seguro-123");
    }

    @Test
    @DisplayName("reenviarInvitacion: fallo de correo deja la cuenta en estado pendiente y recuperable sin lanzar excepción")
    void reenviarInvitacion_EmailFailure_LeavesAccountRecoverable() {
        UsuarioSistema usuario = crearUsuario(2L, "asesor@ufps.edu.co", EstadoActivacionCuenta.PENDIENTE, true);
        when(usuarioSistemaRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(usuario));

        TokenActivacion tokenNuevo = new TokenActivacion("raw-token-456", LocalDateTime.now().plusHours(48));
        when(tokenService.crearTokenParaUsuario(usuario)).thenReturn(tokenNuevo);

        doThrow(new RuntimeException("Brevo gateway timeout"))
                .when(emailService).enviarInvitacionActivacion(anyString(), anyString());

        AuthMessageResponseDTO response = activationService.reenviarInvitacion(2L);

        assertNotNull(response);
        assertEquals(
                "Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación",
                response.getMensaje());

        // Token fue generado y revoca el anterior; la cuenta sigue en PENDIENTE
        verify(tokenService).crearTokenParaUsuario(usuario);
        assertEquals(EstadoActivacionCuenta.PENDIENTE, usuario.getEstadoActivacion());
    }

    @Test
    @DisplayName("reenviarInvitacion: rate limit bloquea segundo reenvío inmediato y retorna mensaje genérico")
    void reenviarInvitacion_RateLimitCooldown_BlocksSecondDispatch() {
        UsuarioSistema usuario = crearUsuario(3L, "monitor@ufps.edu.co", EstadoActivacionCuenta.PENDIENTE, true);
        when(usuarioSistemaRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(usuario));

        TokenActivacion tokenNuevo = new TokenActivacion("raw-token-789", LocalDateTime.now().plusHours(48));
        when(tokenService.crearTokenParaUsuario(usuario)).thenReturn(tokenNuevo);

        // Primer intento
        AuthMessageResponseDTO res1 = activationService.reenviarInvitacion(3L);
        assertEquals("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación", res1.getMensaje());

        // Segundo intento inmediato para el mismo usuario
        AuthMessageResponseDTO res2 = activationService.reenviarInvitacion(3L);
        assertEquals("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación", res2.getMensaje());

        // Solo se generó y despachó un token
        verify(tokenService, times(1)).crearTokenParaUsuario(usuario);
        verify(emailService, times(1)).enviarInvitacionActivacion(anyString(), anyString());
    }

    @Test
    @DisplayName("reenviarInvitacion: cuenta inexistente o ya activa retorna respuesta genérica sin revelar estado")
    void reenviarInvitacion_NotFoundOrActive_ReturnsGenericResponse() {
        // Inexistente
        when(usuarioSistemaRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        AuthMessageResponseDTO resNotFound = activationService.reenviarInvitacion(999L);
        assertEquals("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación", resNotFound.getMensaje());

        // Ya activa
        UsuarioSistema usuarioActivo = crearUsuario(4L, "activo@ufps.edu.co", EstadoActivacionCuenta.ACTIVA, true);
        when(usuarioSistemaRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(usuarioActivo));
        AuthMessageResponseDTO resActive = activationService.reenviarInvitacion(4L);
        assertEquals("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación", resActive.getMensaje());

        verify(tokenService, never()).crearTokenParaUsuario(any());
        verify(emailService, never()).enviarInvitacionActivacion(anyString(), anyString());
    }

    @Test
    @DisplayName("revocarInvitacion: cuenta pendiente revoca tokens activos y retorna mensaje genérico")
    void revocarInvitacion_PendingUser_RevokesActiveTokens() {
        UsuarioSistema usuario = crearUsuario(5L, "pendiente@ufps.edu.co", EstadoActivacionCuenta.PENDIENTE, true);
        when(usuarioSistemaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(usuario));

        AuthMessageResponseDTO response = activationService.revocarInvitacion(5L);

        assertNotNull(response);
        assertEquals("Si la cuenta existe y tenía invitaciones activas, han sido revocadas", response.getMensaje());
        verify(tokenService).revocarTokensActivos(usuario);
    }

    @Test
    @DisplayName("revocarInvitacion: usuario inexistente retorna respuesta genérica")
    void revocarInvitacion_NotFound_ReturnsGenericResponse() {
        when(usuarioSistemaRepository.findByIdForUpdate(888L)).thenReturn(Optional.empty());

        AuthMessageResponseDTO response = activationService.revocarInvitacion(888L);

        assertNotNull(response);
        assertEquals("Si la cuenta existe y tenía invitaciones activas, han sido revocadas", response.getMensaje());
        verify(tokenService, never()).revocarTokensActivos(any());
    }

    @Test
    @DisplayName("reenviarInvitacionPorEmail: normaliza correo y delega correctamente")
    void reenviarInvitacionPorEmail_NormalizesAndDelegates() {
        UsuarioSistema usuario = crearUsuario(6L, "correo@ufps.edu.co", EstadoActivacionCuenta.PENDIENTE, true);
        when(usuarioSistemaRepository.findByUsernameIgnoreCase("correo@ufps.edu.co")).thenReturn(Optional.of(usuario));
        when(usuarioSistemaRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(usuario));

        TokenActivacion tokenNuevo = new TokenActivacion("raw-token-email", LocalDateTime.now().plusHours(48));
        when(tokenService.crearTokenParaUsuario(usuario)).thenReturn(tokenNuevo);

        AuthMessageResponseDTO response = activationService.reenviarInvitacionPorEmail("  CORREO@ufps.edu.co  ");

        assertNotNull(response);
        assertEquals("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación", response.getMensaje());
        verify(emailService).enviarInvitacionActivacion("correo@ufps.edu.co", "raw-token-email");
    }

    @Test
    @DisplayName("revocarInvitacionPorEmail: normaliza correo y revoca tokens")
    void revocarInvitacionPorEmail_NormalizesAndRevokes() {
        UsuarioSistema usuario = crearUsuario(7L, "revocar@ufps.edu.co", EstadoActivacionCuenta.PENDIENTE, true);
        when(usuarioSistemaRepository.findByUsernameIgnoreCase("revocar@ufps.edu.co")).thenReturn(Optional.of(usuario));
        when(usuarioSistemaRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuario));

        AuthMessageResponseDTO response = activationService.revocarInvitacionPorEmail("REVOCAR@ufps.edu.co");

        assertNotNull(response);
        assertEquals("Si la cuenta existe y tenía invitaciones activas, han sido revocadas", response.getMensaje());
        verify(tokenService).revocarTokensActivos(usuario);
    }

    private UsuarioSistema crearUsuario(Long id, String username, EstadoActivacionCuenta estado, boolean activo) {
        UsuarioSistema u = new UsuarioSistema();
        u.setId(id);
        u.setUsername(username);
        u.setEstadoActivacion(estado);
        u.setActivo(activo);
        u.setAuthVersion(0L);
        return u;
    }
}
