package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdministrativoPerfilEstadoHandler implements PerfilEstadoHandler {

    private final AdministrativoRepository administrativoRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ADMINISTRATIVO;
    }

    @Override
    public void desactivarPerfilActual(Long usuarioSistemaId) {
        Administrativo administrativo = administrativoRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El administrativo actual no existe o ya se encuentra inactivo"));

        administrativo.setActivo(false);
        administrativoRepository.save(administrativo);
    }
}