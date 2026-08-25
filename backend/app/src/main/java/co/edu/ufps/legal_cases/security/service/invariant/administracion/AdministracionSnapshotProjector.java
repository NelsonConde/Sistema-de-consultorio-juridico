package co.edu.ufps.legal_cases.security.service.invariant.administracion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.AdministrativoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.PermisoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.RolEstado;

@Component
public class AdministracionSnapshotProjector {

    public AdministracionInvariantSnapshot conUsuarioActivo(
            AdministracionInvariantSnapshot snapshot,
            Long usuarioSistemaId,
            Boolean activo) {

        AdministrativoEstado actual = snapshot.buscarPorUsuario(usuarioSistemaId)
                .orElse(null);

        if (actual == null
                || Objects.equals(actual.usuarioActivo(), activo)) {
            return snapshot;
        }

        return reemplazarAdministrativo(
                snapshot,
                new AdministrativoEstado(
                        actual.administrativoId(),
                        actual.usuarioSistemaId(),
                        activo,
                        actual.tipoPerfilActual(),
                        actual.administrativoActivo(),
                        actual.directora(),
                        actual.rolId()));
    }

    public AdministracionInvariantSnapshot conTipoPerfilUsuario(
            AdministracionInvariantSnapshot snapshot,
            Long usuarioSistemaId,
            TipoPerfilUsuario tipoPerfil) {

        AdministrativoEstado actual = snapshot.buscarPorUsuario(usuarioSistemaId)
                .orElse(null);

        if (actual == null || actual.tipoPerfilActual() == tipoPerfil) {
            return snapshot;
        }

        return reemplazarAdministrativo(
                snapshot,
                new AdministrativoEstado(
                        actual.administrativoId(),
                        actual.usuarioSistemaId(),
                        actual.usuarioActivo(),
                        tipoPerfil,
                        actual.administrativoActivo(),
                        actual.directora(),
                        actual.rolId()));
    }

    public AdministracionInvariantSnapshot conAdministrativoActivo(
            AdministracionInvariantSnapshot snapshot,
            Long administrativoId,
            Boolean activo) {

        AdministrativoEstado actual =
                snapshot.administrativos().get(administrativoId);

        if (actual == null
                || Objects.equals(actual.administrativoActivo(), activo)) {
            return snapshot;
        }

        return reemplazarAdministrativo(
                snapshot,
                new AdministrativoEstado(
                        actual.administrativoId(),
                        actual.usuarioSistemaId(),
                        actual.usuarioActivo(),
                        actual.tipoPerfilActual(),
                        activo,
                        actual.directora(),
                        actual.rolId()));
    }

    public AdministracionInvariantSnapshot conDirectora(
            AdministracionInvariantSnapshot snapshot,
            Long administrativoId,
            Boolean directora) {

        AdministrativoEstado actual =
                snapshot.administrativos().get(administrativoId);

        if (actual == null
                || Objects.equals(actual.directora(), directora)) {
            return snapshot;
        }

        return reemplazarAdministrativo(
                snapshot,
                new AdministrativoEstado(
                        actual.administrativoId(),
                        actual.usuarioSistemaId(),
                        actual.usuarioActivo(),
                        actual.tipoPerfilActual(),
                        actual.administrativoActivo(),
                        directora,
                        actual.rolId()));
    }

    public AdministracionInvariantSnapshot conRolActivo(
            AdministracionInvariantSnapshot snapshot,
            Long rolId,
            Boolean activo) {

        RolEstado actual = snapshot.roles().get(rolId);

        if (actual == null || Objects.equals(actual.activo(), activo)) {
            return snapshot;
        }

        return reemplazarRol(
                snapshot,
                new RolEstado(
                        actual.rolId(),
                        activo,
                        actual.tipoPerfil(),
                        actual.permisoIds()));
    }

    public AdministracionInvariantSnapshot conPermisosRol(
            AdministracionInvariantSnapshot snapshot,
            Long rolId,
            Set<Long> permisoIds) {

        RolEstado actual = snapshot.roles().get(rolId);

        if (actual == null || permisoIds == null) {
            return snapshot;
        }

        Set<Long> propuestos = Set.copyOf(permisoIds);

        if (actual.permisoIds().equals(propuestos)) {
            return snapshot;
        }

        return reemplazarRol(
                snapshot,
                new RolEstado(
                        actual.rolId(),
                        actual.activo(),
                        actual.tipoPerfil(),
                        propuestos));
    }

    public AdministracionInvariantSnapshot sinPermisoRol(
            AdministracionInvariantSnapshot snapshot,
            Long rolId,
            Long permisoId) {

        RolEstado actual = snapshot.roles().get(rolId);

        if (actual == null
                || permisoId == null
                || !actual.permisoIds().contains(permisoId)) {
            return snapshot;
        }

        Set<Long> propuestos = new HashSet<>(actual.permisoIds());
        propuestos.remove(permisoId);

        return conPermisosRol(snapshot, rolId, propuestos);
    }

    public AdministracionInvariantSnapshot conPermiso(
            AdministracionInvariantSnapshot snapshot,
            Long permisoId,
            String nombre,
            Boolean activo) {

        PermisoEstado actual = snapshot.permisos().get(permisoId);

        if (actual == null) {
            return snapshot;
        }

        String nombrePropuesto = nombre != null ? nombre : actual.nombre();
        Boolean activoPropuesto = activo != null ? activo : actual.activo();

        if (Objects.equals(actual.nombre(), nombrePropuesto)
                && Objects.equals(actual.activo(), activoPropuesto)) {
            return snapshot;
        }

        Map<Long, PermisoEstado> permisos =
                new HashMap<>(snapshot.permisos());

        permisos.put(
                permisoId,
                new PermisoEstado(
                        permisoId,
                        nombrePropuesto,
                        activoPropuesto));

        return new AdministracionInvariantSnapshot(
                snapshot.administrativos(),
                snapshot.roles(),
                permisos);
    }

    private AdministracionInvariantSnapshot reemplazarAdministrativo(
            AdministracionInvariantSnapshot snapshot,
            AdministrativoEstado propuesto) {

        Map<Long, AdministrativoEstado> administrativos =
                new HashMap<>(snapshot.administrativos());

        administrativos.put(propuesto.administrativoId(), propuesto);

        return new AdministracionInvariantSnapshot(
                administrativos,
                snapshot.roles(),
                snapshot.permisos());
    }

    private AdministracionInvariantSnapshot reemplazarRol(
            AdministracionInvariantSnapshot snapshot,
            RolEstado propuesto) {

        Map<Long, RolEstado> roles = new HashMap<>(snapshot.roles());
        roles.put(propuesto.rolId(), propuesto);

        return new AdministracionInvariantSnapshot(
                snapshot.administrativos(),
                roles,
                snapshot.permisos());
    }
}