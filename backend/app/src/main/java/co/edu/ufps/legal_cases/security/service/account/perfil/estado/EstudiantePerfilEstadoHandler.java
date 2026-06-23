package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EstudiantePerfilEstadoHandler implements PerfilEstadoHandler {

    private final EstudianteRepository estudianteRepository;
    private final ConsultaResponsableOperacionService consultaResponsableOperacionService;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ESTUDIANTE;
    }

    @Override
    public void desactivarPerfilActual(Long usuarioSistemaId) {
        Estudiante estudiante = estudianteRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El estudiante actual no existe o ya se encuentra inactivo"));

        consultaResponsableOperacionService.validarEstudianteSinConsultasOperativas(estudiante.getId());

        estudiante.setActivo(false);
        estudianteRepository.save(estudiante);
    }
}