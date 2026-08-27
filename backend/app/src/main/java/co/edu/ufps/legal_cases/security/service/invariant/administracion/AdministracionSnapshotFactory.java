package co.edu.ufps.legal_cases.security.service.invariant.administracion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import co.edu.ufps.legal_cases.security.repository.invariant.AdministracionInvariantRepository;
import co.edu.ufps.legal_cases.security.repository.invariant.AdministracionInvariantRepository.PermisoLectura;
import co.edu.ufps.legal_cases.security.repository.invariant.AdministracionInvariantRepository.RolLectura;
import co.edu.ufps.legal_cases.security.repository.invariant.AdministracionInvariantRepository.RolPermisoLectura;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.AdministrativoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.PermisoEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot.RolEstado;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.port.AdministrativoInvariantReader;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.port.AdministrativoInvariantReader.AdministrativoLectura;

/**
 * Construye una fotografía coherente del estado administrativo actual
 * a partir de roles, permisos y perfiles obtenidos bajo bloqueo.
 */
@Component
public class AdministracionSnapshotFactory {

    private final AdministracionInvariantRepository invariantRepository;
    private final AdministrativoInvariantReader administrativoReader;

    public AdministracionSnapshotFactory(
            AdministracionInvariantRepository invariantRepository,
            AdministrativoInvariantReader administrativoReader) {

        this.invariantRepository = invariantRepository;
        this.administrativoReader = administrativoReader;
    }

    public AdministracionInvariantSnapshot cargarBajoBloqueo() {

        List<RolLectura> roles =
                invariantRepository.bloquearYCargarRolesAdministrativos();

        List<RolPermisoLectura> permisosPorRol =
                invariantRepository.cargarPermisosDeRolesAdministrativos();

        List<PermisoLectura> permisos =
                invariantRepository.cargarPermisos();

        List<AdministrativoLectura> administrativos =
                administrativoReader.cargarEstadoAdministrativo();

        return new AdministracionInvariantSnapshot(
                construirAdministrativos(administrativos),
                construirRoles(roles, permisosPorRol),
                construirPermisos(permisos));
    }

    private Map<Long, AdministrativoEstado> construirAdministrativos(
            List<AdministrativoLectura> lecturas) {

        Map<Long, AdministrativoEstado> administrativos = new HashMap<>();

        for (AdministrativoLectura lectura : lecturas) {
            administrativos.put(
                    lectura.administrativoId(),
                    new AdministrativoEstado(
                            lectura.administrativoId(),
                            lectura.usuarioSistemaId(),
                            lectura.usuarioActivo(),
                            lectura.tipoPerfilActual(),
                            lectura.administrativoActivo(),
                            lectura.directora(),
                            lectura.rolId()));
        }

        return administrativos;
    }

    private Map<Long, RolEstado> construirRoles(
            List<RolLectura> roles,
            List<RolPermisoLectura> permisosPorRol) {

        Map<Long, Set<Long>> idsPermisosPorRol =
                agruparPermisos(permisosPorRol);

        Map<Long, RolEstado> resultado = new HashMap<>();

        for (RolLectura rol : roles) {
            resultado.put(
                    rol.rolId(),
                    new RolEstado(
                            rol.rolId(),
                            rol.activo(),
                            rol.tipoPerfil(),
                            idsPermisosPorRol.getOrDefault(
                                    rol.rolId(),
                                    Set.of())));
        }

        return resultado;
    }

    private Map<Long, Set<Long>> agruparPermisos(
            List<RolPermisoLectura> lecturas) {

        Map<Long, Set<Long>> permisosPorRol = new HashMap<>();

        for (RolPermisoLectura lectura : lecturas) {
            permisosPorRol
                    .computeIfAbsent(
                            lectura.rolId(),
                            ignored -> new HashSet<>())
                    .add(lectura.permisoId());
        }

        return permisosPorRol;
    }

    private Map<Long, PermisoEstado> construirPermisos(
            List<PermisoLectura> lecturas) {

        Map<Long, PermisoEstado> permisos = new HashMap<>();

        for (PermisoLectura lectura : lecturas) {
            permisos.put(
                    lectura.permisoId(),
                    new PermisoEstado(
                            lectura.permisoId(),
                            lectura.nombre(),
                            lectura.activo()));
        }

        return permisos;
    }
}