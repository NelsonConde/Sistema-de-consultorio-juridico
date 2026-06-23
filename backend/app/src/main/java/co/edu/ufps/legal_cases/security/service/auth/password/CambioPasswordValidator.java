package co.edu.ufps.legal_cases.security.service.auth.password;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.auth.password.CambiarPasswordRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Valida el cambio de contraseña de un usuario autenticado.
// La modificación real de la contraseña queda en AuthService.
@Component
public class CambioPasswordValidator {

    // Valida que los campos necesarios para cambiar la contraseña sean correctos.
    public void validarDatosCambioPassword(CambiarPasswordRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Los datos para cambiar la contraseña son obligatorios");
        }

        if (dto.getPasswordActual() == null || dto.getPasswordActual().isBlank()) {
            throw new BusinessException("La contraseña actual es obligatoria");
        }

        if (dto.getPasswordNueva() == null || dto.getPasswordNueva().isBlank()) {
            throw new BusinessException("La nueva contraseña es obligatoria");
        }

        if (dto.getPasswordNueva().length() < 8 || dto.getPasswordNueva().length() > 100) {
            throw new BusinessException("La nueva contraseña debe tener entre 8 y 100 caracteres");
        }
    }

    // Verifica que la contraseña actual enviada coincida con la guardada en base de datos.
    public void validarPasswordActual(
            String passwordActual,
            UsuarioSistema usuario,
            PasswordEncoder passwordEncoder) {

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new BusinessException("La contraseña actual no es correcta");
        }
    }

    // Evita que la nueva contraseña sea igual a la contraseña actual.
    public void validarPasswordNuevaDiferente(
            String passwordNueva,
            UsuarioSistema usuario,
            PasswordEncoder passwordEncoder) {

        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new BusinessException("La nueva contraseña no puede ser igual a la actual");
        }
    }
}