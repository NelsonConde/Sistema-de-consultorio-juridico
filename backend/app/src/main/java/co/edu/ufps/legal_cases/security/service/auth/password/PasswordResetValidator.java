package co.edu.ufps.legal_cases.security.service.auth.password;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.auth.password.RestablecerPasswordDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

// Valida las reglas del flujo de recuperación de contraseña.
// No genera tokens, no guarda datos y no envía correos.
@Component
public class PasswordResetValidator {

    private final PerfilUsuarioResolverService perfilUsuarioResolverService;

    public PasswordResetValidator(PerfilUsuarioResolverService perfilUsuarioResolverService) {
        this.perfilUsuarioResolverService = perfilUsuarioResolverService;
    }

    // Valida los datos enviados para restablecer la contraseña.
    public void validarSolicitudRestablecimiento(RestablecerPasswordDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos para restablecer la contraseña son obligatorios");
        }

        if (dto.getToken() == null || dto.getToken().isBlank()) {
            throw new BusinessException("El token es obligatorio");
        }

        if (dto.getPasswordNueva() == null || dto.getPasswordNueva().isBlank()) {
            throw new BusinessException("La nueva contraseña es obligatoria");
        }

        if (dto.getConfirmarPassword() == null || dto.getConfirmarPassword().isBlank()) {
            throw new BusinessException("La confirmación de contraseña es obligatoria");
        }

        if (dto.getPasswordNueva().length() < 8 || dto.getPasswordNueva().length() > 100) {
            throw new BusinessException("La nueva contraseña debe tener entre 8 y 100 caracteres");
        }

        if (!dto.getPasswordNueva().equals(dto.getConfirmarPassword())) {
            throw new BusinessException("La nueva contraseña y su confirmación no coinciden");
        }
    }

    // Valida si el usuario tiene todo activo para poder recuperar contraseña.
    public boolean usuarioPuedeRecuperarPassword(UsuarioSistema usuario) {
        return usuario != null
                && usuarioActivo(usuario)
                && rolActivo(usuario)
                && perfilActivo(usuario);
    }

    // Evita que la nueva contraseña sea igual a la actual.
    public void validarPasswordNuevaDiferente(
            String passwordNueva,
            UsuarioSistema usuario,
            PasswordEncoder passwordEncoder) {

        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new BusinessException("La nueva contraseña no puede ser igual a la actual");
        }
    }

    private boolean usuarioActivo(UsuarioSistema usuario) {
        return Boolean.TRUE.equals(usuario.getActivo());
    }

    private boolean rolActivo(UsuarioSistema usuario) {
        return usuario.getRol() != null
                && Boolean.TRUE.equals(usuario.getRol().getActivo());
    }

    // Valida que el perfil real asociado al usuario también esté activo.
    private boolean perfilActivo(UsuarioSistema usuario) {
        // Nueva validación normalizada.
        // Ya no depende de asesor_id, estudiante_id, monitor_id, administrativo_id
        // ni conciliador_id dentro de usuario_sistema.
        return perfilUsuarioResolverService.tienePerfilActivo(usuario);
    }
}