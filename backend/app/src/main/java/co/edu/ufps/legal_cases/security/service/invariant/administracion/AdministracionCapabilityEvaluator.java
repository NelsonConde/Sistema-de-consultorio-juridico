package co.edu.ufps.legal_cases.security.service.invariant.administracion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.AdministrativoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.PermisoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.RolEstado;

@Component
public class AdministracionCapabilityEvaluator {

    public boolean esAdministradorOperativo(
            AdministracionInvariantSnapshot snapshot,
            AdministrativoEstado administrativo) {

        if (administrativo == null
                || !Boolean.TRUE.equals(administrativo.usuarioActivo())
                || !Boolean.TRUE.equals(administrativo.administrativoActivo())
                || administrativo.tipoPerfilActual()
                        != TipoPerfilUsuario.ADMINISTRATIVO) {
            return false;
        }

        RolEstado rol = snapshot.roles().get(administrativo.rolId());

        return rol != null
                && Boolean.TRUE.equals(rol.activo())
                && rol.tipoPerfil() == TipoPerfilUsuario.ADMINISTRATIVO;
    }

    public boolean esDirectoraOperativa(
            AdministracionInvariantSnapshot snapshot,
            AdministrativoEstado administrativo) {

        return esAdministradorOperativo(snapshot, administrativo)
                && Boolean.TRUE.equals(administrativo.directora());
    }

    public Set<CapacidadAdministrativa> obtenerCapacidades(
            AdministracionInvariantSnapshot snapshot,
            AdministrativoEstado administrativo) {

        if (!esAdministradorOperativo(snapshot, administrativo)) {
            return Set.of();
        }

        RolEstado rol = snapshot.roles().get(administrativo.rolId());

        EnumSet<CapacidadAdministrativa> capacidades =
                EnumSet.noneOf(CapacidadAdministrativa.class);

        agregarSi(
                capacidades,
                CapacidadAdministrativa.ACCESO_ADMINISTRACION,
                tienePermiso(snapshot, rol, ACCEDER_ADMINISTRACION));

        agregarSi(
                capacidades,
                CapacidadAdministrativa.ACCESO_ROLES,
                tienePermiso(snapshot, rol, ACCEDER_ROLES));

        agregarSi(
                capacidades,
                CapacidadAdministrativa.GESTION_USUARIOS,
                puedeGestionarUsuarios(snapshot, rol));

        agregarSi(
                capacidades,
                CapacidadAdministrativa.GESTION_ROLES,
                puedeGestionarRoles(snapshot, rol));

        agregarSi(
                capacidades,
                CapacidadAdministrativa.GESTION_PERMISOS,
                tienePermiso(snapshot, rol, GESTIONAR_PERMISOS));

        agregarSi(
                capacidades,
                CapacidadAdministrativa.GESTION_ADMINISTRATIVOS,
                puedeGestionarAdministrativos(
                        snapshot,
                        administrativo,
                        rol));

        return Set.copyOf(capacidades);
    }

    public boolean tieneCapacidadRecuperacion(
            AdministracionInvariantSnapshot snapshot,
            AdministrativoEstado administrativo) {

        return esDirectoraOperativa(snapshot, administrativo)
                && obtenerCapacidades(snapshot, administrativo)
                        .containsAll(EnumSet.allOf(
                                CapacidadAdministrativa.class));
    }

    private boolean puedeGestionarUsuarios(
            AdministracionInvariantSnapshot snapshot,
            RolEstado rol) {

        return tienePermiso(snapshot, rol, GESTIONAR_USUARIOS)
                || (tienePermiso(snapshot, rol, VER_USUARIOS)
                    && tienePermiso(snapshot, rol, CAMBIAR_ESTADO_USUARIOS)
                    && tienePermiso(snapshot, rol, ASIGNAR_ROL_USUARIOS));
    }

    private boolean puedeGestionarRoles(
            AdministracionInvariantSnapshot snapshot,
            RolEstado rol) {

        return tienePermiso(snapshot, rol, GESTIONAR_ROLES)
                || (tienePermiso(snapshot, rol, VER_ROLES)
                    && tienePermiso(snapshot, rol, EDITAR_ROLES)
                    && tienePermiso(
                            snapshot,
                            rol,
                            ASIGNAR_PERMISOS_A_ROLES));
    }

    private boolean puedeGestionarAdministrativos(
            AdministracionInvariantSnapshot snapshot,
            AdministrativoEstado administrativo,
            RolEstado rol) {

        return Boolean.TRUE.equals(administrativo.directora())
                && (tienePermiso(
                        snapshot,
                        rol,
                        GESTIONAR_ADMINISTRADORES)
                    || tienePermiso(
                        snapshot,
                        rol,
                        GESTIONAR_USUARIOS));
    }

    private boolean tienePermiso(
            AdministracionInvariantSnapshot snapshot,
            RolEstado rol,
            String permisoRequerido) {

        if (rol == null || permisoRequerido == null) {
            return false;
        }

        return rol.permisoIds().stream()
                .map(snapshot.permisos()::get)
                .filter(Objects::nonNull)
                .filter(permiso ->
                        Boolean.TRUE.equals(permiso.activo()))
                .map(PermisoEstado::nombre)
                .anyMatch(permisoRequerido::equals);
    }

    private void agregarSi(
            Set<CapacidadAdministrativa> capacidades,
            CapacidadAdministrativa capacidad,
            boolean condicion) {

        if (condicion) {
            capacidades.add(capacidad);
        }
    }

    public enum CapacidadAdministrativa {
        ACCESO_ADMINISTRACION,
        ACCESO_ROLES,
        GESTION_USUARIOS,
        GESTION_ROLES,
        GESTION_PERMISOS,
        GESTION_ADMINISTRATIVOS
    }
}