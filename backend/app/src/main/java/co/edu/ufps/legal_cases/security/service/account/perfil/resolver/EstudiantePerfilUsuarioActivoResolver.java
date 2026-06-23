package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EstudiantePerfilUsuarioActivoResolver implements PerfilUsuarioActivoResolver {

    private final EstudianteRepository estudianteRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ESTUDIANTE;
    }

    @Override
    public PerfilUsuarioActual resolver(Long usuarioSistemaId) {
        Estudiante estudiante = estudianteRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El estudiante asociado al usuario no existe o se encuentra inactivo"));

        return new PerfilUsuarioActual(estudiante.getId(), TipoPerfilUsuario.ESTUDIANTE);
    }
}