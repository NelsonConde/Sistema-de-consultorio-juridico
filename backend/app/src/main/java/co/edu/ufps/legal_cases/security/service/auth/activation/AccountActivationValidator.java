package co.edu.ufps.legal_cases.security.service.auth.activation;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.AccountActivationException;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.auth.activation.ActivarCuentaRequestDTO;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.model.auth.AccountActivationToken;

@Component
public class AccountActivationValidator {

    public void validarSolicitud(
            ActivarCuentaRequestDTO dto) {

        if (dto == null) {
            throw new BusinessException(
                    "Los datos de activación son obligatorios");
        }

        if (dto.getToken() == null
                || dto.getToken().isBlank()) {

            throw AccountActivationException.invalido();
        }

        if (dto.getPasswordNueva() == null
                || dto.getPasswordNueva().isBlank()) {

            throw new BusinessException(
                    "La nueva contraseña es obligatoria");
        }

        int longitud =
                dto.getPasswordNueva().length();

        if (longitud < 8 || longitud > 100) {
            throw new BusinessException(
                    "La nueva contraseña debe tener entre 8 y 100 caracteres");
        }
    }

    public void validarToken(
            AccountActivationToken token) {

        if (token == null) {
            throw AccountActivationException.invalido();
        }

        if (Boolean.TRUE.equals(
                token.getRevocado())) {

            throw AccountActivationException.revocado();
        }

        if (Boolean.TRUE.equals(
                token.getUsado())) {

            throw AccountActivationException.usado();
        }

        if (token.getFechaExpiracion() == null
                || !token.getFechaExpiracion()
                .isAfter(LocalDateTime.now())) {

            throw AccountActivationException.vencido();
        }
    }

    public void validarCuentaPendiente(
            UsuarioSistema usuario) {

        if (usuario == null) {
            throw AccountActivationException.invalido();
        }

        if (usuario.getEstadoActivacion()
                == EstadoActivacionCuenta.ACTIVA) {

            throw AccountActivationException.usado();
        }

        if (usuario.getEstadoActivacion()
                != EstadoActivacionCuenta.PENDIENTE
                || usuario.getPasswordHash() != null) {

            throw AccountActivationException.invalido();
        }
    }
}