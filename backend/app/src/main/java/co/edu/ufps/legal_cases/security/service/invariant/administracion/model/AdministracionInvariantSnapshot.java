package co.edu.ufps.legal_cases.security.service.invariant.administracion.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;

// Representa una fotografía inmutable del estado administrativo.
// Permite evaluar un cambio sin modificar primero las entidades JPA.
public record AdministracionInvariantSnapshot(
        Map<Long, AdministrativoEstado> administrativos,
        Map<Long, RolEstado> roles,
        Map<Long, PermisoEstado> permisos) {

    public AdministracionInvariantSnapshot {
        administrativos = administrativos == null
                ? Map.of()
                : Map.copyOf(administrativos);

        roles = roles == null
                ? Map.of()
                : Map.copyOf(roles);

        permisos = permisos == null
                ? Map.of()
                : Map.copyOf(permisos);
    }

    public Optional<AdministrativoEstado> buscarPorUsuario(
            Long usuarioSistemaId) {

        if (usuarioSistemaId == null) {
            return Optional.empty();
        }

        return administrativos.values()
                .stream()
                .filter(administrativo -> Objects.equals(
                        administrativo.usuarioSistemaId(),
                        usuarioSistemaId))
                .findFirst();
    }

    public record AdministrativoEstado(
            Long administrativoId,
            Long usuarioSistemaId,
            Boolean usuarioActivo,
            TipoPerfilUsuario tipoPerfilActual,
            Boolean administrativoActivo,
            Boolean directora,
            Long rolId) {
    }

    public record RolEstado(
            Long rolId,
            Boolean activo,
            TipoPerfilUsuario tipoPerfil,
            Set<Long> permisoIds) {

        public RolEstado {
            permisoIds = permisoIds == null
                    ? Set.of()
                    : Set.copyOf(permisoIds);
        }
    }

    public record PermisoEstado(
            Long permisoId,
            String nombre,
            Boolean activo) {
    }
}