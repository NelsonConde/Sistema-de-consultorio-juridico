package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilBaseDTO;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

// Valida reglas locales del cambio de perfil.
// No consulta base de datos; solo valida objetos ya cargados por el service.
@Component
public class UsuarioCambioPerfilValidator {

    // Valida que los datos básicos para hacer el cambio no vengan vacíos.
    public void validarDatosCambio(
            Long usuarioSistemaId,
            TipoPerfilUsuario tipoPerfilDestino,
            CambiarPerfilBaseDTO dto) {

        if (usuarioSistemaId == null) {
            throw new BusinessException("El id del usuario del sistema es obligatorio");
        }

        if (tipoPerfilDestino == null) {
            throw new BusinessException("El tipo de perfil destino es obligatorio");
        }

        if (dto == null) {
            throw new BusinessException("Los datos para el cambio de perfil son obligatorios");
        }
    }

    // Se asegura que el usuario al que se le quiere cambiar el perfil pueda operar.
    public void validarUsuarioPuedeCambiarPerfil(UsuarioSistema usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException("No se puede cambiar el perfil de un usuario inactivo");
        }

        if (usuario.getRol() == null || !Boolean.TRUE.equals(usuario.getRol().getActivo())) {
            throw new BusinessException("No se puede cambiar el perfil porque el rol actual está inactivo");
        }
    }

    // Valida que el cambio no vaya a ser hacia el mismo perfil.
    public void validarPerfilDestinoDiferente(
            PerfilUsuarioActual perfilAnterior,
            TipoPerfilUsuario tipoPerfilDestino) {

        if (perfilAnterior.getTipoPerfil().equals(tipoPerfilDestino)) {
            throw new BusinessException("El usuario ya tiene activo el perfil " + tipoPerfilDestino);
        }
    }

    // Valida la existencia funcional del rol destino y que corresponda con el perfil destino.
    public void validarRolDestino(
            Rol rol,
            Long rolId,
            TipoPerfilUsuario tipoPerfilDestino) {

        if (rolId == null) {
            throw new BusinessException("El rol destino es obligatorio");
        }

        if (rol == null) {
            throw new BusinessException("Rol no encontrado con id: " + rolId);
        }

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new BusinessException("El rol destino se encuentra inactivo");
        }

        if (rol.getTipoPerfil() == null) {
            throw new BusinessException("El rol destino no tiene tipo de perfil configurado");
        }

        if (!rol.getTipoPerfil().equals(tipoPerfilDestino)) {
            throw new BusinessException("El rol destino no corresponde al perfil " + tipoPerfilDestino);
        }
    }
}