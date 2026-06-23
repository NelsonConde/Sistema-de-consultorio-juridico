package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdministrativoPerfilUsuarioActivoResolver implements PerfilUsuarioActivoResolver {

    private final AdministrativoRepository administrativoRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ADMINISTRATIVO;
    }

    @Override
    public PerfilUsuarioActual resolver(Long usuarioSistemaId) {
        Administrativo administrativo = administrativoRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El administrativo asociado al usuario no existe o se encuentra inactivo"));

        return new PerfilUsuarioActual(administrativo.getId(), TipoPerfilUsuario.ADMINISTRATIVO);
    }
}