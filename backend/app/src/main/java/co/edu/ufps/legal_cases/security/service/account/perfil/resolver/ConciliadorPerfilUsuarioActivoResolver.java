package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConciliadorPerfilUsuarioActivoResolver implements PerfilUsuarioActivoResolver {

    private final ConciliadorRepository conciliadorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.CONCILIADOR;
    }

    @Override
    public PerfilUsuarioActual resolver(Long usuarioSistemaId) {
        Conciliador conciliador = conciliadorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El conciliador asociado al usuario no existe o se encuentra inactivo"));

        return new PerfilUsuarioActual(conciliador.getId(), TipoPerfilUsuario.CONCILIADOR);
    }
}