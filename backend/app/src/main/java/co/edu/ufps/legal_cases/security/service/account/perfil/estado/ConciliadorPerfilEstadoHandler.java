package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConciliadorPerfilEstadoHandler implements PerfilEstadoHandler {

    private final ConciliadorRepository conciliadorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.CONCILIADOR;
    }

    @Override
    public void desactivarPerfilActual(Long usuarioSistemaId) {
        Conciliador conciliador = conciliadorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El conciliador actual no existe o ya se encuentra inactivo"));

        conciliador.setActivo(false);
        conciliadorRepository.save(conciliador);
    }
}