package co.edu.ufps.legal_cases.security.service.account;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.dto.account.MiPerfilDTO;
import co.edu.ufps.legal_cases.security.dto.account.PerfilContactoDatos;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

@Component
public class MiPerfilMapper {

    public MiPerfilDTO convertirADTO(UsuarioSistema usuario, PerfilContactoDatos contacto) {
        MiPerfilDTO dto = new MiPerfilDTO();

        dto.setUsername(usuario.getUsername());
        dto.setRolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        dto.setTipoPerfil(usuario.getTipoPerfilActual() != null ? usuario.getTipoPerfilActual().name() : null);

        dto.setNombre(contacto.getNombre());
        dto.setEmail(contacto.getEmail());
        dto.setTelefono(contacto.getTelefono());
        dto.setSede(contacto.getSedeNombre());
        dto.setCodigo(contacto.getCodigo());

        return dto;
    }
}