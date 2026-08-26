package co.edu.ufps.legal_cases.security.repository.invariant;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ACCEDER_ADMINISTRACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ACCEDER_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ADMINISTRADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERMISOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.common.exception.AdministracionInvariantException;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionCapabilityEvaluator;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionInvariantPolicy;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionSnapshotFactory;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.AdministracionSnapshotProjector;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.model.AdministracionInvariantSnapshot;
import jakarta.persistence.EntityManager;

@SpringBootTest
class AdministracionInvariantConcurrencyTest
        extends AdministracionPostgreSqlTestBase {

    private static final Long ACTOR_EXTERNO = -999L;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AdministracionSnapshotFactory snapshotFactory;

    @Autowired
    private AdministracionSnapshotProjector snapshotProjector;

    @Autowired
    private AdministracionInvariantPolicy invariantPolicy;

    @Autowired
    private AdministracionCapabilityEvaluator capabilityEvaluator;

    private ExecutorService executor;

    private Long usuarioUnoId;
    private Long usuarioDosId;

    private Long administrativoUnoId;
    private Long administrativoDosId;

    private Long rolUnoId;
    private Long rolDosId;

    private Long permisoGestionarPermisosId;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);

        ejecutarEnTransaccion(() -> {
            limpiarDatos();
            crearEscenarioBase();
        });
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void dosDesactivacionesConcurrentesNoDejanCeroAdministradores()
            throws Exception {

        CountDownLatch primerLock =
                new CountDownLatch(1);

        CountDownLatch liberarPrimero =
                new CountDownLatch(1);

        CountDownLatch segundoIniciado =
                new CountDownLatch(1);

        Future<Resultado> primero =
                executor.submit(() ->
                        ejecutarMutacion(
                                snapshot ->
                                        snapshotProjector
                                                .conUsuarioActivo(
                                                        snapshot,
                                                        usuarioUnoId,
                                                        false),
                                () -> entityManager.createQuery("""
                                                UPDATE UsuarioSistema u
                                                SET u.activo = false
                                                WHERE u.id = :id
                                                """)
                                        .setParameter(
                                                "id",
                                                usuarioUnoId)
                                        .executeUpdate(),
                                primerLock,
                                liberarPrimero));

        esperar(primerLock);

        Future<Resultado> segundo =
                executor.submit(() -> {
                    segundoIniciado.countDown();

                    return ejecutarMutacion(
                            snapshot ->
                                    snapshotProjector
                                            .conUsuarioActivo(
                                                    snapshot,
                                                    usuarioDosId,
                                                    false),
                            () -> entityManager.createQuery("""
                                            UPDATE UsuarioSistema u
                                            SET u.activo = false
                                            WHERE u.id = :id
                                            """)
                                    .setParameter(
                                            "id",
                                            usuarioDosId)
                                    .executeUpdate(),
                            null,
                            null);
                });

        esperar(segundoIniciado);

        assertThrows(
                TimeoutException.class,
                () -> segundo.get(
                        300,
                        TimeUnit.MILLISECONDS));

        liberarPrimero.countDown();

        assertResultados(
                primero.get(10, TimeUnit.SECONDS),
                segundo.get(10, TimeUnit.SECONDS));

        AdministracionInvariantSnapshot finalSnapshot =
                cargarSnapshotFinal();

        long administradoresOperativos =
                finalSnapshot.administrativos()
                        .values()
                        .stream()
                        .filter(administrativo ->
                                capabilityEvaluator
                                        .esAdministradorOperativo(
                                                finalSnapshot,
                                                administrativo))
                        .count();

        assertEquals(1, administradoresOperativos);
    }

    @Test
    void dosRetirosConcurrentesNoDejanCeroDirectoras()
            throws Exception {

        CountDownLatch primerLock =
                new CountDownLatch(1);

        CountDownLatch liberarPrimero =
                new CountDownLatch(1);

        CountDownLatch segundoIniciado =
                new CountDownLatch(1);

        Future<Resultado> primero =
                executor.submit(() ->
                        ejecutarMutacion(
                                snapshot ->
                                        snapshotProjector
                                                .conDirectora(
                                                        snapshot,
                                                        administrativoUnoId,
                                                        false),
                                () -> entityManager.createQuery("""
                                                UPDATE Administrativo a
                                                SET a.directora = false
                                                WHERE a.id = :id
                                                """)
                                        .setParameter(
                                                "id",
                                                administrativoUnoId)
                                        .executeUpdate(),
                                primerLock,
                                liberarPrimero));

        esperar(primerLock);

        Future<Resultado> segundo =
                executor.submit(() -> {
                    segundoIniciado.countDown();

                    return ejecutarMutacion(
                            snapshot ->
                                    snapshotProjector
                                            .conDirectora(
                                                    snapshot,
                                                    administrativoDosId,
                                                    false),
                            () -> entityManager.createQuery("""
                                            UPDATE Administrativo a
                                            SET a.directora = false
                                            WHERE a.id = :id
                                            """)
                                    .setParameter(
                                            "id",
                                            administrativoDosId)
                                    .executeUpdate(),
                            null,
                            null);
                });

        esperar(segundoIniciado);

        assertThrows(
                TimeoutException.class,
                () -> segundo.get(
                        300,
                        TimeUnit.MILLISECONDS));

        liberarPrimero.countDown();

        assertResultados(
                primero.get(10, TimeUnit.SECONDS),
                segundo.get(10, TimeUnit.SECONDS));

        AdministracionInvariantSnapshot finalSnapshot =
                cargarSnapshotFinal();

        long directoras =
                finalSnapshot.administrativos()
                        .values()
                        .stream()
                        .filter(administrativo ->
                                capabilityEvaluator
                                        .esDirectoraOperativa(
                                                finalSnapshot,
                                                administrativo))
                        .count();

        assertEquals(1, directoras);
    }

    @Test
    void dosRetirosConcurrentesNoEliminanUltimaCapacidadDeRecuperacion()
            throws Exception {

        CountDownLatch primerLock =
                new CountDownLatch(1);

        CountDownLatch liberarPrimero =
                new CountDownLatch(1);

        CountDownLatch segundoIniciado =
                new CountDownLatch(1);

        Future<Resultado> primero =
                executor.submit(() ->
                        ejecutarMutacion(
                                snapshot ->
                                        snapshotProjector
                                                .sinPermisoRol(
                                                        snapshot,
                                                        rolUnoId,
                                                        permisoGestionarPermisosId),
                                () -> eliminarPermisoDeRol(
                                        rolUnoId,
                                        permisoGestionarPermisosId),
                                primerLock,
                                liberarPrimero));

        esperar(primerLock);

        Future<Resultado> segundo =
                executor.submit(() -> {
                    segundoIniciado.countDown();

                    return ejecutarMutacion(
                            snapshot ->
                                    snapshotProjector
                                            .sinPermisoRol(
                                                    snapshot,
                                                    rolDosId,
                                                    permisoGestionarPermisosId),
                            () -> eliminarPermisoDeRol(
                                    rolDosId,
                                    permisoGestionarPermisosId),
                            null,
                            null);
                });

        esperar(segundoIniciado);

        assertThrows(
                TimeoutException.class,
                () -> segundo.get(
                        300,
                        TimeUnit.MILLISECONDS));

        liberarPrimero.countDown();

        assertResultados(
                primero.get(10, TimeUnit.SECONDS),
                segundo.get(10, TimeUnit.SECONDS));

        AdministracionInvariantSnapshot finalSnapshot =
                cargarSnapshotFinal();

        long administradoresConRecuperacion =
                finalSnapshot.administrativos()
                        .values()
                        .stream()
                        .filter(administrativo ->
                                capabilityEvaluator
                                        .tieneCapacidadRecuperacion(
                                                finalSnapshot,
                                                administrativo))
                        .count();

        assertEquals(
                1,
                administradoresConRecuperacion);
    }

    private Resultado ejecutarMutacion(
            UnaryOperator<AdministracionInvariantSnapshot> transicion,
            Runnable mutacion,
            CountDownLatch lockAdquirido,
            CountDownLatch liberar) {

        try {
            nuevaTransaccion()
                    .executeWithoutResult(status -> {
                        AdministracionInvariantSnapshot actual =
                                snapshotFactory
                                        .cargarBajoBloqueo();

                        if (lockAdquirido != null) {
                            lockAdquirido.countDown();
                        }

                        if (liberar != null) {
                            esperar(liberar);
                        }

                        AdministracionInvariantSnapshot propuesto =
                                transicion.apply(actual);

                        invariantPolicy.validarTransicion(
                                actual,
                                propuesto,
                                ACTOR_EXTERNO);

                        mutacion.run();

                        entityManager.flush();
                    });

            return Resultado.APLICADA;

        } catch (AdministracionInvariantException ex) {
            return Resultado.RECHAZADA;
        }
    }

    private AdministracionInvariantSnapshot cargarSnapshotFinal() {
        return nuevaTransaccion()
                .execute(status ->
                        snapshotFactory
                                .cargarBajoBloqueo());
    }

    private void crearEscenarioBase() {
        Permiso accederAdministracion =
                crearPermiso(
                        ACCEDER_ADMINISTRACION);

        Permiso accederRoles =
                crearPermiso(
                        ACCEDER_ROLES);

        Permiso gestionarUsuarios =
                crearPermiso(
                        GESTIONAR_USUARIOS);

        Permiso gestionarRoles =
                crearPermiso(
                        GESTIONAR_ROLES);

        Permiso gestionarPermisos =
                crearPermiso(
                        GESTIONAR_PERMISOS);

        Permiso gestionarAdministradores =
                crearPermiso(
                        GESTIONAR_ADMINISTRADORES);

        entityManager.flush();

        permisoGestionarPermisosId =
                gestionarPermisos.getId();

        Set<Permiso> permisos =
                Set.of(
                        accederAdministracion,
                        accederRoles,
                        gestionarUsuarios,
                        gestionarRoles,
                        gestionarPermisos,
                        gestionarAdministradores);

        Rol rolUno =
                crearRol(
                        "Administrador concurrencia 1",
                        permisos);

        Rol rolDos =
                crearRol(
                        "Administrador concurrencia 2",
                        permisos);

        entityManager.flush();

        rolUnoId = rolUno.getId();
        rolDosId = rolDos.getId();

        UsuarioSistema usuarioUno =
                crearUsuario(
                        "admin-concurrencia-1@test.local",
                        rolUno);

        UsuarioSistema usuarioDos =
                crearUsuario(
                        "admin-concurrencia-2@test.local",
                        rolDos);

        entityManager.flush();

        usuarioUnoId = usuarioUno.getId();
        usuarioDosId = usuarioDos.getId();

        TipoDocumento tipoDocumento =
                new TipoDocumento();

        tipoDocumento.setNombre("CC TEST");
        tipoDocumento.setActivo(true);

        entityManager.persist(tipoDocumento);

        Sede sede = new Sede();

        sede.setNombre("SEDE TEST");
        sede.setActivo(true);

        entityManager.persist(sede);

        entityManager.flush();

        Administrativo administrativoUno =
                crearAdministrativo(
                        "Administrativo concurrencia 1",
                        "90000001",
                        "admin1@test.local",
                        "3000000001",
                        "admin_test_1",
                        "ADM-TEST-1",
                        usuarioUno,
                        tipoDocumento,
                        sede);

        Administrativo administrativoDos =
                crearAdministrativo(
                        "Administrativo concurrencia 2",
                        "90000002",
                        "admin2@test.local",
                        "3000000002",
                        "admin_test_2",
                        "ADM-TEST-2",
                        usuarioDos,
                        tipoDocumento,
                        sede);

        entityManager.flush();

        administrativoUnoId =
                administrativoUno.getId();

        administrativoDosId =
                administrativoDos.getId();
    }

    private Permiso crearPermiso(String nombre) {
        Permiso permiso = new Permiso();

        permiso.setNombre(nombre);
        permiso.setDescripcion(
                "Permiso SEC-08");
        permiso.setActivo(true);

        entityManager.persist(permiso);

        return permiso;
    }

    private Rol crearRol(
            String nombre,
            Set<Permiso> permisos) {

        Rol rol = new Rol();

        rol.setNombre(nombre);
        rol.setDescripcion(
                "Rol SEC-08");
        rol.setActivo(true);
        rol.setTipoPerfil(
                TipoPerfilUsuario.ADMINISTRATIVO);
        rol.setPermisos(permisos);

        entityManager.persist(rol);

        return rol;
    }

    private UsuarioSistema crearUsuario(
            String username,
            Rol rol) {

        UsuarioSistema usuario =
                new UsuarioSistema();

        usuario.setUsername(username);
        usuario.setPasswordHash("test-hash");
        usuario.setActivo(true);
        usuario.setTipoPerfilActual(
                TipoPerfilUsuario.ADMINISTRATIVO);
        usuario.setRol(rol);

        entityManager.persist(usuario);

        return usuario;
    }

    private Administrativo crearAdministrativo(
            String nombre,
            String documento,
            String email,
            String telefono,
            String usuario,
            String codigo,
            UsuarioSistema usuarioSistema,
            TipoDocumento tipoDocumento,
            Sede sede) {

        Administrativo administrativo =
                new Administrativo();

        administrativo.setNombre(nombre);
        administrativo.setDocumento(documento);
        administrativo.setEmail(email);
        administrativo.setTelefono(telefono);
        administrativo.setUsuario(usuario);
        administrativo.setCodigo(codigo);
        administrativo.setUsuarioSistema(
                usuarioSistema);
        administrativo.setTipoDocumento(
                tipoDocumento);
        administrativo.setSede(sede);
        administrativo.setActivo(true);
        administrativo.setDirectora(true);

        entityManager.persist(administrativo);

        return administrativo;
    }

    private void eliminarPermisoDeRol(
            Long rolId,
            Long permisoId) {

        entityManager.createNativeQuery("""
                        DELETE
                        FROM "DB_consultorioJuridico".rol_permiso
                        WHERE rol_id = :rolId
                          AND permiso_id = :permisoId
                        """)
                .setParameter("rolId", rolId)
                .setParameter("permisoId", permisoId)
                .executeUpdate();
    }

    private void limpiarDatos() {
        entityManager.createNativeQuery("""
                        TRUNCATE TABLE
                            "DB_consultorioJuridico".administrativo,
                            "DB_consultorioJuridico".usuario_sistema,
                            "DB_consultorioJuridico".rol,
                            "DB_consultorioJuridico".permiso,
                            "DB_consultorioJuridico".tipodoc,
                            "DB_consultorioJuridico".sede
                        RESTART IDENTITY CASCADE
                        """)
                .executeUpdate();
    }

    private void ejecutarEnTransaccion(
            Runnable accion) {

        nuevaTransaccion()
                .executeWithoutResult(status ->
                        accion.run());
    }

    private TransactionTemplate nuevaTransaccion() {
        TransactionTemplate template =
                new TransactionTemplate(
                        transactionManager);

        template.setTimeout(15);

        return template;
    }

    private void esperar(
            CountDownLatch latch) {

        try {
            boolean completado =
                    latch.await(
                            10,
                            TimeUnit.SECONDS);

            if (!completado) {
                throw new IllegalStateException(
                        "Timeout esperando sincronización del test");
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "El test concurrente fue interrumpido",
                    ex);
        }
    }

    private void assertResultados(
            Resultado primero,
            Resultado segundo) {

        assertEquals(
                Set.of(
                        Resultado.APLICADA,
                        Resultado.RECHAZADA),
                Set.of(
                        primero,
                        segundo));
    }

    private enum Resultado {
        APLICADA,
        RECHAZADA
    }
}