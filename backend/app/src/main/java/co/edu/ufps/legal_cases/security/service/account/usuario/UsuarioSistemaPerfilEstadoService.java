package co.edu.ufps.legal_cases.security.service.account.usuario;

import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioSistemaPerfilEstadoService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;

    public void sincronizarEstadoSiExiste(UsuarioSistema usuarioSistema, Boolean activo) {
        if (usuarioSistema == null || usuarioSistema.getId() == null || activo == null) {
            return;
        }

        if (activo.equals(usuarioSistema.getActivo())) {
            return;
        }

        usuarioSistema.setActivo(activo);
        usuarioSistemaRepository.save(usuarioSistema);
    }
}