package co.edu.ufps.legal_cases.security.service.account.usuario;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Valida reglas locales de UsuarioSistema.
// No consulta base de datos; las búsquedas quedan en el service.
@Component
public class UsuarioSistemaValidator {

    public void validarIdObligatorio(Long id) {
        if (id == null) {
            throw new BusinessException("El id del usuario del sistema es obligatorio");
        }
    }

    public void validarCambioEstado(UsuarioSistema usuario, Boolean activo) {
        if (activo == null) {
            throw new BusinessException("El estado activo es obligatorio");
        }

        if (Objects.equals(usuario.getActivo(), activo)) {
            throw new BusinessException("El usuario ya tiene ese estado");
        }
    }
}