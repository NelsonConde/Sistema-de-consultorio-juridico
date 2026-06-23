package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AsesorPerfilEstadoHandler implements PerfilEstadoHandler {

    private final AsesorRepository asesorRepository;
    private final ConsultaResponsableOperacionService consultaResponsableOperacionService;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.ASESOR;
    }

    @Override
    public void desactivarPerfilActual(Long usuarioSistemaId) {
        Asesor asesor = asesorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El asesor actual no existe o ya se encuentra inactivo"));

        consultaResponsableOperacionService.validarAsesorSinConsultasOperativas(asesor.getId());

        asesor.setActivo(false);
        asesorRepository.save(asesor);
    }
}