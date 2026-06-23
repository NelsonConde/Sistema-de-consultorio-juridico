package co.edu.ufps.legal_cases.security.service.account.perfil.resolver;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Centraliza las estrategias disponibles para resolver perfiles activos.
@Component
public class PerfilUsuarioActivoResolverRegistry {

    private final Map<TipoPerfilUsuario, PerfilUsuarioActivoResolver> resolvers;

    public PerfilUsuarioActivoResolverRegistry(List<PerfilUsuarioActivoResolver> resolvers) {
        this.resolvers = construirMapaResolvers(resolvers);
    }

    private Map<TipoPerfilUsuario, PerfilUsuarioActivoResolver> construirMapaResolvers(
            List<PerfilUsuarioActivoResolver> resolvers) {

        Map<TipoPerfilUsuario, PerfilUsuarioActivoResolver> mapa =
                new EnumMap<>(TipoPerfilUsuario.class);

        for (PerfilUsuarioActivoResolver resolver : resolvers) {
            if (mapa.containsKey(resolver.getTipoPerfil())) {
                throw new BusinessException(
                        "Hay más de un resolver registrado para el perfil " + resolver.getTipoPerfil());
            }

            mapa.put(resolver.getTipoPerfil(), resolver);
        }

        return mapa;
    }

    public PerfilUsuarioActivoResolver obtenerResolver(TipoPerfilUsuario tipoPerfil) {
        if (tipoPerfil == null) {
            throw new BusinessException("El tipo de perfil es obligatorio");
        }

        PerfilUsuarioActivoResolver resolver = resolvers.get(tipoPerfil);

        if (resolver == null) {
            throw new BusinessException("No existe resolver para el perfil " + tipoPerfil);
        }

        return resolver;
    }
}