package co.edu.ufps.legal_cases.security.service.account.cambio;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilBaseDTO;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Organiza los handlers de cambio de perfil por tipo de perfil.
// Así el service principal no tiene que conocer cómo se arma el mapa interno.
@Component
public class PerfilCambioHandlerRegistry {

    private final Map<TipoPerfilUsuario, PerfilCambioHandler<? extends CambiarPerfilBaseDTO>> handlers;

    public PerfilCambioHandlerRegistry(List<PerfilCambioHandler<? extends CambiarPerfilBaseDTO>> handlers) {
        this.handlers = construirMapaHandlers(handlers);
    }

    // En una especie de diccionario organiza por tipo de perfil el manejador de cada uno.
    private Map<TipoPerfilUsuario, PerfilCambioHandler<? extends CambiarPerfilBaseDTO>> construirMapaHandlers(
            List<PerfilCambioHandler<? extends CambiarPerfilBaseDTO>> handlers) {

        Map<TipoPerfilUsuario, PerfilCambioHandler<? extends CambiarPerfilBaseDTO>> mapa =
                new EnumMap<>(TipoPerfilUsuario.class);

        // Recorre todos los manejadores para irlos asociando a un tipo de perfil.
        for (PerfilCambioHandler<? extends CambiarPerfilBaseDTO> handler : handlers) {
            // Verifica que no haya más de un manejador asociado al mismo perfil.
            if (mapa.containsKey(handler.getTipoPerfil())) {
                throw new BusinessException(
                        "Hay más de un handler registrado para el perfil " + handler.getTipoPerfil());
            }

            mapa.put(handler.getTipoPerfil(), handler);
        }

        return mapa;
    }

    // Como se usa de forma genérica se debe validar que exista un manejador
    // de cambio y que el DTO corresponda al perfil destino.
    @SuppressWarnings("unchecked") // El cast es seguro porque antes se valida con getDtoClass().
    public <T extends CambiarPerfilBaseDTO> PerfilCambioHandler<T> obtenerHandler(
            TipoPerfilUsuario tipoPerfilDestino,
            T dto) {

        // Obtiene el manejador del perfil al cual va a cambiar.
        PerfilCambioHandler<? extends CambiarPerfilBaseDTO> handler = handlers.get(tipoPerfilDestino);

        if (handler == null) {
            throw new BusinessException("No existe handler para el perfil destino " + tipoPerfilDestino);
        }

        // Verifica que el DTO sea el que el manejador usa.
        if (!handler.getDtoClass().isInstance(dto)) {
            throw new BusinessException("Los datos enviados no corresponden al perfil destino " + tipoPerfilDestino);
        }

        return (PerfilCambioHandler<T>) handler;
    }
}