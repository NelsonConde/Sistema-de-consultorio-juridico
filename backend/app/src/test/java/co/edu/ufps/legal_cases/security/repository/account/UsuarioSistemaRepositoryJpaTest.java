package co.edu.ufps.legal_cases.security.repository.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=\"DB_consultorioJuridico\"",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioSistemaRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private UsuarioSistemaRepository usuarioSistemaRepository;

    @Autowired
    private EntityManager entityManager;

    private Rol rolAdministrativo;
    private Rol rolAsesor;
    private Rol rolMonitor;
    private Rol rolEstudiante;
    private UsuarioSistema usuarioAdminAlpha;
    private UsuarioSistema usuarioAsesorAlpha;
    private UsuarioSistema usuarioMonitorAlpha;
    private UsuarioSistema usuarioEstudianteAlpha;
    private UsuarioSistema usuarioAsesorBeta;
    private UsuarioSistema usuarioMonitorBeta;

    @BeforeEach
    void setUp() {
        crearRoles();
        crearUsuarios();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void paginaUnoDebeRespetarSize() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, null,
                PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(6, resultado.getTotalElements());
    }

    @Test
    void paginaDosDebeDevolverRegistrosSiguientes() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, null,
                PageRequest.of(1, 2, sortIdDesc()));

        assertIds(resultado, usuarioEstudianteAlpha.getId(), usuarioMonitorAlpha.getId());
        assertEquals(6, resultado.getTotalElements());
    }

    @Test
    void totalElementsDebeSerCorrecto() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, null,
                PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(6, resultado.getTotalElements());
        assertEquals(6, resultado.getTotalPages());
    }

    @Test
    void filtroActivoTrueDebeAplicarseEnContentYCount() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, true, null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, usuarioAsesorBeta.getId(), usuarioMonitorAlpha.getId(),
                usuarioAsesorAlpha.getId(), usuarioAdminAlpha.getId());
        assertEquals(4, resultado.getTotalElements());
    }

    @Test
    void filtroActivoFalseDebeAplicarseEnContentYCount() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, false, null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, usuarioMonitorBeta.getId(), usuarioEstudianteAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtroTipoPerfilDebeAplicarseEnContentYCount() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, TipoPerfilUsuario.ASESOR,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, usuarioAsesorBeta.getId(), usuarioAsesorAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void busquedaPorUsernameDebeSerCaseInsensitive() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar("ASESOR.ALPHA", null, null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, usuarioAsesorAlpha.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void busquedaPorRolNombreDebeSerCaseInsensitive() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar("rol asesor", null, null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, usuarioAsesorBeta.getId(), usuarioAsesorAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void busquedaActivoYTipoPerfilDebenCombinarseConAnd() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(
                "beta", true, TipoPerfilUsuario.ASESOR, PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, usuarioAsesorBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void ordenEstableDebeUsarIdAscComoDesempate() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("rol.nombre"), Sort.Order.asc("id"))));

        assertTrue(ids(resultado).indexOf(usuarioAsesorAlpha.getId())
                < ids(resultado).indexOf(usuarioAsesorBeta.getId()));
        assertTrue(ids(resultado).indexOf(usuarioMonitorAlpha.getId())
                < ids(resultado).indexOf(usuarioMonitorBeta.getId()));
    }

    @Test
    void projectionContieneCamposEsperados() {
        Page<UsuarioSistemaResumenProjection> resultado = buscar("admin.alpha", null, null,
                PageRequest.of(0, 10, sortIdDesc()));

        UsuarioSistemaResumenProjection projection = resultado.getContent().get(0);

        assertEquals(usuarioAdminAlpha.getId(), projection.getId());
        assertEquals("admin.alpha@example.test", projection.getUsername());
        assertEquals(true, projection.getActivo());
        assertEquals(rolAdministrativo.getId(), projection.getRolId());
        assertEquals("Rol Administrativo", projection.getRolNombre());
        assertEquals(TipoPerfilUsuario.ADMINISTRATIVO, projection.getTipoPerfil());
        assertNotNull(projection.getId());
    }

    @Test
    void consultaPaginadaNoDebeCargarPermisosDelRol() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, null,
                PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(0, statistics.getCollectionLoadCount());
        assertEquals(0, statistics.getCollectionFetchCount());
    }

    @Test
    void accesoAGettersDeProjectionNoDebeGenerarNMasUno() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<UsuarioSistemaResumenProjection> resultado = buscar(null, null, null,
                PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, statistics.getPrepareStatementCount());

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getUsername();
            item.getActivo();
            item.getRolId();
            item.getRolNombre();
            item.getTipoPerfil();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    private Page<UsuarioSistemaResumenProjection> buscar(
            String search,
            Boolean activo,
            TipoPerfilUsuario tipoPerfil,
            PageRequest pageable) {
        return usuarioSistemaRepository.buscarResumenPaginado(
                search,
                activo,
                tipoPerfil,
                pageable);
    }

    private Sort sortIdDesc() {
        return Sort.by(Sort.Order.desc("id"));
    }

    private List<Long> ids(Page<UsuarioSistemaResumenProjection> page) {
        return page.getContent().stream()
                .map(UsuarioSistemaResumenProjection::getId)
                .toList();
    }

    private void assertIds(Page<UsuarioSistemaResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), ids(page));
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    private void crearRoles() {
        Permiso permisoVer = crearPermiso("VER_USUARIOS_TEST");
        Permiso permisoGestionar = crearPermiso("GESTIONAR_USUARIOS_TEST");

        rolAdministrativo = crearRol("Rol Administrativo", TipoPerfilUsuario.ADMINISTRATIVO,
                permisoVer, permisoGestionar);
        rolAsesor = crearRol("Rol Asesor", TipoPerfilUsuario.ASESOR, permisoVer);
        rolMonitor = crearRol("Rol Monitor", TipoPerfilUsuario.MONITOR, permisoGestionar);
        rolEstudiante = crearRol("Rol Estudiante", TipoPerfilUsuario.ESTUDIANTE, permisoVer);
    }

    private Permiso crearPermiso(String nombre) {
        Permiso permiso = new Permiso();
        permiso.setNombre(nombre);
        permiso.setDescripcion(nombre);
        permiso.setActivo(true);
        entityManager.persist(permiso);
        return permiso;
    }

    private Rol crearRol(String nombre, TipoPerfilUsuario tipoPerfil, Permiso... permisos) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(nombre);
        rol.setActivo(true);
        rol.setTipoPerfil(tipoPerfil);
        rol.getPermisos().addAll(List.of(permisos));
        entityManager.persist(rol);
        return rol;
    }

    private void crearUsuarios() {
        usuarioAdminAlpha = crearUsuario(
                "admin.alpha@example.test", true, TipoPerfilUsuario.ADMINISTRATIVO, rolAdministrativo);
        usuarioAsesorAlpha = crearUsuario(
                "asesor.alpha@example.test", true, TipoPerfilUsuario.ASESOR, rolAsesor);
        usuarioMonitorAlpha = crearUsuario(
                "monitor.alpha@example.test", true, TipoPerfilUsuario.MONITOR, rolMonitor);
        usuarioEstudianteAlpha = crearUsuario(
                "estudiante.alpha@example.test", false, TipoPerfilUsuario.ESTUDIANTE, rolEstudiante);
        usuarioAsesorBeta = crearUsuario(
                "asesor.beta@example.test", true, TipoPerfilUsuario.ASESOR, rolAsesor);
        usuarioMonitorBeta = crearUsuario(
                "monitor.beta@example.test", false, TipoPerfilUsuario.MONITOR, rolMonitor);
    }

    private UsuarioSistema crearUsuario(
            String username,
            Boolean activo,
            TipoPerfilUsuario tipoPerfil,
            Rol rol) {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername(username);
        usuario.setPasswordHash("hash-" + username);
        usuario.setActivo(activo);
        usuario.setTipoPerfilActual(tipoPerfil);
        usuario.setRol(rol);
        entityManager.persist(usuario);
        return usuario;
    }
}
