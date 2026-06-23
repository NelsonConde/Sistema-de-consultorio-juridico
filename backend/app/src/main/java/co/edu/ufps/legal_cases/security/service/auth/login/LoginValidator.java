package co.edu.ufps.legal_cases.security.service.auth.login;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.perfil.PerfilUsuarioResolverService;

// Valida el flujo de autenticación.
// Mantiene fuera de AuthService las reglas de usuario, rol, perfil y credenciales.
@Component
public class LoginValidator {

    private final PerfilUsuarioResolverService perfilUsuarioResolverService;

    public LoginValidator(PerfilUsuarioResolverService perfilUsuarioResolverService) {
        this.perfilUsuarioResolverService = perfilUsuarioResolverService;
    }

    public String obtenerUsernameNormalizado(LoginRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos de inicio de sesión son obligatorios");
        }

        String username = normalizarEmail(dto.getUsername());

        if (username == null) {
            throw new BusinessException("El correo es obligatorio");
        }

        return username;
    }

    public void validarPasswordInformada(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("La contraseña es obligatoria");
        }
    }

    public void validarPasswordCorrecta(
            String password,
            UsuarioSistema usuario,
            PasswordEncoder passwordEncoder) {

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new BusinessException("Usuario o contraseña incorrectos");
        }
    }

    public PerfilUsuarioActual validarUsuarioPuedeAutenticarse(UsuarioSistema usuario) {
        validarUsuarioActivo(usuario);
        validarRolActivo(usuario);

        // Se valida que el perfil real asociado también esté activo.
        // Esta validación ya no depende de asesor_id, estudiante_id, monitor_id,
        // administrativo_id ni conciliador_id en usuario_sistema.
        return perfilUsuarioResolverService.obtenerPerfilActivoObligatorio(usuario);
    }

    private void validarUsuarioActivo(UsuarioSistema usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario se encuentra inactivo");
        }
    }

    private void validarRolActivo(UsuarioSistema usuario) {
        if (usuario.getRol() == null || !Boolean.TRUE.equals(usuario.getRol().getActivo())) {
            throw new BusinessException("El rol del usuario se encuentra inactivo");
        }
    }
}