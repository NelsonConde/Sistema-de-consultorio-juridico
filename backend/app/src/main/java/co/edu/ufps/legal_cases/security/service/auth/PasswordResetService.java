package co.edu.ufps.legal_cases.security.service.auth;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.email.EmailService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.auth.password.RestablecerPasswordDTO;
import co.edu.ufps.legal_cases.security.dto.auth.password.SolicitarRecuperacionPasswordDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.PasswordResetToken;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetTokenService;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetValidator;

@Service
public class PasswordResetService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetValidator passwordResetValidator;

    public PasswordResetService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            EmailTemplateService emailTemplateService,
            PasswordResetTokenService passwordResetTokenService,
            PasswordResetValidator passwordResetValidator) {

        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordResetValidator = passwordResetValidator;
    }

    @Transactional
    public void solicitarRecuperacion(SolicitarRecuperacionPasswordDTO dto) {
        String username = normalizarEmail(dto.getUsername());

        if (username == null) {
            throw new BusinessException("El correo es obligatorio");
        }

        // Busca el usuario con rol cargado para poder validar si puede recuperar
        // contraseña.
        // El perfil real ya no se carga desde usuario_sistema, ahora se resuelve con
        // PerfilUsuarioResolverService usando tipo_perfil_actual y usuario_sistema_id
        // en la tabla real.
        UsuarioSistema usuario = usuarioSistemaRepository
                .findForPasswordResetByUsernameIgnoreCase(username)
                .orElse(null);

        // No se informa si el usuario existe o no, para evitar enumeración de correos.
        if (!passwordResetValidator.usuarioPuedeRecuperarPassword(usuario)) {
            return;
        }

        PasswordResetTokenService.TokenRecuperacion tokenRecuperacion =
                passwordResetTokenService.crearTokenParaUsuario(usuario);

        String html = emailTemplateService.construirRecuperacionPassword(tokenRecuperacion.enlace());

        emailService.enviarHtml(
                usuario.getUsername(),
                null, // No se envía el nombre del destinatario.
                "Recuperación de contraseña",
                html,
                "recuperación de contraseña");
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordDTO dto) {
        passwordResetValidator.validarSolicitudRestablecimiento(dto);

        // Se genera el hash del token recibido para compararlo con el hash guardado en BD.
        PasswordResetToken resetToken = passwordResetTokenService.obtenerTokenActivo(dto.getToken());

        passwordResetTokenService.validarNoExpirado(resetToken);

        UsuarioSistema usuario = resetToken.getUsuarioSistema();

        // Valida que el usuario, su rol y su perfil sigan activos.
        if (!passwordResetValidator.usuarioPuedeRecuperarPassword(usuario)) {
            throw new BusinessException("Token inválido o expirado");
        }

        passwordResetValidator.validarPasswordNuevaDiferente(
                dto.getPasswordNueva(),
                usuario,
                passwordEncoder);

        // Cifra la nueva contraseña antes de guardarla.
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));

        // Marca el token como usado para que no pueda reutilizarse.
        passwordResetTokenService.marcarComoUsado(resetToken);

        usuarioSistemaRepository.save(usuario);
    }
}