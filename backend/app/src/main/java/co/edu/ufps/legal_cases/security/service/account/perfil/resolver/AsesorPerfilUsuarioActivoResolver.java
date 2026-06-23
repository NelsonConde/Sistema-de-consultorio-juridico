package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AsesorPerfilUsuarioActivoResolver implements PerfilUsuarioActivoResolver {

    private final AsesorRepository asesorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ASESOR;
    }

    @Override
    public PerfilUsuarioActual resolver(Long usuarioSistemaId) {
        Asesor asesor = asesorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El asesor asociado al usuario no existe o se encuentra inactivo"));

        return new PerfilUsuarioActual(asesor.getId(), TipoPerfilUsuario.ASESOR);
    }
}