package co.edu.ufps.legal_cases.security.repository.invariant;

import java.util.List;

import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

/**
 * Centraliza las consultas y bloqueos necesarios para evaluar de forma
 * consistente el estado administrativo durante una transacción.
 */
@Repository
public class AdministracionInvariantRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<RolLectura> bloquearYCargarRolesAdministrativos() {

        // Los roles administrativos funcionan como punto común de
        // serialización para las operaciones que pueden reducir administración.
        return entityManager.createQuery("""
                        SELECT r
                        FROM Rol r
                        WHERE r.tipoPerfil = :tipoPerfil
                        ORDER BY r.id
                        """, Rol.class)
                .setParameter(
                        "tipoPerfil",
                        TipoPerfilUsuario.ADMINISTRATIVO)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .stream()
                .map(rol -> new RolLectura(
                        rol.getId(),
                        rol.getActivo(),
                        rol.getTipoPerfil()))
                .toList();
    }

    public List<RolPermisoLectura>
            cargarPermisosDeRolesAdministrativos() {

        return entityManager.createQuery("""
                        SELECT
                            r.id,
                            p.id
                        FROM Rol r
                        JOIN r.permisos p
                        WHERE r.tipoPerfil = :tipoPerfil
                        ORDER BY r.id, p.id
                        """, Tuple.class)
                .setParameter(
                        "tipoPerfil",
                        TipoPerfilUsuario.ADMINISTRATIVO)
                .getResultList()
                .stream()
                .map(fila -> new RolPermisoLectura(
                        fila.get(0, Long.class),
                        fila.get(1, Long.class)))
                .toList();
    }

    public List<PermisoLectura> cargarPermisos() {

        // Se consulta el catálogo completo porque una actualización de rol
        // puede proponer permisos que todavía no están asociados a ese rol.
        return entityManager.createQuery("""
                        SELECT
                            p.id,
                            p.nombre,
                            p.activo
                        FROM Permiso p
                        ORDER BY p.id
                        """, Tuple.class)
                .getResultList()
                .stream()
                .map(fila -> new PermisoLectura(
                        fila.get(0, Long.class),
                        fila.get(1, String.class),
                        fila.get(2, Boolean.class)))
                .toList();
    }

    public record RolLectura(
            Long rolId,
            Boolean activo,
            TipoPerfilUsuario tipoPerfil) {
    }

    public record RolPermisoLectura(
            Long rolId,
            Long permisoId) {
    }

    public record PermisoLectura(
            Long permisoId,
            String nombre,
            Boolean activo) {
    }
}