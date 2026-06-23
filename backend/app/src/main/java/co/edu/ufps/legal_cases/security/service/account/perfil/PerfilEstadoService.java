package co.edu.ufps.legal_cases.security.service.account.perfil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.account.perfil.estado.PerfilEstadoHandlerRegistry;

// Orquesta la desactivación del perfil actual del usuario.
// La lógica específica por tipo de perfil está delegada a estrategias.
@Service
@Transactional
public class PerfilEstadoService {

    private final PerfilEstadoHandlerRegistry perfilEstadoHandlerRegistry;

    public PerfilEstadoService(PerfilEstadoHandlerRegistry perfilEstadoHandlerRegistry) {
        this.perfilEstadoHandlerRegistry = perfilEstadoHandlerRegistry;
    }

    public void desactivarPerfilActual(Long usuarioSistemaId, TipoPerfilUsuario tipoPerfilActual) {
        if (usuarioSistemaId == null) {
            throw new BusinessException("El id del usuario del sistema es obligatorio");
        }

        if (tipoPerfilActual == null) {
            throw new BusinessException("El tipo de perfil actual es obligatorio");
        }

        perfilEstadoHandlerRegistry
                .obtenerHandler(tipoPerfilActual)
                .desactivarPerfilActual(usuarioSistemaId);
    }
}