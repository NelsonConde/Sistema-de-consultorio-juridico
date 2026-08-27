package co.edu.ufps.legal_cases.security.service.invariant.administracion;

import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionCapabilityEvaluator.CapacidadAdministrativa;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.AdministrativoEstado;

@Component
public class AdministracionInvariantPolicy {

    private final AdministracionCapabilityEvaluator capabilityEvaluator;

    public AdministracionInvariantPolicy(
            AdministracionCapabilityEvaluator capabilityEvaluator) {
        this.capabilityEvaluator = capabilityEvaluator;
    }

    public void validarTransicion(
            AdministracionInvariantSnapshot actual,
            AdministracionInvariantSnapshot propuesto,
            Long usuarioActorId) {

        validarAutoafectacion(actual, propuesto, usuarioActorId);
        validarEstado(propuesto);
    }

    public void validarEstado(AdministracionInvariantSnapshot snapshot) {
        validarAdministradorOperativo(snapshot);
        validarDirectoraOperativa(snapshot);
        validarCapacidadRecuperacion(snapshot);
    }

    private void validarAdministradorOperativo(
            AdministracionInvariantSnapshot snapshot) {

        boolean existe = snapshot.administrativos().values().stream()
                .anyMatch(administrativo ->
                        capabilityEvaluator.esAdministradorOperativo(
                                snapshot,
                                administrativo));

        if (!existe) {
            throw new AdministracionInvariantException(
                    "La operación dejaría el sistema sin administradores operativos");
        }
    }

    private void validarDirectoraOperativa(
            AdministracionInvariantSnapshot snapshot) {

        boolean existe = snapshot.administrativos().values().stream()
                .anyMatch(administrativo ->
                        capabilityEvaluator.esDirectoraOperativa(
                                snapshot,
                                administrativo));

        if (!existe) {
            throw new AdministracionInvariantException(
                    "La operación dejaría el sistema sin una directora operativa");
        }
    }

    private void validarCapacidadRecuperacion(
            AdministracionInvariantSnapshot snapshot) {

        boolean existe = snapshot.administrativos().values().stream()
                .anyMatch(administrativo ->
                        capabilityEvaluator.tieneCapacidadRecuperacion(
                                snapshot,
                                administrativo));

        if (!existe) {
            throw new AdministracionInvariantException(
                    "La operación dejaría el sistema sin capacidad administrativa de recuperación");
        }
    }

    private void validarAutoafectacion(
            AdministracionInvariantSnapshot actual,
            AdministracionInvariantSnapshot propuesto,
            Long usuarioActorId) {

        if (usuarioActorId == null) {
            return;
        }

        AdministrativoEstado antes =
                actual.buscarPorUsuario(usuarioActorId).orElse(null);
        AdministrativoEstado despues =
                propuesto.buscarPorUsuario(usuarioActorId).orElse(null);

        if (antes == null || despues == null) {
            return;
        }

        boolean eraOperativo =
                capabilityEvaluator.esAdministradorOperativo(actual, antes);
        boolean sigueOperativo =
                capabilityEvaluator.esAdministradorOperativo(propuesto, despues);

        if (eraOperativo && !sigueOperativo) {
            throw new AdministracionInvariantException(
                    "Un administrativo no puede desactivar su propia condición operativa");
        }

        boolean eraDirectora =
                capabilityEvaluator.esDirectoraOperativa(actual, antes);
        boolean sigueDirectora =
                capabilityEvaluator.esDirectoraOperativa(propuesto, despues);

        if (eraDirectora && !sigueDirectora) {
            throw new AdministracionInvariantException(
                    "Una directora no puede retirarse a sí misma la condición de directora");
        }

        Set<CapacidadAdministrativa> capacidadesAntes =
                capabilityEvaluator.obtenerCapacidades(actual, antes);
        Set<CapacidadAdministrativa> capacidadesDespues =
                capabilityEvaluator.obtenerCapacidades(propuesto, despues);

        if (!capacidadesDespues.containsAll(capacidadesAntes)) {
            throw new AdministracionInvariantException(
                    "Un administrativo no puede retirarse a sí mismo capacidades administrativas críticas");
        }
    }
}