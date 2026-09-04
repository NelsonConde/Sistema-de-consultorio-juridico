package co.edu.ufps.legal_cases.business.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionResumenProjection;
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
class ConciliacionRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private ConciliacionRepository conciliacionRepository;

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
    private Area area;
    private Tema tema;
    private Tipo tipo;
    private TipoDocumento tipoDocumentoPerfil;
    private Rol rolSistema;
    private UsuarioSistema usuarioSistema;
    private EstadoConciliacion estadoEspera;
    private EstadoConciliacion estadoReunion;
    private EstadoConciliacion estadoConciliado;
    private EstadoConciliacion estadoNoConciliado;
    private Asesor asesorA;
    private Asesor asesorB;
    private Estudiante estudianteA;
    private Estudiante estudianteB;
    private Monitor monitorA;
    private Monitor monitorB;
    private Conciliador conciliadorA;
    private Conciliador conciliadorB;

    private Conciliacion conciliacionAlpha;
    private Conciliacion conciliacionBeta;
    private Conciliacion conciliacionAsesorA;
    private Conciliacion conciliacionMonitorA;
    private Conciliacion conciliacionConciliadorA;
    private Conciliacion conciliacionEstudianteDirecto;
    private Conciliacion conciliacionEstudiantePorConsulta;
    private Conciliacion conciliacionArchivada;
    private Conciliacion conciliacionInactiva;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearConciliacionesBase();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void paginacionGlobalDebeExcluirConsultasArchivadasYConciliacionesInactivas() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(7, resultado.getTotalElements());
        assertEquals(4, resultado.getTotalPages());
        assertTrue(ids(resultado).stream().noneMatch(conciliacionArchivada.getId()::equals));
        assertTrue(ids(resultado).stream().noneMatch(conciliacionInactiva.getId()::equals));
    }

    @Test
    void paginaFueraDeRangoDebeRetornarContentVacioConTotalReal() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(10, 2, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(7, resultado.getTotalElements());
        assertEquals(4, resultado.getTotalPages());
    }

    @Test
    void searchDebeAplicarseAntesDePaginarYContarSoloCoincidencias() {
        crearConciliacion(
                crearConsulta(crearPersona("Sofia", "Search", "DOC-S-001"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta claveconciliacion descripcion", LocalDate.of(2026, 2, 1)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 3, 1, 8, 0),
                null,
                null);
        crearConciliacion(
                crearConsulta(crearPersona("Claveconciliacion", "Torres", "DOC-S-002"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta comun", LocalDate.of(2026, 2, 2)),
                estudianteA,
                conciliadorA,
                estadoReunion,
                true,
                LocalDateTime.of(2026, 3, 2, 8, 0),
                null,
                null);
        crearConciliacion(
                crearConsulta(crearPersona("Mario", "Search", "DOC-S-003"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta comun", LocalDate.of(2026, 2, 3)),
                estudianteA,
                crearConciliador("Claveconciliacion", "CON-CLAVE", "C-CLAVE", "conciliador.clave"),
                estadoConciliado,
                true,
                LocalDateTime.of(2026, 3, 3, 8, 0),
                null,
                null);
        crearConciliacion(
                crearConsulta(crearPersona("No", "Coincide", "DOC-S-004"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta sin coincidencia", LocalDate.of(2026, 2, 4)),
                estudianteA,
                conciliadorA,
                estadoNoConciliado,
                true,
                LocalDateTime.of(2026, 3, 4, 8, 0),
                null,
                null);
        entityManager.flush();
        entityManager.clear();

        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                "claveconciliacion",
                null,
                null,
                null,
                PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void searchDebeCoincidirPorConsultaPersonaEstadoEstudianteYConciliador() {
        assertIds(buscarGlobal("consulta alpha", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), conciliacionAlpha.getId());
        assertIds(buscarGlobal("Ana Alvarez", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), conciliacionAlpha.getId());
        assertIds(buscarGlobal("Alvarez", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), conciliacionAlpha.getId());
        assertIds(buscarGlobal("DOC-001", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), conciliacionAlpha.getId());
        assertEquals(3, buscarGlobal("EN_ESPERA", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())).getTotalElements());
        assertEquals(5, buscarGlobal("Estudiante B", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())).getTotalElements());
        assertIds(buscarGlobal("Conciliador A", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), conciliacionConciliadorA.getId(), conciliacionAlpha.getId());
    }

    @Test
    void filtroEstadoDebeAplicarseEnContentYCount() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                "EN_ESPERA",
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(
                resultado,
                conciliacionEstudiantePorConsulta.getId(),
                conciliacionEstudianteDirecto.getId(),
                conciliacionAlpha.getId());
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void rangoDeFechasDebeAplicarseSobreFechaCreacionEnContentYCount() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, conciliacionAsesorA.getId(), conciliacionAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtrosCombinadosDebenAplicarseConAnd() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                "alpha",
                "EN_ESPERA",
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, conciliacionAlpha.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void scopeAsesorDebeVerSoloConciliacionesDeConsultasAsignadasDirectamente() {
        Page<ConciliacionResumenProjection> resultado = buscarRestringido(
                "ASESOR",
                asesorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, conciliacionAsesorA.getId(), conciliacionAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeVerSoloConciliacionesDeConsultasAsignadas() {
        Page<ConciliacionResumenProjection> resultado = buscarRestringido(
                "MONITOR",
                monitorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, conciliacionMonitorA.getId(), conciliacionAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeConciliadorDebeVerSoloConciliacionesAsignadas() {
        Page<ConciliacionResumenProjection> resultado = buscarRestringido(
                "CONCILIADOR",
                conciliadorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, conciliacionConciliadorA.getId(), conciliacionAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeEstudianteDebeVerRelacionDirectaYPorConsulta() {
        Page<ConciliacionResumenProjection> resultado = buscarRestringido(
                "ESTUDIANTE",
                estudianteA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(
                resultado,
                conciliacionEstudiantePorConsulta.getId(),
                conciliacionEstudianteDirecto.getId(),
                conciliacionAlpha.getId());
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void perfilNoSoportadoDebeResolverFailClosed() {
        Page<ConciliacionResumenProjection> resultado = buscarRestringido(
                "ADMINISTRATIVO",
                999L,
                PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void ordenPorFechaCreacionDebeUsarIdAscComoDesempate() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(
                        Sort.Order.asc("fechaCreacion"),
                        Sort.Order.asc("id"))));

        List<Long> ids = ids(resultado);

        assertTrue(ids.indexOf(conciliacionAlpha.getId()) < ids.indexOf(conciliacionAsesorA.getId()));
    }

    @Test
    void proyeccionResumenDebeExponerCamposEsperados() {
        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                "consulta alpha",
                null,
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        ConciliacionResumenProjection projection = resultado.getContent().getFirst();

        assertEquals(conciliacionAlpha.getId(), projection.getId());
        assertNotNull(projection.getVersion());
        assertEquals("consulta alpha", projection.getConsulta());
        assertEquals(estadoEspera.getCodigo(), projection.getEstadoCodigo());
        assertEquals(estadoEspera.getNombre(), projection.getEstadoNombre());
        assertEquals(estudianteA.getId(), projection.getEstudianteId());
        assertEquals("Estudiante A", projection.getEstudianteNombre());
        assertEquals(conciliadorA.getId(), projection.getConciliadorId());
        assertEquals("Conciliador A", projection.getConciliadorNombre());
        assertEquals(LocalDateTime.of(2026, 1, 10, 9, 0), projection.getFechaCreacion());
        assertEquals(LocalDateTime.of(2026, 1, 20, 10, 0), projection.getFechaConciliacion());
        assertEquals(true, projection.getActivo());
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

        Page<ConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortIdDesc()));

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getVersion();
            item.getConsultaId();
            item.getConsulta();
            item.getEstadoCodigo();
            item.getEstadoNombre();
            item.getEstudianteId();
            item.getEstudianteNombre();
            item.getConciliadorId();
            item.getConciliadorNombre();
            item.getFechaCreacion();
            item.getFechaConciliacion();
            item.getFechaFinalizacion();
            item.getActivo();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    private Page<ConciliacionResumenProjection> buscarGlobal(
            String search,
            String estadoCodigo,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            PageRequest pageable) {
        return conciliacionRepository.buscarResumenPaginado(
                search,
                estadoCodigo,
                fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Page<ConciliacionResumenProjection> buscarRestringido(
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return conciliacionRepository.buscarResumenPaginado(
                null,
                null,
                null,
                null,
                false,
                tipoPerfil,
                perfilId,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Sort sortIdDesc() {
        return Sort.by(Sort.Order.desc("id"));
    }

    private List<Long> ids(Page<ConciliacionResumenProjection> page) {
        return page.getContent().stream()
                .map(ConciliacionResumenProjection::getId)
                .toList();
    }

    private void assertIds(Page<ConciliacionResumenProjection> page, Long... expectedIds) {
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

        area = new Area();
        area.setNombre("Civil");
        entityManager.persist(area);

        tema = new Tema();
        tema.setNombre("Contratos");
        tema.setArea(area);
        entityManager.persist(tema);

        tipo = new Tipo();
        tipo.setNombre("Asesoria civil");
        tipo.setTema(tema);
        entityManager.persist(tipo);

        tipoDocumentoPerfil = new TipoDocumento();
        tipoDocumentoPerfil.setNombre("Cedula de ciudadania");
        entityManager.persist(tipoDocumentoPerfil);

        rolSistema = new Rol();
        rolSistema.setNombre("ROL_CONCILIACION_TEST");
        rolSistema.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rolSistema);

        usuarioSistema = new UsuarioSistema();
        usuarioSistema.setUsername("sistema_conciliacion_test@example.test");
        usuarioSistema.setPasswordHash("hash");
        usuarioSistema.setRol(rolSistema);
        entityManager.persist(usuarioSistema);

        estadoEspera = crearEstado("EN_ESPERA", "En espera", 1);
        estadoReunion = crearEstado("REUNION_PROGRAMADA", "Reunion programada", 2);
        estadoConciliado = crearEstado("COMPLETO_CONCILIADO", "Completo conciliado", 3);
        estadoNoConciliado = crearEstado("COMPLETO_NO_CONCILIADO", "Completo no conciliado", 4);
    }

    private EstadoConciliacion crearEstado(String codigo, String nombre, int orden) {
        EstadoConciliacion estado = new EstadoConciliacion();
        estado.setCodigo(codigo);
        estado.setNombre(nombre);
        estado.setOrden(orden);
        estado.setActivo(true);
        entityManager.persist(estado);
        return estado;
    }

    private void crearPerfiles() {
        asesorA = crearAsesor("Asesor A", "ASE-A", "A-001", "asesor.a");
        asesorB = crearAsesor("Asesor B", "ASE-B", "A-002", "asesor.b");
        estudianteA = crearEstudiante("Estudiante A", "EST-A", "E-001", "estudiante.a", asesorA);
        estudianteB = crearEstudiante("Estudiante B", "EST-B", "E-002", "estudiante.b", asesorB);
        monitorA = crearMonitor("Monitor A", "MON-A", "M-001", "monitor.a");
        monitorB = crearMonitor("Monitor B", "MON-B", "M-002", "monitor.b");
        conciliadorA = crearConciliador("Conciliador A", "CON-A", "C-001", "conciliador.a");
        conciliadorB = crearConciliador("Conciliador B", "CON-B", "C-002", "conciliador.b");
    }

    private Asesor crearAsesor(String nombre, String codigo, String documento, String usuario) {
        Asesor asesor = new Asesor();
        asesor.setNombre(nombre);
        asesor.setTipoDocumento(tipoDocumentoPerfil);
        asesor.setDocumento(documento);
        asesor.setEmail(usuario + "@example.test");
        asesor.setTelefono("300-" + documento);
        asesor.setUsuario(usuario);
        asesor.setSede(sede);
        asesor.setCodigo(codigo);
        asesor.setArea(area);
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
        estudiante.setEmail(usuario + "@example.test");
        estudiante.setTelefono("310-" + documento);
        estudiante.setUsuario(usuario);
        estudiante.setSede(sede);
        estudiante.setCodigo(codigo);
        estudiante.setAsesor(asesor);
        estudiante.setConciliacion(true);
        entityManager.persist(estudiante);
        return estudiante;
    }

    private Monitor crearMonitor(String nombre, String codigo, String documento, String usuario) {
        Monitor monitor = new Monitor();
        monitor.setNombre(nombre);
        monitor.setTipoDocumento(tipoDocumentoPerfil);
        monitor.setDocumento(documento);
        monitor.setEmail(usuario + "@example.test");
        monitor.setTelefono("320-" + documento);
        monitor.setUsuario(usuario);
        monitor.setCodigo(codigo);
        monitor.setSede(sede);
        entityManager.persist(monitor);
        return monitor;
    }

    private Conciliador crearConciliador(String nombre, String codigo, String documento, String usuario) {
        Conciliador conciliador = new Conciliador();
        conciliador.setNombre(nombre);
        conciliador.setTipoDocumento(tipoDocumentoPerfil);
        conciliador.setDocumento(documento);
        conciliador.setEmail(usuario + "@example.test");
        conciliador.setTelefono("330-" + documento);
        conciliador.setUsuario(usuario);
        conciliador.setSede(sede);
        conciliador.setCodigo(codigo);
        conciliador.setTipoConciliador(TipoConciliador.INTERNO);
        conciliador.setActivo(true);
        entityManager.persist(conciliador);
        return conciliador;
    }

    private void crearConciliacionesBase() {
        conciliacionAlpha = crearConciliacion(
                crearConsulta(crearPersona("Ana", "Alvarez", "DOC-001"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta alpha", LocalDate.of(2026, 1, 10)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0),
                null);

        conciliacionBeta = crearConciliacion(
                crearConsulta(crearPersona("Bruno", "Bermudez", "DOC-002"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta beta", LocalDate.of(2026, 1, 11)),
                estudianteB,
                conciliadorB,
                estadoReunion,
                true,
                LocalDateTime.of(2026, 1, 11, 9, 0),
                LocalDateTime.of(2026, 1, 21, 10, 0),
                null);

        conciliacionAsesorA = crearConciliacion(
                crearConsulta(crearPersona("Carla", "Castillo", "DOC-003"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteB, monitorB,
                        "consulta directa asesor", LocalDate.of(2026, 1, 10)),
                estudianteB,
                conciliadorB,
                estadoConciliado,
                true,
                LocalDateTime.of(2026, 1, 10, 15, 0),
                LocalDateTime.of(2026, 1, 22, 10, 0),
                LocalDateTime.of(2026, 1, 30, 8, 0));

        conciliacionMonitorA = crearConciliacion(
                crearConsulta(crearPersona("Diana", "Duarte", "DOC-004"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorA,
                        "consulta monitor", LocalDate.of(2026, 1, 13)),
                estudianteB,
                conciliadorB,
                estadoNoConciliado,
                true,
                LocalDateTime.of(2026, 1, 13, 23, 59),
                LocalDateTime.of(2026, 1, 23, 10, 0),
                LocalDateTime.of(2026, 1, 31, 8, 0));

        conciliacionConciliadorA = crearConciliacion(
                crearConsulta(crearPersona("Elena", "Escalante", "DOC-005"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta conciliador", LocalDate.of(2026, 1, 12)),
                estudianteB,
                conciliadorA,
                estadoReunion,
                true,
                LocalDateTime.of(2026, 1, 12, 9, 0),
                LocalDateTime.of(2026, 1, 24, 10, 0),
                null);

        conciliacionEstudianteDirecto = crearConciliacion(
                crearConsulta(crearPersona("Fabio", "Fuentes", "DOC-006"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta estudiante directo", LocalDate.of(2026, 1, 14)),
                estudianteA,
                conciliadorB,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 1, 14, 9, 0),
                LocalDateTime.of(2026, 1, 25, 10, 0),
                null);

        conciliacionEstudiantePorConsulta = crearConciliacion(
                crearConsulta(crearPersona("Gloria", "Garcia", "DOC-007"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteA, monitorB,
                        "consulta estudiante por consulta", LocalDate.of(2026, 1, 15)),
                estudianteB,
                conciliadorB,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 1, 15, 9, 0),
                LocalDateTime.of(2026, 1, 26, 10, 0),
                null);

        conciliacionArchivada = crearConciliacion(
                crearConsulta(crearPersona("Helena", "Herrera", "DOC-008"),
                        EstadoConsulta.ARCHIVADO, asesorA, estudianteA, monitorA,
                        "consulta archivada", LocalDate.of(2026, 1, 16)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 1, 16, 9, 0),
                LocalDateTime.of(2026, 1, 27, 10, 0),
                null);

        conciliacionInactiva = crearConciliacion(
                crearConsulta(crearPersona("Ivan", "Ibarra", "DOC-009"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta conciliacion inactiva", LocalDate.of(2026, 1, 17)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                false,
                LocalDateTime.of(2026, 1, 17, 9, 0),
                LocalDateTime.of(2026, 1, 28, 10, 0),
                null);
    }

    private Conciliacion crearConciliacion(
            Consulta consulta,
            Estudiante estudiante,
            Conciliador conciliador,
            EstadoConciliacion estado,
            boolean activo,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaConciliacion,
            LocalDateTime fechaFinalizacion) {
        Conciliacion conciliacion = new Conciliacion();
        conciliacion.setConsulta(consulta);
        conciliacion.setEstudiante(estudiante);
        conciliacion.setConciliador(conciliador);
        conciliacion.setEstado(estado);
        conciliacion.setSolicitadoPor(usuarioSistema);
        conciliacion.setActivo(activo);
        conciliacion.setFechaCreacion(fechaCreacion);
        conciliacion.setFechaConciliacion(fechaConciliacion);
        conciliacion.setFechaFinalizacion(fechaFinalizacion);
        entityManager.persist(conciliacion);
        return conciliacion;
    }

    private Consulta crearConsulta(
            Persona persona,
            EstadoConsulta estado,
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
        persona.setPronombre("no informa");
        persona.setSexo("no informa");
        persona.setGenero("no informa");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("3001234567");
        persona.setCorreo(documento + "@example.test");
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
