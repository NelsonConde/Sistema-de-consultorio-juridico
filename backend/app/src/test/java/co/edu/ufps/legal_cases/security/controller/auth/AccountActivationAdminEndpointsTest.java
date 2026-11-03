package co.edu.ufps.legal_cases.security.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import co.edu.ufps.legal_cases.config.security.AuthCookieProperties;
import co.edu.ufps.legal_cases.security.controller.account.UsuarioSistemaController;
import co.edu.ufps.legal_cases.security.dto.auth.AuthMessageResponseDTO;
import co.edu.ufps.legal_cases.security.dto.auth.activation.ReenviarInvitacionRequestDTO;
import co.edu.ufps.legal_cases.security.dto.auth.activation.RevocarInvitacionRequestDTO;
import co.edu.ufps.legal_cases.security.service.account.UsuarioCambioPerfilService;
import co.edu.ufps.legal_cases.security.service.account.UsuarioSistemaService;
import co.edu.ufps.legal_cases.security.service.auth.AuthService;
import co.edu.ufps.legal_cases.security.service.auth.PasswordResetService;
import co.edu.ufps.legal_cases.security.service.auth.activation.AccountActivationService;

class AccountActivationAdminEndpointsTest {

    private AccountActivationService accountActivationService;
    private AuthController authController;
    private UsuarioSistemaController usuarioSistemaController;

    @BeforeEach
    void setUp() {
        accountActivationService = mock(AccountActivationService.class);

        AuthService authService = mock(AuthService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        CsrfTokenRepository csrfTokenRepository = mock(CsrfTokenRepository.class);
        AuthCookieProperties authCookieProperties = new AuthCookieProperties();

        authController = new AuthController(
                authService,
                passwordResetService,
                accountActivationService,
                csrfTokenRepository,
                authCookieProperties);

        UsuarioSistemaService usuarioSistemaService = mock(UsuarioSistemaService.class);
        UsuarioCambioPerfilService usuarioCambioPerfilService = mock(UsuarioCambioPerfilService.class);

        usuarioSistemaController = new UsuarioSistemaController(
                usuarioSistemaService,
                usuarioCambioPerfilService,
                accountActivationService);
    }

    @Test
    @DisplayName("AuthController: reenviar invitacion por usuarioId delega a AccountActivationService")
    void authController_ReenviarInvitacion_ByUsuarioId() {
        ReenviarInvitacionRequestDTO dto = new ReenviarInvitacionRequestDTO(10L, null);
        AuthMessageResponseDTO expected = new AuthMessageResponseDTO("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación");

        when(accountActivationService.reenviarInvitacion(10L)).thenReturn(expected);

        ResponseEntity<AuthMessageResponseDTO> response = authController.reenviarInvitacion(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected.getMensaje(), response.getBody().getMensaje());
        verify(accountActivationService).reenviarInvitacion(10L);
    }

    @Test
    @DisplayName("AuthController: reenviar invitacion por email delega a AccountActivationService")
    void authController_ReenviarInvitacion_ByEmail() {
        ReenviarInvitacionRequestDTO dto = new ReenviarInvitacionRequestDTO(null, "usuario@ufps.edu.co");
        AuthMessageResponseDTO expected = new AuthMessageResponseDTO("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación");

        when(accountActivationService.reenviarInvitacionPorEmail("usuario@ufps.edu.co")).thenReturn(expected);

        ResponseEntity<AuthMessageResponseDTO> response = authController.reenviarInvitacion(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected.getMensaje(), response.getBody().getMensaje());
        verify(accountActivationService).reenviarInvitacionPorEmail("usuario@ufps.edu.co");
    }

    @Test
    @DisplayName("AuthController: revocar invitacion por usuarioId delega a AccountActivationService")
    void authController_RevocarInvitacion_ByUsuarioId() {
        RevocarInvitacionRequestDTO dto = new RevocarInvitacionRequestDTO(20L, null);
        AuthMessageResponseDTO expected = new AuthMessageResponseDTO("Si la cuenta existe y tenía invitaciones activas, han sido revocadas");

        when(accountActivationService.revocarInvitacion(20L)).thenReturn(expected);

        ResponseEntity<AuthMessageResponseDTO> response = authController.revocarInvitacion(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected.getMensaje(), response.getBody().getMensaje());
        verify(accountActivationService).revocarInvitacion(20L);
    }

    @Test
    @DisplayName("UsuarioSistemaController: reenviar invitacion por ID de path delega a AccountActivationService")
    void usuarioSistemaController_ReenviarInvitacion() {
        AuthMessageResponseDTO expected = new AuthMessageResponseDTO("Si la cuenta existe y se encuentra pendiente de activación, se procesará el envío de la invitación");

        when(accountActivationService.reenviarInvitacion(30L)).thenReturn(expected);

        ResponseEntity<AuthMessageResponseDTO> response = usuarioSistemaController.reenviarInvitacionActivacion(30L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected.getMensaje(), response.getBody().getMensaje());
        verify(accountActivationService).reenviarInvitacion(30L);
    }

    @Test
    @DisplayName("UsuarioSistemaController: revocar invitacion por ID de path delega a AccountActivationService")
    void usuarioSistemaController_RevocarInvitacion() {
        AuthMessageResponseDTO expected = new AuthMessageResponseDTO("Si la cuenta existe y tenía invitaciones activas, han sido revocadas");

        when(accountActivationService.revocarInvitacion(40L)).thenReturn(expected);

        ResponseEntity<AuthMessageResponseDTO> response = usuarioSistemaController.revocarInvitacionActivacion(40L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected.getMensaje(), response.getBody().getMensaje());
        verify(accountActivationService).revocarInvitacion(40L);
    }
}
