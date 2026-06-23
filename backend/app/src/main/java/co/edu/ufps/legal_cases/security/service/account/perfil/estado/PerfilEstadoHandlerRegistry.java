package co.edu.ufps.legal_cases.security.service.account.perfil.estado;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Registra los handlers de estado de perfil por tipo.
// Así el servicio orquestador no conoce detalles de asesor, estudiante, monitor, etc.
@Component
public class PerfilEstadoHandlerRegistry {

    private final Map<TipoPerfilUsuario, PerfilEstadoHandler> handlers;

    public PerfilEstadoHandlerRegistry(List<PerfilEstadoHandler> handlers) {
        this.handlers = construirMapaHandlers(handlers);
    }

    private Map<TipoPerfilUsuario, PerfilEstadoHandler> construirMapaHandlers(
            List<PerfilEstadoHandler> handlers) {

        Map<TipoPerfilUsuario, PerfilEstadoHandler> mapa =
                new EnumMap<>(TipoPerfilUsuario.class);

        for (PerfilEstadoHandler handler : handlers) {
            if (mapa.containsKey(handler.getTipoPerfil())) {
                throw new BusinessException(
                        "Hay más de un handler de estado registrado para el perfil " + handler.getTipoPerfil());
            }

            mapa.put(handler.getTipoPerfil(), handler);
        }

        return mapa;
    }

    public PerfilEstadoHandler obtenerHandler(TipoPerfilUsuario tipoPerfil) {
        if (tipoPerfil == null) {
            throw new BusinessException("El tipo de perfil es obligatorio");
        }

        PerfilEstadoHandler handler = handlers.get(tipoPerfil);

        if (handler == null) {
            throw new BusinessException("No existe handler de estado para el perfil " + tipoPerfil);
        }

        return handler;
    }
}