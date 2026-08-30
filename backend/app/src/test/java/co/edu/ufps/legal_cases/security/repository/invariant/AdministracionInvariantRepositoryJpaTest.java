package co.edu.ufps.legal_cases.security.repository.invariant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class AdministracionInvariantRepositoryJpaTest
        extends AdministracionPostgreSqlTestBase {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AdministracionInvariantRepository repository;

    private Rol rolAdministrativoUno;
    private Rol rolAdministrativoDos;
    private Rol rolEstudiante;

    private Permiso permisoAdministracion;
    private Permiso permisoRoles;

    @BeforeEach
    void setUp() {
        permisoAdministracion = crearPermiso(
                "ACCEDER_ADMINISTRACION");

        permisoRoles = crearPermiso(
                "ACCEDER_ROLES");

        rolAdministrativoUno = crearRol(
                "Administrador JPA 1",
                TipoPerfilUsuario.ADMINISTRATIVO,
                Set.of(
                        permisoAdministracion,
                        permisoRoles));

        rolAdministrativoDos = crearRol(
                "Administrador JPA 2",
                TipoPerfilUsuario.ADMINISTRATIVO,
                Set.of(permisoAdministracion));

        rolEstudiante = crearRol(
                "Estudiante JPA",
                TipoPerfilUsuario.ESTUDIANTE,
                Set.of(permisoRoles));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void bloqueaYCargaSoloRolesAdministrativosEnOrdenEstable() {
        var roles =
                repository.bloquearYCargarRolesAdministrativos();

        assertEquals(2, roles.size());

        assertEquals(
                rolAdministrativoUno.getId(),
                roles.get(0).rolId());

        assertEquals(
                rolAdministrativoDos.getId(),
                roles.get(1).rolId());

        assertTrue(
                roles.stream()
                        .allMatch(rol ->
                                rol.tipoPerfil()
                                        == TipoPerfilUsuario.ADMINISTRATIVO));

        assertFalse(
                roles.stream()
                        .anyMatch(rol ->
                                rol.rolId()
                                        .equals(rolEstudiante.getId())));
    }

    @Test
    void cargaPermisosAsociadosSoloARolesAdministrativos() {
        var relaciones =
                repository.cargarPermisosDeRolesAdministrativos();

        assertEquals(3, relaciones.size());

        assertTrue(
                relaciones.stream()
                        .anyMatch(relacion ->
                                relacion.rolId()
                                        .equals(rolAdministrativoUno.getId())
                                        && relacion.permisoId()
                                        .equals(permisoAdministracion.getId())));

        assertTrue(
                relaciones.stream()
                        .anyMatch(relacion ->
                                relacion.rolId()
                                        .equals(rolAdministrativoUno.getId())
                                        && relacion.permisoId()
                                        .equals(permisoRoles.getId())));

        assertTrue(
                relaciones.stream()
                        .anyMatch(relacion ->
                                relacion.rolId()
                                        .equals(rolAdministrativoDos.getId())
                                        && relacion.permisoId()
                                        .equals(permisoAdministracion.getId())));

        assertFalse(
                relaciones.stream()
                        .anyMatch(relacion ->
                                relacion.rolId()
                                        .equals(rolEstudiante.getId())));
    }

    @Test
    void cargaCatalogoRealDePermisos() {
        var permisos = repository.cargarPermisos();

        assertTrue(
                permisos.stream()
                        .anyMatch(permiso ->
                                permiso.permisoId()
                                        .equals(permisoAdministracion.getId())
                                        && "ACCEDER_ADMINISTRACION"
                                        .equals(permiso.nombre())
                                        && Boolean.TRUE.equals(
                                        permiso.activo())));

        assertTrue(
                permisos.stream()
                        .anyMatch(permiso ->
                                permiso.permisoId()
                                        .equals(permisoRoles.getId())
                                        && "ACCEDER_ROLES"
                                        .equals(permiso.nombre())
                                        && Boolean.TRUE.equals(
                                        permiso.activo())));
    }

    private Permiso crearPermiso(String nombre) {
        Permiso permiso = new Permiso();

        permiso.setNombre(nombre);
        permiso.setDescripcion(
                "Permiso de prueba JPA");
        permiso.setActivo(true);

        entityManager.persist(permiso);

        return permiso;
    }

    private Rol crearRol(
            String nombre,
            TipoPerfilUsuario tipoPerfil,
            Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre(nombre);
        rol.setDescripcion(
                "Rol de prueba JPA");
        rol.setActivo(true);
        rol.setTipoPerfil(tipoPerfil);
        rol.setPermisos(permisos);

        entityManager.persist(rol);

        return rol;
    }
}