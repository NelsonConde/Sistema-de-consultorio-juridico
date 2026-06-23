package co.edu.ufps.legal_cases.security.service.auth.login;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.dto.account.UsuarioSistemaDTO;
import co.edu.ufps.legal_cases.security.dto.auth.login.LoginResponseDTO;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.account.usuario.UsuarioSistemaMapper;

// Centraliza la construcción de la respuesta de login.
// El AuthService no debe conocer los detalles de mapeo del usuario.
@Component
public class LoginMapper {

    private final UsuarioSistemaMapper usuarioSistemaMapper;

    public LoginMapper(UsuarioSistemaMapper usuarioSistemaMapper) {
        this.usuarioSistemaMapper = usuarioSistemaMapper;
    }

    public LoginResponseDTO convertirAResponse(
            UsuarioSistema usuario,
            PerfilUsuarioActual perfilActual) {

        UsuarioSistemaDTO usuarioDTO = usuarioSistemaMapper.convertirADTO(usuario);

        return new LoginResponseDTO(
                usuarioDTO.getId(),
                usuarioDTO.getUsername(),
                usuarioDTO.getRolId(),
                usuarioDTO.getRolNombre(),
                perfilActual.getPerfilId(),
                perfilActual.getTipoPerfil().name(),
                usuarioDTO.getPermisos());
    }
}