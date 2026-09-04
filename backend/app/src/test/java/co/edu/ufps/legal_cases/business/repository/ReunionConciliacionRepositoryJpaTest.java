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
import co.edu.ufps.legal_cases.business.model.conciliacion.reunion.ReunionConciliacion;
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
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionAgendaProjection;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionRepository;
import co.edu.ufps.legal_cases.business.repository.conciliacion.reunion.ReunionConciliacionResumenProjection;
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
class ReunionConciliacionRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private ReunionConciliacionRepository reunionRepository;

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

    private ReunionConciliacion reunionAlpha;
    private ReunionConciliacion reunionBeta;
    private ReunionConciliacion reunionAsesorA;
    private ReunionConciliacion reunionMonitorA;
    private ReunionConciliacion reunionConciliadorA;
    private ReunionConciliacion reunionEstudianteDirecto;
    private ReunionConciliacion reunionEstudiantePorConsulta;
    private ReunionConciliacion reunionArchivada;
    private ReunionConciliacion reunionInactiva;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearReunionesBase();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void paginacionGlobalDebeExcluirConsultasArchivadasYConciliacionesInactivas() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(7, resultado.getTotalElements());
        assertEquals(4, resultado.getTotalPages());
        assertTrue(ids(resultado).stream().noneMatch(reunionArchivada.getConciliacionId()::equals));
        assertTrue(ids(resultado).stream().noneMatch(reunionInactiva.getConciliacionId()::equals));
    }

    @Test
    void paginaFueraDeRangoDebeRetornarContentVacioConTotalReal() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
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
        crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Sofia", "Search", "DOC-S-001"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta clavereunion descripcion", LocalDate.of(2026, 2, 1)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 3, 1, 8, 0),
                null,
                null), LocalDateTime.of(2026, 3, 10, 8, 0), "extra uno");
        crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Clavereunion", "Torres", "DOC-S-002"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta comun", LocalDate.of(2026, 2, 2)),
                estudianteA,
                conciliadorA,
                estadoReunion,
                true,
                LocalDateTime.of(2026, 3, 2, 8, 0),
                null,
                null), LocalDateTime.of(2026, 3, 11, 8, 0), "extra dos");
        crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Mario", "Search", "DOC-S-003"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta comun", LocalDate.of(2026, 2, 3)),
                estudianteA,
                crearConciliador("Clavereunion", "CON-CLAVE", "C-CLAVE", "conciliador.clave"),
                estadoConciliado,
                true,
                LocalDateTime.of(2026, 3, 3, 8, 0),
                null,
                null), LocalDateTime.of(2026, 3, 12, 8, 0), "extra tres");
        crearReunion(crearConciliacion(
                crearConsulta(crearPersona("No", "Coincide", "DOC-S-004"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta sin coincidencia", LocalDate.of(2026, 2, 4)),
                estudianteA,
                conciliadorA,
                estadoNoConciliado,
                true,
                LocalDateTime.of(2026, 3, 4, 8, 0),
                null,
                null), LocalDateTime.of(2026, 3, 13, 8, 0), "extra cuatro");
        entityManager.flush();
        entityManager.clear();

        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                "clavereunion",
                null,
                null,
                null,
                PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void searchDebeCoincidirPorConsultaPersonaEstadoSedeEstudianteYConciliador() {
        assertIds(buscarGlobal("consulta alpha", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), reunionAlpha.getConciliacionId());
        assertIds(buscarGlobal("Ana Alvarez", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), reunionAlpha.getConciliacionId());
        assertIds(buscarGlobal("Alvarez", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), reunionAlpha.getConciliacionId());
        assertIds(buscarGlobal("DOC-001", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), reunionAlpha.getConciliacionId());
        assertEquals(3, buscarGlobal("EN_ESPERA", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())).getTotalElements());
        assertEquals(7, buscarGlobal("Principal", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())).getTotalElements());
        assertEquals(5, buscarGlobal("Estudiante B", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())).getTotalElements());
        assertIds(buscarGlobal("Conciliador A", null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), reunionConciliadorA.getConciliacionId(),
                reunionAlpha.getConciliacionId());
    }

    @Test
    void filtroEstadoDebeAplicarseEnContentYCount() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                "EN_ESPERA",
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(
                resultado,
                reunionEstudiantePorConsulta.getConciliacionId(),
                reunionEstudianteDirecto.getConciliacionId(),
                reunionAlpha.getConciliacionId());
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void rangoDeFechasDebeAplicarseSobreFechaReunionEnContentYCount() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 10),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, reunionAsesorA.getConciliacionId(), reunionAlpha.getConciliacionId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void fechaHastaDebeSerExclusiva() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 14),
                PageRequest.of(0, 10, Sort.by(
                        Sort.Order.asc("fechaReunion"),
                        Sort.Order.asc("conciliacionId"))));

        assertEquals(6, resultado.getTotalElements());
        assertTrue(ids(resultado).stream().noneMatch(reunionEstudiantePorConsulta.getConciliacionId()::equals));
    }

    @Test
    void filtrosCombinadosDebenAplicarseConAnd() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                "alpha",
                "EN_ESPERA",
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 10),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, reunionAlpha.getConciliacionId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void scopeAsesorDebeVerSoloReunionesDeConsultasAsignadasDirectamente() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarRestringido(
                "ASESOR",
                asesorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, reunionAsesorA.getConciliacionId(), reunionAlpha.getConciliacionId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeVerSoloReunionesDeConsultasAsignadas() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarRestringido(
                "MONITOR",
                monitorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, reunionMonitorA.getConciliacionId(), reunionAlpha.getConciliacionId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeConciliadorDebeVerSoloReunionesAsignadas() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarRestringido(
                "CONCILIADOR",
                conciliadorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, reunionConciliadorA.getConciliacionId(), reunionAlpha.getConciliacionId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeEstudianteDebeVerRelacionDirectaYPorConsulta() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarRestringido(
                "ESTUDIANTE",
                estudianteA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(
                resultado,
                reunionEstudiantePorConsulta.getConciliacionId(),
                reunionEstudianteDirecto.getConciliacionId(),
                reunionAlpha.getConciliacionId());
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void perfilNoSoportadoDebeResolverFailClosed() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarRestringido(
                "ADMINISTRATIVO",
                999L,
                PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void ordenPorFechaReunionDebeUsarConciliacionIdAscComoDesempate() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(
                        Sort.Order.asc("fechaReunion"),
                        Sort.Order.asc("conciliacionId"))));

        List<Long> ids = ids(resultado);

        assertTrue(ids.indexOf(reunionAlpha.getConciliacionId())
                < ids.indexOf(reunionAsesorA.getConciliacionId()));
    }

    @Test
    void proyeccionResumenDebeExponerCamposEsperados() {
        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                "consulta alpha",
                null,
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        ReunionConciliacionResumenProjection projection = resultado.getContent().getFirst();

        assertEquals(reunionAlpha.getConciliacionId(), projection.getConciliacionId());
        assertNotNull(projection.getVersion());
        assertNotNull(projection.getConciliacionVersion());
        assertEquals(reunionAlpha.getConciliacion().getConsulta().getId(), projection.getConsultaId());
        assertEquals(estadoEspera.getCodigo(), projection.getEstadoCodigo());
        assertEquals(estadoEspera.getNombre(), projection.getEstadoNombre());
        assertEquals(sede.getId(), projection.getSedeId());
        assertEquals("Principal", projection.getSedeNombre());
        assertEquals(LocalDateTime.of(2026, 2, 10, 9, 0), projection.getFechaReunion());
        assertEquals("Audiencia alpha", projection.getObservaciones());
        assertEquals(estudianteA.getId(), projection.getEstudianteId());
        assertEquals("Estudiante A", projection.getEstudianteNombre());
        assertEquals(conciliadorA.getId(), projection.getConciliadorId());
        assertEquals("Conciliador A", projection.getConciliadorNombre());
        assertEquals(LocalDateTime.of(2026, 2, 1, 9, 0), projection.getFechaCreacion());
    }

    @Test
    void agendaDebeConsultarDirectamentePorRangoYScope() {
        List<ReunionAgendaProjection> resultado = reunionRepository.buscarParaAgenda(
                LocalDateTime.of(2026, 2, 10, 0, 0),
                LocalDateTime.of(2026, 2, 15, 0, 0),
                false,
                "CONCILIADOR",
                conciliadorA.getId(),
                EstadoConsulta.ARCHIVADO);

        assertEquals(List.of(
                reunionAlpha.getConciliacionId(),
                reunionConciliadorA.getConciliacionId()), resultado.stream()
                        .map(ReunionAgendaProjection::getConciliacionId)
                        .toList());
        assertTrue(resultado.stream().allMatch(item -> item.getFechaReunion()
                .isBefore(LocalDateTime.of(2026, 2, 15, 0, 0))));
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

        Page<ReunionConciliacionResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortIdDesc()));

        resultado.getContent().forEach(item -> {
            item.getConciliacionId();
            item.getVersion();
            item.getConciliacionVersion();
            item.getConsultaId();
            item.getEstadoCodigo();
            item.getEstadoNombre();
            item.getSedeId();
            item.getSedeNombre();
            item.getFechaReunion();
            item.getObservaciones();
            item.getEstudianteId();
            item.getEstudianteNombre();
            item.getConciliadorId();
            item.getConciliadorNombre();
            item.getFechaCreacion();
            item.getFechaActualizacion();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    private Page<ReunionConciliacionResumenProjection> buscarGlobal(
            String search,
            String estadoCodigo,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            PageRequest pageable) {
        return reunionRepository.buscarResumenPaginado(
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

    private Page<ReunionConciliacionResumenProjection> buscarRestringido(
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return reunionRepository.buscarResumenPaginado(
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
        return Sort.by(Sort.Order.desc("conciliacionId"));
    }

    private List<Long> ids(Page<ReunionConciliacionResumenProjection> page) {
        return page.getContent().stream()
                .map(ReunionConciliacionResumenProjection::getConciliacionId)
                .toList();
    }

    private void assertIds(Page<ReunionConciliacionResumenProjection> page, Long... expectedIds) {
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

        Rol rolSistema = new Rol();
        rolSistema.setNombre("ROL_REUNION_TEST");
        rolSistema.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rolSistema);

        usuarioSistema = new UsuarioSistema();
        usuarioSistema.setUsername("sistema_reunion_test@example.test");
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

    private void crearReunionesBase() {
        reunionAlpha = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Ana", "Alvarez", "DOC-001"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta alpha", LocalDate.of(2026, 1, 10)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 2, 1, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 10, 9, 0), "Audiencia alpha");

        reunionBeta = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Bruno", "Bermudez", "DOC-002"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta beta", LocalDate.of(2026, 1, 11)),
                estudianteB,
                conciliadorB,
                estadoReunion,
                true,
                LocalDateTime.of(2026, 2, 2, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 11, 10, 0), "Audiencia beta");

        reunionAsesorA = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Carla", "Castillo", "DOC-003"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteB, monitorB,
                        "consulta directa asesor", LocalDate.of(2026, 1, 12)),
                estudianteB,
                conciliadorB,
                estadoConciliado,
                true,
                LocalDateTime.of(2026, 2, 3, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 10, 9, 0), "Audiencia asesor");

        reunionMonitorA = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Diana", "Duarte", "DOC-004"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorA,
                        "consulta monitor", LocalDate.of(2026, 1, 13)),
                estudianteB,
                conciliadorB,
                estadoNoConciliado,
                true,
                LocalDateTime.of(2026, 2, 4, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 13, 23, 59), "Audiencia monitor");

        reunionConciliadorA = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Elena", "Escalante", "DOC-005"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta conciliador", LocalDate.of(2026, 1, 14)),
                estudianteB,
                conciliadorA,
                estadoReunion,
                true,
                LocalDateTime.of(2026, 2, 5, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 12, 10, 0), "Audiencia conciliador");

        reunionEstudianteDirecto = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Fabio", "Fuentes", "DOC-006"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta estudiante directo", LocalDate.of(2026, 1, 15)),
                estudianteA,
                conciliadorB,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 2, 6, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 14, 9, 0), "Audiencia estudiante directo");

        reunionEstudiantePorConsulta = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Gloria", "Garcia", "DOC-007"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteA, monitorB,
                        "consulta estudiante por consulta", LocalDate.of(2026, 1, 16)),
                estudianteB,
                conciliadorB,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 2, 7, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 15, 9, 0), "Audiencia estudiante consulta");

        reunionArchivada = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Helena", "Herrera", "DOC-008"),
                        EstadoConsulta.ARCHIVADO, asesorA, estudianteA, monitorA,
                        "consulta archivada", LocalDate.of(2026, 1, 17)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                true,
                LocalDateTime.of(2026, 2, 8, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 16, 9, 0), "Audiencia archivada");

        reunionInactiva = crearReunion(crearConciliacion(
                crearConsulta(crearPersona("Ivan", "Ibarra", "DOC-009"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta conciliacion inactiva", LocalDate.of(2026, 1, 18)),
                estudianteA,
                conciliadorA,
                estadoEspera,
                false,
                LocalDateTime.of(2026, 2, 9, 8, 0),
                null,
                null), LocalDateTime.of(2026, 2, 17, 9, 0), "Audiencia inactiva");
    }

    private ReunionConciliacion crearReunion(
            Conciliacion conciliacion,
            LocalDateTime fechaReunion,
            String observaciones) {
        ReunionConciliacion reunion = new ReunionConciliacion();
        reunion.setConciliacion(conciliacion);
        reunion.setFechaReunion(fechaReunion);
        reunion.setSede(sede);
        reunion.setObservaciones(observaciones);
        reunion.setFechaCreacion(fechaReunion.minusDays(9));
        entityManager.persist(reunion);
        return reunion;
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
