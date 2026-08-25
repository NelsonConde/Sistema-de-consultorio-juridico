package co.edu.ufps.legal_cases.security.service.invariant.administracion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.AdministrativoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.PermisoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.RolEstado;

class AdministracionInvariantPolicyTest {

    private AdministracionInvariantPolicy policy;
    private AdministracionSnapshotProjector projector;

    @BeforeEach
    void setUp() {
        policy = new AdministracionInvariantPolicy(
                new AdministracionCapabilityEvaluator());
        projector = new AdministracionSnapshotProjector();
    }

    @Test
    void permiteRetirarUnAdministradorSiOtroConservaContinuidad() {
        AdministracionInvariantSnapshot actual = snapshotDosDirectoras();

        assertDoesNotThrow(() -> policy.validarTransicion(
                actual,
                projector.conUsuarioActivo(actual, 10L, false),
                999L));
    }

    @Test
    void rechazaUltimoAdministrador() {
        AdministracionInvariantSnapshot actual = snapshotUnaDirectora();

        AdministracionInvariantException ex = assertThrows(
                AdministracionInvariantException.class,
                () -> policy.validarTransicion(
                        actual,
                        projector.conUsuarioActivo(
                                actual,
                                10L,
                                false),
                        999L));

        assertEquals(
                "La operación dejaría el sistema sin administradores operativos",
                ex.getMessage());
    }

    @Test
    void rechazaUltimaDirectora() {
        AdministracionInvariantSnapshot actual = snapshotUnaDirectora();

        assertThrows(
                AdministracionInvariantException.class,
                () -> policy.validarTransicion(
                        actual,
                        projector.conDirectora(actual, 1L, false),
                        999L));
    }

    @Test
    void rechazaUltimaCapacidad() {
        AdministracionInvariantSnapshot actual = snapshotUnaDirectora();
        Long permisoId = permisoId(actual, GESTIONAR_PERMISOS);

        assertThrows(
                AdministracionInvariantException.class,
                () -> policy.validarTransicion(
                        actual,
                        projector.sinPermisoRol(
                                actual,
                                100L,
                                permisoId),
                        999L));
    }

    @Test
    void rechazaAutoDesactivacion() {
        AdministracionInvariantSnapshot actual = snapshotDosDirectoras();

        assertThrows(
                AdministracionInvariantException.class,
                () -> policy.validarTransicion(
                        actual,
                        projector.conUsuarioActivo(
                                actual,
                                10L,
                                false),
                        10L));
    }

    @Test
    void rechazaAutoRetiroDeDirectora() {
        AdministracionInvariantSnapshot actual = snapshotDosDirectoras();

        assertThrows(
                AdministracionInvariantException.class,
                () -> policy.validarTransicion(
                        actual,
                        projector.conDirectora(actual, 1L, false),
                        10L));
    }

    @Test
    void rechazaPerdidaDeCapacidadDelPropioRol() {
        AdministracionInvariantSnapshot actual = snapshotDosDirectoras();
        Long permisoId = permisoId(actual, ACCEDER_ROLES);

        assertThrows(
                AdministracionInvariantException.class,
                () -> policy.validarTransicion(
                        actual,
                        projector.sinPermisoRol(
                                actual,
                                100L,
                                permisoId),
                        10L));
    }

    private AdministracionInvariantSnapshot snapshotUnaDirectora() {
        AdministracionInvariantSnapshot completo = snapshotDosDirectoras();

        return new AdministracionInvariantSnapshot(
                Map.of(1L, completo.administrativos().get(1L)),
                Map.of(100L, completo.roles().get(100L)),
                completo.permisos());
    }

    private AdministracionInvariantSnapshot snapshotDosDirectoras() {
        Map<Long, PermisoEstado> permisos = permisos();
        Set<Long> permisoIds = permisos.keySet();

        return new AdministracionInvariantSnapshot(
                Map.of(
                        1L,
                        new AdministrativoEstado(
                                1L, 10L, true,
                                TipoPerfilUsuario.ADMINISTRATIVO,
                                true, true, 100L),
                        2L,
                        new AdministrativoEstado(
                                2L, 20L, true,
                                TipoPerfilUsuario.ADMINISTRATIVO,
                                true, true, 200L)),
                Map.of(
                        100L,
                        new RolEstado(
                                100L,
                                true,
                                TipoPerfilUsuario.ADMINISTRATIVO,
                                permisoIds),
                        200L,
                        new RolEstado(
                                200L,
                                true,
                                TipoPerfilUsuario.ADMINISTRATIVO,
                                permisoIds)),
                permisos);
    }

    private Map<Long, PermisoEstado> permisos() {
        Set<String> nombres = Set.of(
                ACCEDER_ADMINISTRACION,
                ACCEDER_ROLES,
                GESTIONAR_USUARIOS,
                GESTIONAR_ROLES,
                GESTIONAR_PERMISOS,
                GESTIONAR_ADMINISTRADORES);

        Map<Long, PermisoEstado> resultado = new LinkedHashMap<>();
        long id = 1000L;

        for (String nombre : nombres.stream().sorted().toList()) {
            resultado.put(
                    id,
                    new PermisoEstado(id, nombre, true));
            id++;
        }

        return resultado;
    }

    private Long permisoId(
            AdministracionInvariantSnapshot snapshot,
            String nombre) {

        return snapshot.permisos().values().stream()
                .filter(p -> nombre.equals(p.nombre()))
                .map(PermisoEstado::permisoId)
                .findFirst()
                .orElseThrow();
    }
}