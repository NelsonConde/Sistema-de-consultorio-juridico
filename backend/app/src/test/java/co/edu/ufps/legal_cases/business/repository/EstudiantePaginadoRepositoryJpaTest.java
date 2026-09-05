package co.edu.ufps.legal_cases.business.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteRepository;
import co.edu.ufps.legal_cases.business.repository.perfil.EstudianteResumenProjection;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=\"DB_consultorioJuridico\"",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EstudiantePaginadoRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private EntityManager entityManager;

    private TipoDocumento tipoDocumento;
    private Sede sedePrincipal;
    private Area areaCivil;
    private Asesor asesorA;
    private Asesor asesorB;
    private Estudiante estudianteA1;
    private Estudiante estudianteA2;
    private Estudiante estudianteA3;
    private Estudiante estudianteA4;
    private Estudiante estudianteB1;
    private Estudiante estudianteB2;
    private Estudiante estudianteB3;
    private Estudiante estudianteB4;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        asesorA = crearAsesor("Asesor A", "asesor.a");
        asesorB = crearAsesor("Asesor B", "asesor.b");

        estudianteA1 = crearEstudiante(
                "Nombre Empate Estudiante", "DOC-A1", "juan.a1@example.test", "juan.a1", "EST-A1",
                true, true, asesorA);
        estudianteA2 = crearEstudiante(
                "Nombre Empate Estudiante", "DOC-A2", "maria.a2@example.test", "maria.a2", "EST-A2",
                true, false, asesorA);
        estudianteA3 = crearEstudiante(
                "Estudiante Luis A", "DOC-A3", "luis.a3@example.test", "luis.a3", "EST-A3",
                false, true, asesorA);
        estudianteA4 = crearEstudiante(
                "Estudiante Carlos A", "DOC-A4", "carlos.a4@example.test", "carlos.a4", "EST-A4",
                true, false, asesorA);

        estudianteB1 = crearEstudiante(
                "Estudiante Ana B", "DOC-B1", "ana.b1@example.test", "ana.b1", "EST-B1",
                true, true, asesorB);
        estudianteB2 = crearEstudiante(
                "Estudiante Pedro B", "DOC-B2", "pedro.b2@example.test", "pedro.b2", "EST-B2",
                false, false, asesorB);
        estudianteB3 = crearEstudiante(
                "Estudiante Luisa B", "DOC-B3", "luisa.b3@example.test", "luisa.b3", "EST-B3",
                true, true, asesorB);
        estudianteB4 = crearEstudiante(
                "Estudiante Mateo B", "DOC-B4", "mateo.b4@example.test", "mateo.b4", "EST-B4",
                true, false, asesorB);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void scopeGlobalPaginaUnoPaginaDosYTotales() {
        Page<EstudianteResumenProjection> paginaUno = buscar(
                null, null, null, PageRequest.of(0, 3, sortIdDesc()));
        assertIds(paginaUno, estudianteB4.getId(), estudianteB3.getId(), estudianteB2.getId());
        assertEquals(8, paginaUno.getTotalElements());
        assertEquals(3, paginaUno.getTotalPages());

        Page<EstudianteResumenProjection> paginaDos = buscar(
                null, null, null, PageRequest.of(1, 3, sortIdDesc()));
        assertIds(paginaDos, estudianteB1.getId(), estudianteA4.getId(), estudianteA3.getId());
        assertEquals(8, paginaDos.getTotalElements());
        assertEquals(3, paginaDos.getTotalPages());
    }

    @Test
    void filtroActivoTrueYFalseAfectaContenidoYCount() {
        Page<EstudianteResumenProjection> activos = buscar(
                null, true, null, PageRequest.of(0, 2, sortIdDesc()));
        assertIds(activos, estudianteB4.getId(), estudianteB3.getId());
        assertEquals(6, activos.getTotalElements());
        assertEquals(3, activos.getTotalPages());

        Page<EstudianteResumenProjection> inactivos = buscar(
                null, false, null, PageRequest.of(0, 1, sortIdDesc()));
        assertIds(inactivos, estudianteB2.getId());
        assertEquals(2, inactivos.getTotalElements());
        assertEquals(2, inactivos.getTotalPages());
    }

    @Test
    void searchDebeCubrirNombreDocumentoYSerCaseInsensitive() {
        Page<EstudianteResumenProjection> porNombre = buscar(
                "nOmBrE eMpAtE", null, null, PageRequest.of(0, 10, sortIdDesc()));
        assertIds(porNombre, estudianteA2.getId(), estudianteA1.getId());
        assertEquals(2, porNombre.getTotalElements());

        Page<EstudianteResumenProjection> porDocumento = buscar(
                "doc-b", null, null, PageRequest.of(0, 10, sortIdDesc()));
        assertIds(
                porDocumento,
                estudianteB4.getId(), estudianteB3.getId(), estudianteB2.getId(), estudianteB1.getId());
        assertEquals(4, porDocumento.getTotalElements());
    }

    @Test
    void searchYActivoDebenCombinarseConAnd() {
        Page<EstudianteResumenProjection> resultado = buscar(
                "example.test", true, null, PageRequest.of(0, 2, sortIdDesc()));

        assertIds(resultado, estudianteB4.getId(), estudianteB3.getId());
        assertEquals(6, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void scopeAsesorAFiltraAntesDePaginarYCalculaTotalesDelScope() {
        Page<EstudianteResumenProjection> paginaUno = buscar(
                null, null, asesorA.getId(), PageRequest.of(0, 2, sortIdDesc()));
        assertIds(paginaUno, estudianteA4.getId(), estudianteA3.getId());
        assertEquals(4, paginaUno.getTotalElements());
        assertEquals(2, paginaUno.getTotalPages());
        assertSoloAsesor(paginaUno, asesorA.getId());

        Page<EstudianteResumenProjection> paginaDos = buscar(
                null, null, asesorA.getId(), PageRequest.of(1, 2, sortIdDesc()));
        assertIds(paginaDos, estudianteA2.getId(), estudianteA1.getId());
        assertEquals(4, paginaDos.getTotalElements());
        assertEquals(2, paginaDos.getTotalPages());
        assertSoloAsesor(paginaDos, asesorA.getId());
    }

    @Test
    void scopeAsesorBDebeExcluirCompletamenteEstudiantesDelAsesorA() {
        Page<EstudianteResumenProjection> resultado = buscar(
                null, null, asesorB.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(
                resultado,
                estudianteB4.getId(), estudianteB3.getId(), estudianteB2.getId(), estudianteB1.getId());
        assertEquals(4, resultado.getTotalElements());
        assertEquals(1, resultado.getTotalPages());
        assertSoloAsesor(resultado, asesorB.getId());
    }

    @Test
    void ordenEstableDebeUsarIdAscComoDesempate() {
        Page<EstudianteResumenProjection> resultado = buscar(
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));

        assertTrue(ids(resultado).indexOf(estudianteA1.getId())
                < ids(resultado).indexOf(estudianteA2.getId()));
    }

    @Test
    void projectionDebeContenerExactamenteLosCamposEscalaresEsperados() {
        EstudianteResumenProjection projection = buscar(
                "juan.a1", null, null, PageRequest.of(0, 10, sortIdDesc()))
                .getContent()
                .get(0);

        assertEquals(estudianteA1.getId(), projection.getId());
        assertEquals("Nombre Empate Estudiante", projection.getNombre());
        assertEquals("DOC-A1", projection.getDocumento());
        assertEquals("juan.a1@example.test", projection.getEmail());
        assertEquals("juan.a1", projection.getUsuario());
        assertEquals("EST-A1", projection.getCodigo());
        assertEquals(true, projection.getActivo());
        assertEquals(sedePrincipal.getId(), projection.getSedeId());
        assertEquals("Sede Principal", projection.getSedeNombre());
        assertEquals(asesorA.getId(), projection.getAsesorId());
        assertEquals("Asesor A", projection.getAsesorNombre());
        assertEquals(true, projection.getConciliacion());
    }

    @Test
    void accesoATodosLosGettersDeProjectionNoDebeGenerarNMasUno() {
        Statistics statistics = statistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<EstudianteResumenProjection> resultado = buscar(
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
            item.getSedeId();
            item.getSedeNombre();
            item.getAsesorId();
            item.getAsesorNombre();
            item.getConciliacion();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
        assertEquals(0, statistics.getCollectionLoadCount());
        assertEquals(0, statistics.getCollectionFetchCount());
    }

    private Page<EstudianteResumenProjection> buscar(
            String search,
            Boolean activo,
            Long asesorIdScope,
            PageRequest pageable) {
        return estudianteRepository.buscarResumenPaginado(search, activo, asesorIdScope, pageable);
    }

    private void crearCatalogos() {
        tipoDocumento = new TipoDocumento();
        tipoDocumento.setNombre("Cedula de ciudadania");
        entityManager.persist(tipoDocumento);

        sedePrincipal = new Sede();
        sedePrincipal.setNombre("Sede Principal");
        entityManager.persist(sedePrincipal);

        areaCivil = new Area();
        areaCivil.setNombre("Area Civil");
        entityManager.persist(areaCivil);
    }

    private Asesor crearAsesor(String nombre, String usuario) {
        Asesor asesor = new Asesor();
        asesor.setNombre(nombre);
        asesor.setTipoDocumento(tipoDocumento);
        asesor.setDocumento("DOC-" + usuario);
        asesor.setEmail(usuario + "@example.test");
        asesor.setTelefono("310-" + usuario);
        asesor.setUsuario(usuario);
        asesor.setCodigo("ASE-" + usuario);
        asesor.setActivo(true);
        asesor.setArea(areaCivil);
        asesor.setSede(sedePrincipal);
        entityManager.persist(asesor);
        return asesor;
    }

    private Estudiante crearEstudiante(
            String nombre,
            String documento,
            String email,
            String usuario,
            String codigo,
            Boolean activo,
            Boolean conciliacion,
            Asesor asesor) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(nombre);
        estudiante.setTipoDocumento(tipoDocumento);
        estudiante.setDocumento(documento);
        estudiante.setEmail(email);
        estudiante.setTelefono("300-" + documento);
        estudiante.setUsuario(usuario);
        estudiante.setCodigo(codigo);
        estudiante.setSede(sedePrincipal);
        estudiante.setActivo(activo);
        estudiante.setConciliacion(conciliacion);
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);
        return estudiante;
    }

    private Sort sortIdDesc() {
        return Sort.by(Sort.Order.desc("id"));
    }

    private List<Long> ids(Page<EstudianteResumenProjection> page) {
        return page.getContent().stream()
                .map(EstudianteResumenProjection::getId)
                .toList();
    }

    private void assertIds(Page<EstudianteResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), ids(page));
    }

    private void assertSoloAsesor(Page<EstudianteResumenProjection> page, Long asesorId) {
        assertTrue(page.getContent().stream()
                .allMatch(item -> asesorId.equals(item.getAsesorId())));
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
    }
}
