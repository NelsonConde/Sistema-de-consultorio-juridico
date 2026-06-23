package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaResponsableOperacionService;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MonitorPerfilEstadoHandler implements PerfilEstadoHandler {

    private final MonitorRepository monitorRepository;
    private final ConsultaResponsableOperacionService consultaResponsableOperacionService;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.MONITOR;
    }

    @Override
    public void desactivarPerfilActual(Long usuarioSistemaId) {
        Monitor monitor = monitorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El monitor actual no existe o ya se encuentra inactivo"));

        consultaResponsableOperacionService.validarMonitorSinConsultasOperativas(monitor.getId());

        monitor.setActivo(false);
        monitorRepository.save(monitor);
    }
}