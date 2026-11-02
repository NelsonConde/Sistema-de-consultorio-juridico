package co.edu.ufps.legal_cases.security.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.ufps.legal_cases.common.email.EmailService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.auth.password.RestablecerPasswordDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.PasswordResetToken;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetTokenService;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetValidator;

class PasswordResetServiceAuthVersionTest {

    private UsuarioSistemaRepository usuarioSistemaRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private EmailTemplateService emailTemplateService;
    private PasswordResetTokenService passwordResetTokenService;
    private PasswordResetValidator passwordResetValidator;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        usuarioSistemaRepository =
                mock(UsuarioSistemaRepository.class);

        passwordEncoder =
                mock(PasswordEncoder.class);

        emailService =
                mock(EmailService.class);

        emailTemplateService =
                mock(EmailTemplateService.class);

        passwordResetTokenService =
                mock(PasswordResetTokenService.class);

        passwordResetValidator =
                mock(PasswordResetValidator.class);

        passwordResetService = new PasswordResetService(
                usuarioSistemaRepository,
                passwordEncoder,
                emailService,
                emailTemplateService,
                passwordResetTokenService,
                passwordResetValidator);
    }

    @Test
    void restablecerPasswordIncrementaAuthVersion() {
        String token = "reset-token-test";

        RestablecerPasswordDTO dto =
                new RestablecerPasswordDTO();

        dto.setToken(token);
        dto.setPasswordNueva("NuevaClave123");
        dto.setConfirmarPassword("NuevaClave123");

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(42L);
        usuario.setUsername("usuario@prueba.test");
        usuario.setPasswordHash("hash-anterior");
        usuario.setAuthVersion(8L);

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setId(100L);
        resetToken.setUsuarioSistema(usuario);
        resetToken.setUsado(false);

        when(passwordResetTokenService
                .obtenerTokenActivo(token))
                .thenReturn(resetToken);

        when(usuarioSistemaRepository
                .findByIdForUpdate(42L))
                .thenReturn(Optional.of(usuario));

        when(passwordResetValidator
                .usuarioPuedeRecuperarPassword(usuario))
                .thenReturn(true);

        when(passwordEncoder.encode("NuevaClave123"))
                .thenReturn("hash-nuevo");

        passwordResetService.restablecerPassword(dto);

        assertEquals(
                "hash-nuevo",
                usuario.getPasswordHash());

        assertEquals(
                9L,
                usuario.getAuthVersion());

        verify(passwordResetValidator)
                .validarSolicitudRestablecimiento(dto);

        verify(passwordResetTokenService)
                .validarNoExpirado(resetToken);

        verify(passwordResetValidator)
                .validarPasswordNuevaDiferente(
                        "NuevaClave123",
                        usuario,
                        passwordEncoder);

        verify(passwordResetTokenService)
                .marcarComoUsado(resetToken);

        verify(usuarioSistemaRepository)
                .save(usuario);
    }

    @Test
    void restablecerPasswordNoModificaUsuarioSiNuevaPasswordEsRechazada() {
        String token = "reset-token-test";

        RestablecerPasswordDTO dto =
                new RestablecerPasswordDTO();

        dto.setToken(token);
        dto.setPasswordNueva("MismaClave123");
        dto.setConfirmarPassword("MismaClave123");

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setId(42L);
        usuario.setUsername("usuario@prueba.test");
        usuario.setPasswordHash("hash-actual");
        usuario.setAuthVersion(8L);

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setId(100L);
        resetToken.setUsuarioSistema(usuario);
        resetToken.setUsado(false);

        when(passwordResetTokenService
                .obtenerTokenActivo(token))
                .thenReturn(resetToken);

        when(usuarioSistemaRepository
                .findByIdForUpdate(42L))
                .thenReturn(Optional.of(usuario));

        when(passwordResetValidator
                .usuarioPuedeRecuperarPassword(usuario))
                .thenReturn(true);

        org.mockito.Mockito.doThrow(
                        new BusinessException(
                                "La nueva contraseña no puede ser igual a la actual"))
                .when(passwordResetValidator)
                .validarPasswordNuevaDiferente(
                        "MismaClave123",
                        usuario,
                        passwordEncoder);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> passwordResetService
                        .restablecerPassword(dto));

        assertEquals(
                "La nueva contraseña no puede ser igual a la actual",
                exception.getMessage());

        assertEquals(
                "hash-actual",
                usuario.getPasswordHash());

        assertEquals(
                8L,
                usuario.getAuthVersion());

        verify(passwordEncoder, never())
                .encode(any(String.class));

        verify(passwordResetTokenService, never())
                .marcarComoUsado(any(PasswordResetToken.class));

        verify(usuarioSistemaRepository, never())
                .save(any(UsuarioSistema.class));
    }
}