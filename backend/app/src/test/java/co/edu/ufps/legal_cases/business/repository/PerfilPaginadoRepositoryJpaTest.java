package co.edu.ufps.legal_cases.business.repository;

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

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AdministrativoResumenProjection;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.AsesorResumenProjection;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.ConciliadorResumenProjection;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.MonitorResumenProjection;
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
class PerfilPaginadoRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private AdministrativoRepository administrativoRepository;

    @Autowired
    private AsesorRepository asesorRepository;

    @Autowired
    private MonitorRepository monitorRepository;

    @Autowired
    private ConciliadorRepository conciliadorRepository;

    @Autowired
    private EntityManager entityManager;

    private TipoDocumento tipoDocumento;
    private Sede sedePrincipal;
    private Sede sedeAlterna;
    private Area areaCivil;
    private Area areaLaboral;
    private Rol rolSistema;
    private Administrativo adminAlpha;
    private Administrativo adminBeta;
    private Administrativo adminGamma;
    private Administrativo adminDelta;
    private Administrativo adminEpsilon;
    private Administrativo adminZeta;
    private Asesor asesorAlpha;
    private Asesor asesorBeta;
    private Asesor asesorGamma;
    private Asesor asesorDelta;
    private Asesor asesorEpsilon;
    private Asesor asesorZeta;
    private Monitor monitorAlpha;
    private Monitor monitorBeta;
    private Monitor monitorGamma;
    private Monitor monitorDelta;
    private Monitor monitorEpsilon;
    private Monitor monitorZeta;
    private Conciliador conciliadorAlpha;
    private Conciliador conciliadorBeta;
    private Conciliador conciliadorGamma;
    private Conciliador conciliadorDelta;
    private Conciliador conciliadorEpsilon;
    private Conciliador conciliadorZeta;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearAdministrativos();
        crearAsesores();
        crearMonitores();
        crearConciliadores();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void administrativosPaginaUnoRespetaSizeYTotal() {
        Page<AdministrativoResumenProjection> resultado = buscarAdministrativos(
                null, null, PageRequest.of(0, 2, sortIdDesc()));

        assertIdsAdministrativos(resultado, adminZeta.getId(), adminEpsilon.getId());
        assertEquals(2, resultado.getContent().size());
        assertEquals(6, resultado.getTotalElements());
    }

    @Test
    void administrativosPaginaDosDevuelveRegistrosSiguientesYTotalPages() {
        Page<AdministrativoResumenProjection> resultado = buscarAdministrativos(
                null, null, PageRequest.of(1, 2, sortIdDesc()));

        assertIdsAdministrativos(resultado, adminDelta.getId(), adminGamma.getId());
        assertEquals(6, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void administrativosFiltranActivoTrueYFalse() {
        assertIdsAdministrativos(
                buscarAdministrativos(null, true, PageRequest.of(0, 10, sortIdDesc())),
                adminZeta.getId(), adminDelta.getId(), adminBeta.getId(), adminAlpha.getId());
        assertIdsAdministrativos(
                buscarAdministrativos(null, false, PageRequest.of(0, 10, sortIdDesc())),
                adminEpsilon.getId(), adminGamma.getId());
    }

    @Test
    void administrativosBuscanPorNombreDocumentoYCaseInsensitive() {
        assertIdsAdministrativos(buscarAdministrativos("empate", null, PageRequest.of(0, 10, sortIdDesc())),
                adminBeta.getId(), adminAlpha.getId());
        assertIdsAdministrativos(buscarAdministrativos("DOC-ADM-G", null, PageRequest.of(0, 10, sortIdDesc())),
                adminGamma.getId());
        assertIdsAdministrativos(buscarAdministrativos("ADMIN.ZETA", null, PageRequest.of(0, 10, sortIdDesc())),
                adminZeta.getId());
    }

    @Test
    void administrativosCombinanSearchYActivo() {
        Page<AdministrativoResumenProjection> resultado = buscarAdministrativos(
                "admin.beta", true, PageRequest.of(0, 10, sortIdDesc()));

        assertIdsAdministrativos(resultado, adminBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void administrativosOrdenEstableProjectionYNMasUno() {
        Page<AdministrativoResumenProjection> ordenado = buscarAdministrativos(
                null, null, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));

        assertTrue(idsAdministrativos(ordenado).indexOf(adminAlpha.getId())
                < idsAdministrativos(ordenado).indexOf(adminBeta.getId()));

        AdministrativoResumenProjection projection = buscarAdministrativos(
                "admin.alpha", null, PageRequest.of(0, 10, sortIdDesc())).getContent().get(0);
        assertEquals(adminAlpha.getId(), projection.getId());
        assertEquals("Nombre Empate Administrativo", projection.getNombre());
        assertEquals("DOC-ADM-A", projection.getDocumento());
        assertEquals("admin.alpha@example.test", projection.getEmail());
        assertEquals("admin.alpha", projection.getUsuario());
        assertEquals("ADM-A", projection.getCodigo());
        assertEquals(true, projection.getActivo());
        assertEquals(false, projection.getDirectora());
        assertEquals(sedePrincipal.getId(), projection.getSedeId());
        assertEquals("Sede Principal", projection.getSedeNombre());
        assertNotNull(projection.getId());

        assertSinNMasUnoAdministrativo();
    }

    @Test
    void asesoresPaginaUnoPaginaDosYTotales() {
        assertIdsAsesores(
                buscarAsesores(null, null, PageRequest.of(0, 2, sortIdDesc())),
                asesorZeta.getId(), asesorEpsilon.getId());

        Page<AsesorResumenProjection> paginaDos = buscarAsesores(
                null, null, PageRequest.of(1, 2, sortIdDesc()));
        assertIdsAsesores(paginaDos, asesorDelta.getId(), asesorGamma.getId());
        assertEquals(6, paginaDos.getTotalElements());
        assertEquals(3, paginaDos.getTotalPages());
    }

    @Test
    void asesoresFiltranActivoTrueYFalse() {
        assertIdsAsesores(
                buscarAsesores(null, true, PageRequest.of(0, 10, sortIdDesc())),
                asesorZeta.getId(), asesorDelta.getId(), asesorBeta.getId(), asesorAlpha.getId());
        assertIdsAsesores(
                buscarAsesores(null, false, PageRequest.of(0, 10, sortIdDesc())),
                asesorEpsilon.getId(), asesorGamma.getId());
    }

    @Test
    void asesoresBuscanPorNombreDocumentoYCaseInsensitive() {
        assertIdsAsesores(buscarAsesores("empate", null, PageRequest.of(0, 10, sortIdDesc())),
                asesorBeta.getId(), asesorAlpha.getId());
        assertIdsAsesores(buscarAsesores("DOC-ASE-G", null, PageRequest.of(0, 10, sortIdDesc())),
                asesorGamma.getId());
        assertIdsAsesores(buscarAsesores("ASESOR.ZETA", null, PageRequest.of(0, 10, sortIdDesc())),
                asesorZeta.getId());
    }

    @Test
    void asesoresCombinanSearchYActivo() {
        Page<AsesorResumenProjection> resultado = buscarAsesores(
                "asesor.beta", true, PageRequest.of(0, 10, sortIdDesc()));

        assertIdsAsesores(resultado, asesorBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void asesoresOrdenEstableProjectionYNMasUno() {
        Page<AsesorResumenProjection> ordenado = buscarAsesores(
                null, null, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));

        assertTrue(idsAsesores(ordenado).indexOf(asesorAlpha.getId())
                < idsAsesores(ordenado).indexOf(asesorBeta.getId()));

        AsesorResumenProjection projection = buscarAsesores(
                "asesor.alpha", null, PageRequest.of(0, 10, sortIdDesc())).getContent().get(0);
        assertEquals(asesorAlpha.getId(), projection.getId());
        assertEquals("Nombre Empate Asesor", projection.getNombre());
        assertEquals("DOC-ASE-A", projection.getDocumento());
        assertEquals("asesor.alpha@example.test", projection.getEmail());
        assertEquals("asesor.alpha", projection.getUsuario());
        assertEquals("ASE-A", projection.getCodigo());
        assertEquals(true, projection.getActivo());
        assertEquals(areaCivil.getId(), projection.getAreaId());
        assertEquals("Area Civil", projection.getAreaNombre());
        assertEquals(sedePrincipal.getId(), projection.getSedeId());
        assertEquals("Sede Principal", projection.getSedeNombre());

        assertSinNMasUnoAsesor();
    }

    @Test
    void monitoresPaginaUnoPaginaDosYTotales() {
        assertIdsMonitores(
                buscarMonitores(null, null, PageRequest.of(0, 2, sortIdDesc())),
                monitorZeta.getId(), monitorEpsilon.getId());

        Page<MonitorResumenProjection> paginaDos = buscarMonitores(
                null, null, PageRequest.of(1, 2, sortIdDesc()));
        assertIdsMonitores(paginaDos, monitorDelta.getId(), monitorGamma.getId());
        assertEquals(6, paginaDos.getTotalElements());
        assertEquals(3, paginaDos.getTotalPages());
    }

    @Test
    void monitoresFiltranActivoTrueYFalse() {
        assertIdsMonitores(
                buscarMonitores(null, true, PageRequest.of(0, 10, sortIdDesc())),
                monitorZeta.getId(), monitorDelta.getId(), monitorBeta.getId(), monitorAlpha.getId());
        assertIdsMonitores(
                buscarMonitores(null, false, PageRequest.of(0, 10, sortIdDesc())),
                monitorEpsilon.getId(), monitorGamma.getId());
    }

    @Test
    void monitoresBuscanPorNombreDocumentoYCaseInsensitive() {
        assertIdsMonitores(buscarMonitores("empate", null, PageRequest.of(0, 10, sortIdDesc())),
                monitorBeta.getId(), monitorAlpha.getId());
        assertIdsMonitores(buscarMonitores("DOC-MON-G", null, PageRequest.of(0, 10, sortIdDesc())),
                monitorGamma.getId());
        assertIdsMonitores(buscarMonitores("MONITOR.ZETA", null, PageRequest.of(0, 10, sortIdDesc())),
                monitorZeta.getId());
    }

    @Test
    void monitoresCombinanSearchYActivo() {
        Page<MonitorResumenProjection> resultado = buscarMonitores(
                "monitor.beta", true, PageRequest.of(0, 10, sortIdDesc()));

        assertIdsMonitores(resultado, monitorBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void monitoresOrdenEstableProjectionYNMasUno() {
        Page<MonitorResumenProjection> ordenado = buscarMonitores(
                null, null, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));

        assertTrue(idsMonitores(ordenado).indexOf(monitorAlpha.getId())
                < idsMonitores(ordenado).indexOf(monitorBeta.getId()));

        MonitorResumenProjection projection = buscarMonitores(
                "monitor.alpha", null, PageRequest.of(0, 10, sortIdDesc())).getContent().get(0);
        assertEquals(monitorAlpha.getId(), projection.getId());
        assertEquals("Nombre Empate Monitor", projection.getNombre());
        assertEquals("DOC-MON-A", projection.getDocumento());
        assertEquals("monitor.alpha@example.test", projection.getEmail());
        assertEquals("monitor.alpha", projection.getUsuario());
        assertEquals("MON-A", projection.getCodigo());
        assertEquals(true, projection.getActivo());
        assertEquals(sedePrincipal.getId(), projection.getSedeId());
        assertEquals("Sede Principal", projection.getSedeNombre());

        assertSinNMasUnoMonitor();
    }

    @Test
    void conciliadoresPaginaUnoPaginaDosYTotales() {
        assertIdsConciliadores(
                buscarConciliadores(null, null, null, PageRequest.of(0, 2, sortIdDesc())),
                conciliadorZeta.getId(), conciliadorEpsilon.getId());

        Page<ConciliadorResumenProjection> paginaDos = buscarConciliadores(
                null, null, null, PageRequest.of(1, 2, sortIdDesc()));
        assertIdsConciliadores(paginaDos, conciliadorDelta.getId(), conciliadorGamma.getId());
        assertEquals(6, paginaDos.getTotalElements());
        assertEquals(3, paginaDos.getTotalPages());
    }

    @Test
    void conciliadoresFiltranActivoTrueYFalse() {
        assertIdsConciliadores(
                buscarConciliadores(null, true, null, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorZeta.getId(), conciliadorDelta.getId(), conciliadorBeta.getId(), conciliadorAlpha.getId());
        assertIdsConciliadores(
                buscarConciliadores(null, false, null, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorEpsilon.getId(), conciliadorGamma.getId());
    }

    @Test
    void conciliadoresFiltranPorTipoConciliador() {
        assertIdsConciliadores(
                buscarConciliadores(null, null, TipoConciliador.INTERNO, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorZeta.getId(), conciliadorDelta.getId(), conciliadorBeta.getId(), conciliadorAlpha.getId());
        assertIdsConciliadores(
                buscarConciliadores(null, null, TipoConciliador.EXTERNO, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorEpsilon.getId(), conciliadorGamma.getId());
    }

    @Test
    void conciliadoresBuscanPorNombreDocumentoYCaseInsensitive() {
        assertIdsConciliadores(buscarConciliadores("empate", null, null, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorBeta.getId(), conciliadorAlpha.getId());
        assertIdsConciliadores(buscarConciliadores("DOC-CON-G", null, null, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorGamma.getId());
        assertIdsConciliadores(buscarConciliadores("CONCILIADOR.ZETA", null, null, PageRequest.of(0, 10, sortIdDesc())),
                conciliadorZeta.getId());
    }

    @Test
    void conciliadoresCombinanSearchActivoYTipoConciliador() {
        Page<ConciliadorResumenProjection> resultado = buscarConciliadores(
                "conciliador.beta", true, TipoConciliador.INTERNO, PageRequest.of(0, 10, sortIdDesc()));

        assertIdsConciliadores(resultado, conciliadorBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void conciliadoresOrdenEstableProjectionYNMasUno() {
        Page<ConciliadorResumenProjection> ordenado = buscarConciliadores(
                null, null, null, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));

        assertTrue(idsConciliadores(ordenado).indexOf(conciliadorAlpha.getId())
                < idsConciliadores(ordenado).indexOf(conciliadorBeta.getId()));

        ConciliadorResumenProjection projection = buscarConciliadores(
                "conciliador.alpha", null, null, PageRequest.of(0, 10, sortIdDesc())).getContent().get(0);
        assertEquals(conciliadorAlpha.getId(), projection.getId());
        assertEquals("Nombre Empate Conciliador", projection.getNombre());
        assertEquals("DOC-CON-A", projection.getDocumento());
        assertEquals("conciliador.alpha@example.test", projection.getEmail());
        assertEquals("conciliador.alpha", projection.getUsuario());
        assertEquals("CON-A", projection.getCodigo());
        assertEquals(true, projection.getActivo());
        assertEquals(TipoConciliador.INTERNO, projection.getTipoConciliador());
        assertEquals(sedePrincipal.getId(), projection.getSedeId());
        assertEquals("Sede Principal", projection.getSedeNombre());

        assertSinNMasUnoConciliador();
    }

    private void assertSinNMasUnoAdministrativo() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<AdministrativoResumenProjection> resultado = buscarAdministrativos(
                null, null, PageRequest.of(0, 2, sortIdDesc()));
        assertEquals(2, statistics.getPrepareStatementCount());

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getNombre();
            item.getDocumento();
            item.getEmail();
            item.getUsuario();
            item.getCodigo();
            item.getActivo();
            item.getDirectora();
            item.getSedeId();
            item.getSedeNombre();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(0, statistics.getCollectionLoadCount());
        assertEquals(0, statistics.getCollectionFetchCount());
    }

    private void assertSinNMasUnoAsesor() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<AsesorResumenProjection> resultado = buscarAsesores(
                null, null, PageRequest.of(0, 2, sortIdDesc()));
        assertEquals(2, statistics.getPrepareStatementCount());

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getNombre();
            item.getDocumento();
            item.getEmail();
            item.getUsuario();
            item.getCodigo();
            item.getActivo();
            item.getAreaId();
            item.getAreaNombre();
            item.getSedeId();
            item.getSedeNombre();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(0, statistics.getCollectionLoadCount());
        assertEquals(0, statistics.getCollectionFetchCount());
    }

    private void assertSinNMasUnoMonitor() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<MonitorResumenProjection> resultado = buscarMonitores(
                null, null, PageRequest.of(0, 2, sortIdDesc()));
        assertEquals(2, statistics.getPrepareStatementCount());

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getNombre();
            item.getDocumento();
            item.getEmail();
            item.getUsuario();
            item.getCodigo();
            item.getActivo();
            item.getSedeId();
            item.getSedeNombre();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(0, statistics.getCollectionLoadCount());
        assertEquals(0, statistics.getCollectionFetchCount());
    }

    private void assertSinNMasUnoConciliador() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<ConciliadorResumenProjection> resultado = buscarConciliadores(
                null, null, null, PageRequest.of(0, 2, sortIdDesc()));
        assertEquals(2, statistics.getPrepareStatementCount());

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getNombre();
            item.getDocumento();
            item.getEmail();
            item.getUsuario();
            item.getCodigo();
            item.getActivo();
            item.getTipoConciliador();
            item.getSedeId();
            item.getSedeNombre();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(0, statistics.getCollectionLoadCount());
        assertEquals(0, statistics.getCollectionFetchCount());
    }

    private Page<AdministrativoResumenProjection> buscarAdministrativos(
            String search,
            Boolean activo,
            PageRequest pageable) {
        return administrativoRepository.buscarResumenPaginado(search, activo, pageable);
    }

    private Page<AsesorResumenProjection> buscarAsesores(
            String search,
            Boolean activo,
            PageRequest pageable) {
        return asesorRepository.buscarResumenPaginado(search, activo, pageable);
    }

    private Page<MonitorResumenProjection> buscarMonitores(
            String search,
            Boolean activo,
            PageRequest pageable) {
        return monitorRepository.buscarResumenPaginado(search, activo, pageable);
    }

    private Page<ConciliadorResumenProjection> buscarConciliadores(
            String search,
            Boolean activo,
            TipoConciliador tipoConciliador,
            PageRequest pageable) {
        return conciliadorRepository.buscarResumenPaginado(search, activo, tipoConciliador, pageable);
    }

    private Sort sortIdDesc() {
        return Sort.by(Sort.Order.desc("id"));
    }

    private List<Long> idsAdministrativos(Page<AdministrativoResumenProjection> page) {
        return page.getContent().stream()
                .map(AdministrativoResumenProjection::getId)
                .toList();
    }

    private List<Long> idsAsesores(Page<AsesorResumenProjection> page) {
        return page.getContent().stream()
                .map(AsesorResumenProjection::getId)
                .toList();
    }

    private List<Long> idsMonitores(Page<MonitorResumenProjection> page) {
        return page.getContent().stream()
                .map(MonitorResumenProjection::getId)
                .toList();
    }

    private List<Long> idsConciliadores(Page<ConciliadorResumenProjection> page) {
        return page.getContent().stream()
                .map(ConciliadorResumenProjection::getId)
                .toList();
    }

    private void assertIdsAdministrativos(Page<AdministrativoResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), idsAdministrativos(page));
    }

    private void assertIdsAsesores(Page<AsesorResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), idsAsesores(page));
    }

    private void assertIdsMonitores(Page<MonitorResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), idsMonitores(page));
    }

    private void assertIdsConciliadores(Page<ConciliadorResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), idsConciliadores(page));
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    private void crearCatalogos() {
        tipoDocumento = new TipoDocumento();
        tipoDocumento.setNombre("Cedula de ciudadania");
        entityManager.persist(tipoDocumento);

        sedePrincipal = new Sede();
        sedePrincipal.setNombre("Sede Principal");
        entityManager.persist(sedePrincipal);

        sedeAlterna = new Sede();
        sedeAlterna.setNombre("Sede Alterna");
        entityManager.persist(sedeAlterna);

        areaCivil = new Area();
        areaCivil.setNombre("Area Civil");
        entityManager.persist(areaCivil);

        areaLaboral = new Area();
        areaLaboral.setNombre("Area Laboral");
        entityManager.persist(areaLaboral);

        Permiso permiso = new Permiso();
        permiso.setNombre("PERMISO_PERFIL_PAGINADO_TEST");
        permiso.setDescripcion("Permiso de prueba");
        permiso.setActivo(true);
        entityManager.persist(permiso);

        rolSistema = new Rol();
        rolSistema.setNombre("ROL_PERFIL_PAGINADO_TEST");
        rolSistema.setDescripcion("Rol de prueba");
        rolSistema.setActivo(true);
        rolSistema.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        rolSistema.getPermisos().add(permiso);
        entityManager.persist(rolSistema);
    }

    private void crearAdministrativos() {
        adminAlpha = crearAdministrativo("Nombre Empate Administrativo", "DOC-ADM-A",
                "admin.alpha@example.test", "admin.alpha", "ADM-A", true, false, sedePrincipal);
        adminBeta = crearAdministrativo("Nombre Empate Administrativo", "DOC-ADM-B",
                "admin.beta@example.test", "admin.beta", "ADM-B", true, true, sedePrincipal);
        adminGamma = crearAdministrativo("Administrativo Gamma", "DOC-ADM-G",
                "admin.gamma@example.test", "admin.gamma", "ADM-G", false, false, sedeAlterna);
        adminDelta = crearAdministrativo("Administrativo Delta", "DOC-ADM-D",
                "admin.delta@example.test", "admin.delta", "ADM-D", true, false, sedeAlterna);
        adminEpsilon = crearAdministrativo("Administrativo Epsilon", "DOC-ADM-E",
                "admin.epsilon@example.test", "admin.epsilon", "ADM-E", false, false, sedePrincipal);
        adminZeta = crearAdministrativo("Administrativo Zeta", "DOC-ADM-Z",
                "admin.zeta@example.test", "admin.zeta", "ADM-Z", true, false, sedeAlterna);
    }

    private Administrativo crearAdministrativo(
            String nombre,
            String documento,
            String email,
            String usuario,
            String codigo,
            Boolean activo,
            Boolean directora,
            Sede sede) {
        Administrativo administrativo = new Administrativo();
        administrativo.setNombre(nombre);
        administrativo.setTipoDocumento(tipoDocumento);
        administrativo.setDocumento(documento);
        administrativo.setEmail(email);
        administrativo.setTelefono("300-" + documento);
        administrativo.setUsuario(usuario);
        administrativo.setCodigo(codigo);
        administrativo.setSede(sede);
        administrativo.setActivo(activo);
        administrativo.setDirectora(directora);
        administrativo.setUsuarioSistema(crearUsuarioSistema("usuario." + usuario + "@example.test"));
        entityManager.persist(administrativo);
        return administrativo;
    }

    private void crearAsesores() {
        asesorAlpha = crearAsesor("Nombre Empate Asesor", "DOC-ASE-A",
                "asesor.alpha@example.test", "asesor.alpha", "ASE-A", true, sedePrincipal, areaCivil);
        asesorBeta = crearAsesor("Nombre Empate Asesor", "DOC-ASE-B",
                "asesor.beta@example.test", "asesor.beta", "ASE-B", true, sedePrincipal, areaCivil);
        asesorGamma = crearAsesor("Asesor Gamma", "DOC-ASE-G",
                "asesor.gamma@example.test", "asesor.gamma", "ASE-G", false, sedeAlterna, areaLaboral);
        asesorDelta = crearAsesor("Asesor Delta", "DOC-ASE-D",
                "asesor.delta@example.test", "asesor.delta", "ASE-D", true, sedeAlterna, areaLaboral);
        asesorEpsilon = crearAsesor("Asesor Epsilon", "DOC-ASE-E",
                "asesor.epsilon@example.test", "asesor.epsilon", "ASE-E", false, sedePrincipal, areaCivil);
        asesorZeta = crearAsesor("Asesor Zeta", "DOC-ASE-Z",
                "asesor.zeta@example.test", "asesor.zeta", "ASE-Z", true, sedeAlterna, areaLaboral);
    }

    private Asesor crearAsesor(
            String nombre,
            String documento,
            String email,
            String usuario,
            String codigo,
            Boolean activo,
            Sede sede,
            Area area) {
        Asesor asesor = new Asesor();
        asesor.setNombre(nombre);
        asesor.setTipoDocumento(tipoDocumento);
        asesor.setDocumento(documento);
        asesor.setEmail(email);
        asesor.setTelefono("310-" + documento);
        asesor.setUsuario(usuario);
        asesor.setCodigo(codigo);
        asesor.setSede(sede);
        asesor.setArea(area);
        asesor.setActivo(activo);
        asesor.setUsuarioSistema(crearUsuarioSistema("usuario." + usuario + "@example.test"));
        entityManager.persist(asesor);
        return asesor;
    }

    private void crearMonitores() {
        monitorAlpha = crearMonitor("Nombre Empate Monitor", "DOC-MON-A",
                "monitor.alpha@example.test", "monitor.alpha", "MON-A", true, sedePrincipal);
        monitorBeta = crearMonitor("Nombre Empate Monitor", "DOC-MON-B",
                "monitor.beta@example.test", "monitor.beta", "MON-B", true, sedePrincipal);
        monitorGamma = crearMonitor("Monitor Gamma", "DOC-MON-G",
                "monitor.gamma@example.test", "monitor.gamma", "MON-G", false, sedeAlterna);
        monitorDelta = crearMonitor("Monitor Delta", "DOC-MON-D",
                "monitor.delta@example.test", "monitor.delta", "MON-D", true, sedeAlterna);
        monitorEpsilon = crearMonitor("Monitor Epsilon", "DOC-MON-E",
                "monitor.epsilon@example.test", "monitor.epsilon", "MON-E", false, sedePrincipal);
        monitorZeta = crearMonitor("Monitor Zeta", "DOC-MON-Z",
                "monitor.zeta@example.test", "monitor.zeta", "MON-Z", true, sedeAlterna);
    }

    private Monitor crearMonitor(
            String nombre,
            String documento,
            String email,
            String usuario,
            String codigo,
            Boolean activo,
            Sede sede) {
        Monitor monitor = new Monitor();
        monitor.setNombre(nombre);
        monitor.setTipoDocumento(tipoDocumento);
        monitor.setDocumento(documento);
        monitor.setEmail(email);
        monitor.setTelefono("320-" + documento);
        monitor.setUsuario(usuario);
        monitor.setCodigo(codigo);
        monitor.setSede(sede);
        monitor.setActivo(activo);
        monitor.setUsuarioSistema(crearUsuarioSistema("usuario." + usuario + "@example.test"));
        entityManager.persist(monitor);
        return monitor;
    }

    private void crearConciliadores() {
        conciliadorAlpha = crearConciliador("Nombre Empate Conciliador", "DOC-CON-A",
                "conciliador.alpha@example.test", "conciliador.alpha", "CON-A", true, TipoConciliador.INTERNO, sedePrincipal);
        conciliadorBeta = crearConciliador("Nombre Empate Conciliador", "DOC-CON-B",
                "conciliador.beta@example.test", "conciliador.beta", "CON-B", true, TipoConciliador.INTERNO, sedePrincipal);
        conciliadorGamma = crearConciliador("Conciliador Gamma", "DOC-CON-G",
                "conciliador.gamma@example.test", "conciliador.gamma", "CON-G", false, TipoConciliador.EXTERNO, sedeAlterna);
        conciliadorDelta = crearConciliador("Conciliador Delta", "DOC-CON-D",
                "conciliador.delta@example.test", "conciliador.delta", "CON-D", true, TipoConciliador.INTERNO, sedeAlterna);
        conciliadorEpsilon = crearConciliador("Conciliador Epsilon", "DOC-CON-E",
                "conciliador.epsilon@example.test", "conciliador.epsilon", "CON-E", false, TipoConciliador.EXTERNO, sedePrincipal);
        conciliadorZeta = crearConciliador("Conciliador Zeta", "DOC-CON-Z",
                "conciliador.zeta@example.test", "conciliador.zeta", "CON-Z", true, TipoConciliador.INTERNO, sedeAlterna);
    }

    private Conciliador crearConciliador(
            String nombre,
            String documento,
            String email,
            String usuario,
            String codigo,
            Boolean activo,
            TipoConciliador tipoConciliador,
            Sede sede) {
        Conciliador conciliador = new Conciliador();
        conciliador.setNombre(nombre);
        conciliador.setTipoDocumento(tipoDocumento);
        conciliador.setDocumento(documento);
        conciliador.setEmail(email);
        conciliador.setTelefono("330-" + documento);
        conciliador.setUsuario(usuario);
        conciliador.setCodigo(codigo);
        conciliador.setTipoConciliador(tipoConciliador);
        conciliador.setSede(sede);
        conciliador.setActivo(activo);
        conciliador.setUsuarioSistema(crearUsuarioSistema("usuario." + usuario + "@example.test"));
        entityManager.persist(conciliador);
        return conciliador;
    }

    private UsuarioSistema crearUsuarioSistema(String username) {
        UsuarioSistema usuarioSistema = new UsuarioSistema();
        usuarioSistema.setUsername(username);
        usuarioSistema.setPasswordHash("hash-" + username);
        usuarioSistema.setActivo(true);
        usuarioSistema.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        usuarioSistema.setRol(rolSistema);
        entityManager.persist(usuarioSistema);
        return usuarioSistema;
    }
}
