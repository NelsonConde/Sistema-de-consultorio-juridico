package co.edu.ufps.legal_cases.security.service.invariant.administracion;

import java.util.Set;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;

@Service
public class AdministracionInvariantService {

    private final AdministracionSnapshotFactory snapshotFactory;
    private final AdministracionSnapshotProjector snapshotProjector;
    private final AdministracionInvariantPolicy invariantPolicy;
    private final UsuarioActualService usuarioActualService;

    public AdministracionInvariantService(
            AdministracionSnapshotFactory snapshotFactory,
            AdministracionSnapshotProjector snapshotProjector,
            AdministracionInvariantPolicy invariantPolicy,
            UsuarioActualService usuarioActualService) {

        this.snapshotFactory = snapshotFactory;
        this.snapshotProjector = snapshotProjector;
        this.invariantPolicy = invariantPolicy;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarCambioEstadoUsuario(Long id, Boolean activo) {
        if (!Boolean.FALSE.equals(activo)) {
            return;
        }

        validar(snapshot ->
                snapshotProjector.conUsuarioActivo(snapshot, id, false));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarCambioPerfil(
            Long id,
            TipoPerfilUsuario tipoDestino) {

        if (tipoDestino == TipoPerfilUsuario.ADMINISTRATIVO) {
            return;
        }

        validar(snapshot ->
                snapshotProjector.conTipoPerfilUsuario(
                        snapshot,
                        id,
                        tipoDestino));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarCambioEstadoAdministrativo(
            Long id,
            Boolean activo) {

        if (!Boolean.FALSE.equals(activo)) {
            return;
        }

        validar(snapshot ->
                snapshotProjector.conAdministrativoActivo(
                        snapshot,
                        id,
                        false));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarEliminacionAdministrativo(Long id) {
        validar(snapshot ->
                snapshotProjector.conAdministrativoActivo(
                        snapshot,
                        id,
                        false));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarCambioDirectora(
            Long id,
            Boolean directora) {

        if (!Boolean.FALSE.equals(directora)) {
            return;
        }

        validar(snapshot ->
                snapshotProjector.conDirectora(snapshot, id, false));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarActualizacionRol(
            Long rolId,
            Boolean activo,
            Set<Long> permisoIds) {

        validar(snapshot -> {
            AdministracionInvariantSnapshot propuesto = snapshot;

            if (activo != null) {
                propuesto = snapshotProjector.conRolActivo(
                        propuesto,
                        rolId,
                        activo);
            }

            if (permisoIds != null) {
                propuesto = snapshotProjector.conPermisosRol(
                        propuesto,
                        rolId,
                        permisoIds);
            }

            return propuesto;
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarCambioEstadoRol(Long rolId, Boolean activo) {
        if (!Boolean.FALSE.equals(activo)) {
            return;
        }

        validar(snapshot ->
                snapshotProjector.conRolActivo(snapshot, rolId, false));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarRetiroPermisoRol(
            Long rolId,
            Long permisoId) {

        validar(snapshot ->
                snapshotProjector.sinPermisoRol(
                        snapshot,
                        rolId,
                        permisoId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarActualizacionPermiso(
            Long permisoId,
            String nombre,
            Boolean activo) {

        validar(snapshot ->
                snapshotProjector.conPermiso(
                        snapshot,
                        permisoId,
                        nombre,
                        activo));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarCambioEstadoPermiso(
            Long permisoId,
            Boolean activo) {

        if (!Boolean.FALSE.equals(activo)) {
            return;
        }

        validar(snapshot ->
                snapshotProjector.conPermiso(
                        snapshot,
                        permisoId,
                        null,
                        false));
    }

    private void validar(
            UnaryOperator<AdministracionInvariantSnapshot> transicion) {

        AdministracionInvariantSnapshot actual =
                snapshotFactory.cargarBajoBloqueo();

        AdministracionInvariantSnapshot propuesto =
                transicion.apply(actual);

        if (actual.equals(propuesto)) {
            return;
        }

        Long actorId = usuarioActualService.obtenerUsuarioActualId();

        invariantPolicy.validarTransicion(
                actual,
                propuesto,
                actorId);
    }
}