package co.edu.ufps.legal_cases.business.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaResumenProjection;
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
class ConsultaRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private EntityManager entityManager;

    private TipoPersona tipoPersona;
    private Nacionalidad nacionalidad;
    private Condicion condicion;
    private Municipio municipio;
    private Barrio barrio;
    private Ocupacion ocupacion;
    private Empresa empresa;
    private Sede sede;
    private Area areaCivil;
    private Area areaLaboral;
    private Tema temaCivil;
    private Tema temaLaboral;
    private Tipo tipoCivil;
    private Tipo tipoLaboral;
    private TipoDocumento tipoDocumentoPerfil;

    private Asesor asesorA;
    private Asesor asesorB;
    private Estudiante estudianteA;
    private Estudiante estudianteB;
    private Monitor monitorA;
    private Monitor monitorB;

    private Persona personaAlpha;
    private Persona personaBeta;
    private Persona personaGamma;
    private Persona personaDelta;
    private Persona personaArchivada;

    private Consulta consultaAlpha;
    private Consulta consultaBeta;
    private Consulta consultaGamma;
    private Consulta consultaDelta;
    private Consulta consultaArchivada;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearPersonasBase();
        crearConsultasBase();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void paginacionYCountGlobalDebeExcluirArchivadas() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortFechaDescIdAsc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(4, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
    }

    @Test
    void segundaPaginaNoDebeRepetirElementosYDebeConservarTotales() {
        Page<ConsultaResumenProjection> primera = buscarGlobal(
                null, null, null, null, null, null,
                PageRequest.of(0, 2, sortFechaDescIdAsc()));
        Page<ConsultaResumenProjection> segunda = buscarGlobal(
                null, null, null, null, null, null,
                PageRequest.of(1, 2, sortFechaDescIdAsc()));

        assertEquals(2, segunda.getContent().size());
        assertTrue(ids(primera).stream().noneMatch(ids(segunda)::contains));
        assertEquals(4, segunda.getTotalElements());
        assertEquals(2, segunda.getTotalPages());
    }

    @Test
    void paginaPosteriorALaUltimaDebeRetornarContentVacioConTotalReal() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(10, 2, sortFechaDescIdAsc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(4, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
    }

    @Test
    void searchDebeAplicarseAntesDePaginarYContarSoloCoincidencias() {
        crearConsulta(
                crearPersona("Sofia", "Rojas", "DOC-S-001"),
                EstadoConsulta.ACTIVO,
                areaCivil,
                temaCivil,
                tipoCivil,
                asesorA,
                estudianteA,
                monitorA,
                "claveintegracion por descripcion",
                LocalDate.of(2026, 2, 1));
        crearConsulta(
                crearPersona("Claveintegracion", "Torres", "DOC-S-002"),
                EstadoConsulta.PENDIENTE,
                areaCivil,
                temaCivil,
                tipoCivil,
                asesorA,
                estudianteA,
                monitorA,
                "Consulta por nombre",
                LocalDate.of(2026, 2, 2));
        crearConsulta(
                crearPersona("Mario", "Claveintegracion", "DOC-S-003"),
                EstadoConsulta.CERRADO,
                areaLaboral,
                temaLaboral,
                tipoLaboral,
                asesorB,
                estudianteB,
                monitorB,
                "Consulta por apellido",
                LocalDate.of(2026, 2, 3));
        crearConsulta(
                crearPersona("No", "Coincide", "DOC-S-004"),
                EstadoConsulta.ACTIVO,
                areaLaboral,
                temaLaboral,
                tipoLaboral,
                asesorB,
                estudianteB,
                monitorB,
                "Consulta sin coincidencia",
                LocalDate.of(2026, 2, 4));
        entityManager.flush();
        entityManager.clear();

        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                "claveintegracion",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 1, sortFechaDescIdAsc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void searchPorNombreCompletoDebeEncontrarConsulta() {
        Consulta consulta = crearConsulta(
                crearPersona("Laura Maria", "Gomez Rojas", "DOC-LAURA"),
                EstadoConsulta.ACTIVO,
                areaCivil,
                temaCivil,
                tipoCivil,
                asesorA,
                estudianteA,
                monitorA,
                "Consulta nombre completo",
                LocalDate.of(2026, 3, 1));
        entityManager.flush();
        entityManager.clear();

        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                "Laura Maria Gomez",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consulta.getId());
    }

    @Test
    void filtroAreaIdDebeContarSoloConsultasDelArea() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                areaCivil.getId(),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaAlpha.getId(), consultaBeta.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtroEstadoDebeContarSoloConsultasDelEstado() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                EstadoConsulta.CERRADO,
                null,
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaGamma.getId());
        assertTrue(resultado.getContent().stream()
                .allMatch(item -> EstadoConsulta.CERRADO.equals(item.getEstado())));
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void estadoArchivadoDebeQuedarSiempreExcluido() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                null,
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void filtroAsesorIdNoDebeIncluirConsultasPorAsesorDelEstudiante() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                asesorA.getId(),
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaDelta.getId(), consultaAlpha.getId());
        assertFalse(ids(resultado).contains(consultaBeta.getId()));
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtroMonitorIdDebeContarSoloConsultasDelMonitor() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                monitorA.getId(),
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaDelta.getId(), consultaAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtroEstudianteIdDebeContarSoloConsultasDelEstudiante() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                null,
                estudianteA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaAlpha.getId(), consultaBeta.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeEstudianteDebeExcluirAjenasDelContentYDelTotal() {
        Consulta sinRelacion = crearConsulta(
                crearPersona("Ramon", "Rueda", "DOC-SIN-REL"),
                EstadoConsulta.ACTIVO,
                areaCivil,
                temaCivil,
                tipoCivil,
                null,
                null,
                null,
                "consulta sin relacion",
                LocalDate.of(2026, 1, 14));
        entityManager.flush();
        entityManager.clear();

        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                null,
                null,
                null,
                null,
                null,
                null,
                "ESTUDIANTE",
                estudianteA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaAlpha.getId(), consultaBeta.getId());
        assertFalse(ids(resultado).contains(sinRelacion.getId()));
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeAsesorDirectoDebeIncluirConsultaAsignadaDirectamente() {
        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                "asesor directo",
                null,
                null,
                null,
                null,
                null,
                "ASESOR",
                asesorA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaDelta.getId());
    }

    @Test
    void scopeAsesorPorEstudianteDebeIncluirConsultaDelEstudianteAsesorado() {
        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                "rama estudiante asesorado",
                null,
                null,
                null,
                null,
                null,
                "ASESOR",
                asesorA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaBeta.getId());
    }

    @Test
    void asesorAjenoNoDebeAparecerNiIncrementarTotal() {
        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                "solo asesor b",
                null,
                null,
                null,
                null,
                null,
                "ASESOR",
                asesorA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeExcluirConsultasDeOtrosMonitoresDelTotal() {
        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                null,
                null,
                null,
                null,
                null,
                null,
                "MONITOR",
                monitorA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaDelta.getId(), consultaAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeConciliadorDebeSerFailClosed() {
        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                null,
                null,
                null,
                null,
                null,
                null,
                "CONCILIADOR",
                999L,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void perfilNoSoportadoDebeSerFailClosed() {
        Page<ConsultaResumenProjection> resultado = buscarRestringido(
                null,
                null,
                null,
                null,
                null,
                null,
                "ADMINISTRATIVO",
                asesorA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void ordenPorFechaDebeDesempatarPorIdAsc() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                "empate determinista",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaAlpha.getId(), consultaBeta.getId());
        assertTrue(consultaAlpha.getId() < consultaBeta.getId());
    }

    @Test
    void sortPorPersonaNombresDebeOrdenarPorPropiedadRelacionada() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(
                        Sort.Order.asc("persona.nombres").ignoreCase(),
                        Sort.Order.asc("id"))));

        assertEquals(List.of("Ana", "Bruno", "Carla", "Diana"), nombres(resultado));
    }

    @Test
    void projectionResumenDebeExponerCamposEsperados() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                "proyeccion alpha",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        ConsultaResumenProjection projection = resultado.getContent().getFirst();

        assertEquals(consultaAlpha.getId(), projection.getId());
        assertNotNull(projection.getVersion());
        assertEquals("empate determinista proyeccion alpha", projection.getConsulta());
        assertEquals(LocalDate.of(2026, 1, 10), projection.getFecha());
        assertEquals("Ana", projection.getNombre());
        assertEquals("Alvarez", projection.getApellido());
        assertEquals("DOC-001", projection.getCedula());
        assertEquals(EstadoConsulta.ACTIVO, projection.getEstado());
    }

    @Test
    void accesoAGettersDeProjectionNoDebeGenerarNMasUno() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortFechaDescIdAsc()));

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getVersion();
            item.getConsulta();
            item.getFecha();
            item.getNombre();
            item.getApellido();
            item.getCedula();
            item.getEstado();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    @Test
    void filtrosCombinadosDebenAplicarseConAnd() {
        Page<ConsultaResumenProjection> resultado = buscarGlobal(
                null,
                areaCivil.getId(),
                EstadoConsulta.PENDIENTE,
                null,
                null,
                estudianteA.getId(),
                PageRequest.of(0, 10, sortFechaDescIdAsc()));

        assertIds(resultado, consultaBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void searchDebeCoincidirPorDescripcionNombreApellidoYDocumento() {
        assertIds(buscarGlobal("proyeccion alpha", null, null, null, null, null,
                PageRequest.of(0, 10, sortFechaDescIdAsc())), consultaAlpha.getId());
        assertIds(buscarGlobal("Bruno", null, null, null, null, null,
                PageRequest.of(0, 10, sortFechaDescIdAsc())), consultaBeta.getId());
        assertIds(buscarGlobal("Castillo", null, null, null, null, null,
                PageRequest.of(0, 10, sortFechaDescIdAsc())), consultaGamma.getId());
        assertIds(buscarGlobal("DOC-004", null, null, null, null, null,
                PageRequest.of(0, 10, sortFechaDescIdAsc())), consultaDelta.getId());
    }

    private Page<ConsultaResumenProjection> buscarGlobal(
            String search,
            Long areaId,
            EstadoConsulta estado,
            Long asesorId,
            Long monitorId,
            Long estudianteId,
            PageRequest pageable) {
        return consultaRepository.buscarResumenPaginado(
                search,
                areaId,
                estado,
                asesorId,
                monitorId,
                estudianteId,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Page<ConsultaResumenProjection> buscarRestringido(
            String search,
            Long areaId,
            EstadoConsulta estado,
            Long asesorId,
            Long monitorId,
            Long estudianteId,
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return consultaRepository.buscarResumenPaginado(
                search,
                areaId,
                estado,
                asesorId,
                monitorId,
                estudianteId,
                false,
                tipoPerfil,
                perfilId,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Sort sortFechaDescIdAsc() {
        return Sort.by(
                Sort.Order.desc("fecha"),
                Sort.Order.asc("id"));
    }

    private List<Long> ids(Page<ConsultaResumenProjection> page) {
        return page.getContent().stream()
                .map(ConsultaResumenProjection::getId)
                .toList();
    }

    private List<String> nombres(Page<ConsultaResumenProjection> page) {
        return page.getContent().stream()
                .map(ConsultaResumenProjection::getNombre)
                .toList();
    }

    private void assertIds(Page<ConsultaResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), ids(page));
    }

    private void crearCatalogos() {
        Departamento departamento = new Departamento();
        departamento.setNombre("Norte de Santander");
        entityManager.persist(departamento);

        municipio = new Municipio();
        municipio.setNombre("Cucuta");
        municipio.setDepartamento(departamento);
        entityManager.persist(municipio);

        barrio = new Barrio();
        barrio.setNombre("Centro");
        barrio.setMunicipio(municipio);
        entityManager.persist(barrio);

        nacionalidad = new Nacionalidad();
        nacionalidad.setNombre("Colombiana");
        entityManager.persist(nacionalidad);

        tipoPersona = new TipoPersona();
        tipoPersona.setNombre("Solicitante");
        entityManager.persist(tipoPersona);

        condicion = new Condicion();
        condicion.setNombre("Ninguna");
        entityManager.persist(condicion);

        ocupacion = new Ocupacion();
        ocupacion.setNombre("Independiente");
        entityManager.persist(ocupacion);

        empresa = new Empresa();
        empresa.setNombre("No aplica");
        entityManager.persist(empresa);

        sede = new Sede();
        sede.setNombre("Principal");
        entityManager.persist(sede);

        areaCivil = crearArea("Civil");
        areaLaboral = crearArea("Laboral");

        temaCivil = crearTema("Contratos", areaCivil);
        temaLaboral = crearTema("Despidos", areaLaboral);

        tipoCivil = crearTipo("Asesoria civil", temaCivil);
        tipoLaboral = crearTipo("Asesoria laboral", temaLaboral);

        tipoDocumentoPerfil = new TipoDocumento();
        tipoDocumentoPerfil.setNombre("Cedula de ciudadania");
        entityManager.persist(tipoDocumentoPerfil);
    }

    private Area crearArea(String nombre) {
        Area area = new Area();
        area.setNombre(nombre);
        entityManager.persist(area);
        return area;
    }

    private Tema crearTema(String nombre, Area area) {
        Tema tema = new Tema();
        tema.setNombre(nombre);
        tema.setArea(area);
        entityManager.persist(tema);
        return tema;
    }

    private Tipo crearTipo(String nombre, Tema tema) {
        Tipo tipo = new Tipo();
        tipo.setNombre(nombre);
        tipo.setTema(tema);
        entityManager.persist(tipo);
        return tipo;
    }

    private void crearPerfiles() {
        asesorA = crearAsesor("Asesor A", "ASE-A", "A-001", "asesor.a");
        asesorB = crearAsesor("Asesor B", "ASE-B", "A-002", "asesor.b");

        estudianteA = crearEstudiante("Estudiante A", "EST-A", "E-001", "estudiante.a", asesorA);
        estudianteB = crearEstudiante("Estudiante B", "EST-B", "E-002", "estudiante.b", asesorB);

        monitorA = crearMonitor("Monitor A", "MON-A", "M-001", "monitor.a");
        monitorB = crearMonitor("Monitor B", "MON-B", "M-002", "monitor.b");
    }

    private Asesor crearAsesor(String nombre, String codigo, String documento, String usuario) {
        Asesor asesor = new Asesor();
        asesor.setNombre(nombre);
        asesor.setTipoDocumento(tipoDocumentoPerfil);
        asesor.setDocumento(documento);
        asesor.setEmail(usuario + "@example.com");
        asesor.setTelefono("300-" + documento);
        asesor.setUsuario(usuario);
        asesor.setSede(sede);
        asesor.setCodigo(codigo);
        asesor.setArea(areaCivil);
        entityManager.persist(asesor);
        return asesor;
    }

    private Estudiante crearEstudiante(
            String nombre,
            String codigo,
            String documento,
            String usuario,
            Asesor asesor) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(nombre);
        estudiante.setTipoDocumento(tipoDocumentoPerfil);
        estudiante.setDocumento(documento);
        estudiante.setEmail(usuario + "@example.com");
        estudiante.setTelefono("310-" + documento);
        estudiante.setUsuario(usuario);
        estudiante.setSede(sede);
        estudiante.setCodigo(codigo);
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);
        return estudiante;
    }

    private Monitor crearMonitor(String nombre, String codigo, String documento, String usuario) {
        Monitor monitor = new Monitor();
        monitor.setNombre(nombre);
        monitor.setTipoDocumento(tipoDocumentoPerfil);
        monitor.setDocumento(documento);
        monitor.setEmail(usuario + "@example.com");
        monitor.setTelefono("320-" + documento);
        monitor.setUsuario(usuario);
        monitor.setCodigo(codigo);
        monitor.setSede(sede);
        entityManager.persist(monitor);
        return monitor;
    }

    private void crearPersonasBase() {
        personaAlpha = crearPersona("Ana", "Alvarez", "DOC-001");
        personaBeta = crearPersona("Bruno", "Bermudez", "DOC-002");
        personaGamma = crearPersona("Carla", "Castillo", "DOC-003");
        personaDelta = crearPersona("Diana", "Duarte", "DOC-004");
        personaArchivada = crearPersona("Elena", "Escalante", "DOC-005");
    }

    private void crearConsultasBase() {
        consultaAlpha = crearConsulta(
                personaAlpha,
                EstadoConsulta.ACTIVO,
                areaCivil,
                temaCivil,
                tipoCivil,
                asesorA,
                estudianteA,
                monitorA,
                "empate determinista proyeccion alpha",
                LocalDate.of(2026, 1, 10));

        consultaBeta = crearConsulta(
                personaBeta,
                EstadoConsulta.PENDIENTE,
                areaCivil,
                temaCivil,
                tipoCivil,
                asesorB,
                estudianteA,
                monitorB,
                "empate determinista rama estudiante asesorado",
                LocalDate.of(2026, 1, 10));

        consultaGamma = crearConsulta(
                personaGamma,
                EstadoConsulta.CERRADO,
                areaLaboral,
                temaLaboral,
                tipoLaboral,
                asesorB,
                estudianteB,
                monitorB,
                "solo asesor b",
                LocalDate.of(2026, 1, 11));

        consultaDelta = crearConsulta(
                personaDelta,
                EstadoConsulta.EN_PROCESO,
                areaLaboral,
                temaLaboral,
                tipoLaboral,
                asesorA,
                estudianteB,
                monitorA,
                "asesor directo",
                LocalDate.of(2026, 1, 12));

        consultaArchivada = crearConsulta(
                personaArchivada,
                EstadoConsulta.ARCHIVADO,
                areaCivil,
                temaCivil,
                tipoCivil,
                asesorA,
                estudianteA,
                monitorA,
                "consulta archivada",
                LocalDate.of(2026, 1, 13));
    }

    private Consulta crearConsulta(
            Persona persona,
            EstadoConsulta estado,
            Area area,
            Tema tema,
            Tipo tipo,
            Asesor asesor,
            Estudiante estudiante,
            Monitor monitor,
            String descripcion,
            LocalDate fecha) {
        Consulta consulta = new Consulta();
        consulta.setFecha(fecha);
        consulta.setDescripcion(descripcion);
        consulta.setHechos("Hechos de prueba");
        consulta.setPretensiones("Pretensiones de prueba");
        consulta.setConceptoJuridico("Concepto de prueba");
        consulta.setTramite("Asesoria");
        consulta.setEstado(estado);
        consulta.setPersona(persona);
        consulta.setSede(sede);
        consulta.setArea(area);
        consulta.setTema(tema);
        consulta.setTipo(tipo);
        consulta.setAsesor(asesor);
        consulta.setEstudiante(estudiante);
        consulta.setMonitor(monitor);
        entityManager.persist(consulta);
        return consulta;
    }

    private Persona crearPersona(String nombres, String apellidos, String documento) {
        Persona persona = new Persona();
        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento(documento);
        persona.setFechaExpedicion(LocalDate.of(2010, 1, 1));
        persona.setCiudadExpedicion("Cucuta");
        persona.setNombres(nombres);
        persona.setApellidos(apellidos);
        persona.setNombreIdentitario(nombres);
        persona.setPronombre("ella");
        persona.setSexo("mujer");
        persona.setGenero("femenino");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("3001234567");
        persona.setCorreo(documento + "@example.com");
        persona.setNacionalidad(nacionalidad);
        persona.setEstadoCivil("soltero");
        persona.setEscolaridad("universitaria");
        persona.setGrupoEtnico("no informa");
        persona.setCondicionActual(condicion);
        persona.setSabeLeerEscribir(true);
        persona.setDiscapacidad("ninguna");
        persona.setCaracterizacionPcd("no aplica");
        persona.setNecesitaAjustePcd(false);
        persona.setMunicipio(municipio);
        persona.setBarrio(barrio);
        persona.setDireccion("Direccion de prueba");
        persona.setComuna("Uno");
        persona.setLocalidad("Centro");
        persona.setEstrato(2);
        persona.setTipoVivienda("arrendada");
        persona.setZona("urbana");
        persona.setTenencia("arrendatario");
        persona.setNumeroPersonasACargo(0);
        persona.setIngresosAdicionales(false);
        persona.setEnergiaElectrica(true);
        persona.setAcueducto(true);
        persona.setAlcantarillado(true);
        persona.setOcupacion(ocupacion);
        persona.setEmpresa(empresa);
        persona.setSalario(0);
        persona.setCargo("No aplica");
        persona.setDireccionEmpresa("No aplica");
        persona.setTelefonoEmpresa("0000000000");
        persona.setComoSeEntero("Universidad");
        persona.setRelacionConUniversidad("Ninguna");
        persona.setActivo(true);
        entityManager.persist(persona);
        return persona;
    }
}
