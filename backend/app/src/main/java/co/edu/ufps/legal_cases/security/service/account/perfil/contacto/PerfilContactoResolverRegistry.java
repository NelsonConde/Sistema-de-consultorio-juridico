package co.edu.ufps.legal_cases.security.service.account.perfil.contacto;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Centraliza las estrategias disponibles para resolver contacto por perfil.
@Component
public class PerfilContactoResolverRegistry {

    private final Map<TipoPerfilUsuario, PerfilContactoResolver> resolvers;

    public PerfilContactoResolverRegistry(List<PerfilContactoResolver> resolvers) {
        this.resolvers = construirMapaResolvers(resolvers);
    }

    private Map<TipoPerfilUsuario, PerfilContactoResolver> construirMapaResolvers(
            List<PerfilContactoResolver> resolvers) {

        Map<TipoPerfilUsuario, PerfilContactoResolver> mapa =
                new EnumMap<>(TipoPerfilUsuario.class);

        for (PerfilContactoResolver resolver : resolvers) {
            if (mapa.containsKey(resolver.getTipoPerfil())) {
                throw new BusinessException(
                        "Hay mas de un resolver de contacto registrado para el perfil "
                                + resolver.getTipoPerfil());
            }

            mapa.put(resolver.getTipoPerfil(), resolver);
        }

        return mapa;
    }

    public PerfilContactoResolver obtenerResolver(TipoPerfilUsuario tipoPerfil) {
        if (tipoPerfil == null) {
            throw new BusinessException("El tipo de perfil es obligatorio");
        }

        PerfilContactoResolver resolver = resolvers.get(tipoPerfil);

        if (resolver == null) {
            throw new BusinessException("No existe resolver de contacto para el perfil " + tipoPerfil);
        }

        return resolver;
    }
}