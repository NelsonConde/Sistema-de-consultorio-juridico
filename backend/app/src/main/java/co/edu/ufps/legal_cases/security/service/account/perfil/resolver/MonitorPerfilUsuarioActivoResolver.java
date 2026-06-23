package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.PerfilUsuarioActual;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MonitorPerfilUsuarioActivoResolver implements PerfilUsuarioActivoResolver {

    private final MonitorRepository monitorRepository;

    @Override
    public TipoPerfilUsuario getTipoPerfil() {
        return TipoPerfilUsuario.MONITOR;
    }

    @Override
    public PerfilUsuarioActual resolver(Long usuarioSistemaId) {
        Monitor monitor = monitorRepository.findByUsuarioSistema_IdAndActivoTrue(usuarioSistemaId)
                .orElseThrow(() -> new BusinessException(
                        "El monitor asociado al usuario no existe o se encuentra inactivo"));

        return new PerfilUsuarioActual(monitor.getId(), TipoPerfilUsuario.MONITOR);
    }
}