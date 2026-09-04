package co.edu.ufps.legal_cases.business.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import co.edu.ufps.legal_cases.business.model.seguimiento.CategoriaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.EstadoSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.Seguimiento;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoAgendaProjection;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoRepository;
import co.edu.ufps.legal_cases.business.repository.seguimiento.SeguimientoResumenProjection;
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
class SeguimientoRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private SeguimientoRepository seguimientoRepository;

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
    private UsuarioSistema autorA;
    private UsuarioSistema autorB;
    private CategoriaSeguimiento categoriaAudiencia;
    private CategoriaSeguimiento categoriaDocumento;
    private Asesor asesorA;
    private Asesor asesorB;
    private Estudiante estudianteA;
    private Estudiante estudianteB;
    private Monitor monitorA;
    private Monitor monitorB;

    private Consulta consultaAlpha;
    private Consulta consultaBeta;
    private Consulta consultaGamma;
    private Consulta consultaDelta;
    private Seguimiento seguimientoAlpha;
    private Seguimiento seguimientoBeta;
    private Seguimiento seguimientoGamma;
    private Seguimiento seguimientoDelta;
    private Seguimiento seguimientoInactivo;
    private Seguimiento seguimientoArchivado;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearSeguimientosBase();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void paginacionGlobalDebeExcluirInactivosYConsultasArchivadas() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, null, null, PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(4, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
        assertFalse(ids(resultado).contains(seguimientoInactivo.getId()));
        assertFalse(ids(resultado).contains(seguimientoArchivado.getId()));
    }

    @Test
    void totalElementsDebeContarTodosLosResultadosDelFiltro() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, null, null, PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(4, resultado.getTotalElements());
        assertEquals(4, resultado.getTotalPages());
    }

    @Test
    void paginaFueraDeRangoDebeRetornarContentVacioConTotalReal() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, null, null, PageRequest.of(10, 2, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(4, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
    }

    @Test
    void searchDebeAplicarseAntesDePaginarYBuscarCamposEscalares() {
        CategoriaSeguimiento categoriaClave = crearCategoria("ClaveBusqueda Categoria");
        UsuarioSistema autorClave = crearUsuario("clavebusqueda.autor@example.test");
        crearSeguimiento(consultaAlpha, categoriaAudiencia, autorA, "clavebusqueda por descripcion",
                EstadoSeguimiento.PENDIENTE, true, false, LocalDateTime.of(2026, 2, 1, 8, 0));
        crearSeguimiento(consultaAlpha, categoriaClave, autorA, "seguimiento por categoria",
                EstadoSeguimiento.PENDIENTE, true, false, LocalDateTime.of(2026, 2, 2, 8, 0));
        crearSeguimiento(consultaAlpha, categoriaAudiencia, autorClave, "seguimiento por autor",
                EstadoSeguimiento.PENDIENTE, true, false, LocalDateTime.of(2026, 2, 3, 8, 0));
        crearSeguimiento(consultaAlpha, categoriaAudiencia, autorA, "seguimiento sin coincidencia",
                EstadoSeguimiento.PENDIENTE, true, false, LocalDateTime.of(2026, 2, 4, 8, 0));
        entityManager.flush();
        entityManager.clear();

        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                "clavebusqueda", null, null, null, null, null, PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void searchDebeCoincidirPorEstadoYDescripcionDeConsulta() {
        assertEquals(1, buscarGlobal("CANCELADO", null, null, null, null, null,
                PageRequest.of(0, 10, sortIdDesc())).getTotalElements());
        assertIds(buscarGlobal("consulta beta asesorado", null, null, null, null, null,
                PageRequest.of(0, 10, sortIdDesc())), seguimientoBeta.getId());
    }

    @Test
    void filtroEstadoDebeAplicarseEnContentYCount() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, EstadoSeguimiento.PENDIENTE, null, null, null, null, PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoDelta.getId(), seguimientoAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtroFechaCreacionDebeUsarDiaDesdeInclusivoYHastaExclusivo() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null,
                null,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10),
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoBeta.getId(), seguimientoAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtroConsultaIdDebeAplicarseEnContentYCount() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, consultaAlpha.getId(), null, PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoAlpha.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void filtroAutorIdDebeAplicarseEnContentYCount() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, null, autorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoGamma.getId(), seguimientoAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtrosCombinadosDebenAplicarseConAnd() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                "empate alpha",
                EstadoSeguimiento.PENDIENTE,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10),
                consultaAlpha.getId(),
                autorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoAlpha.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void excluyeSeguimientoInactivo() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                "seguimiento inactivo", null, null, null, null, null, PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void excluyeConsultaArchivada() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                "seguimiento archivado", null, null, null, null, null, PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void scopeAdminDebeSerGlobal() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, null, null, PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoDelta.getId(), seguimientoGamma.getId(),
                seguimientoBeta.getId(), seguimientoAlpha.getId());
        assertEquals(4, resultado.getTotalElements());
    }

    @Test
    void scopeEstudianteDebeVerSoloSuConsultaYNotificarEstudianteTrue() {
        Page<SeguimientoResumenProjection> resultado = buscarRestringido(
                "ESTUDIANTE", estudianteA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoAlpha.getId());
        assertFalse(ids(resultado).contains(seguimientoBeta.getId()));
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void scopeAsesorDirectoPorConsultaDebeIncluirSeguimiento() {
        Page<SeguimientoResumenProjection> resultado = buscarRestringido(
                "ASESOR", asesorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(ids(resultado).contains(seguimientoDelta.getId()));
        assertTrue(ids(resultado).contains(seguimientoAlpha.getId()));
    }

    @Test
    void scopeAsesorPorEstudianteDebeIncluirConsultaDelEstudianteAsesorado() {
        Page<SeguimientoResumenProjection> resultado = buscarRestringido(
                "ASESOR", asesorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(ids(resultado).contains(seguimientoBeta.getId()));
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeVerSoloConsultasAsignadas() {
        Page<SeguimientoResumenProjection> resultado = buscarRestringido(
                "MONITOR", monitorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, seguimientoDelta.getId(), seguimientoAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeConciliadorDebeSerFailClosed() {
        Page<SeguimientoResumenProjection> resultado = buscarRestringido(
                "CONCILIADOR", 999L, PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void perfilNoSoportadoONullDebeSerFailClosed() {
        Page<SeguimientoResumenProjection> noSoportado = buscarRestringido(
                "ADMINISTRATIVO", asesorA.getId(), PageRequest.of(0, 10, sortIdDesc()));
        Page<SeguimientoResumenProjection> nulo = buscarRestringido(
                null, null, PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(noSoportado.getContent().isEmpty());
        assertEquals(0, noSoportado.getTotalElements());
        assertTrue(nulo.getContent().isEmpty());
        assertEquals(0, nulo.getTotalElements());
    }

    @Test
    void ordenEstableDebeUsarIdAscComoDesempate() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                "empate estable", null, null, null, null, null,
                PageRequest.of(0, 10, Sort.by(
                        Sort.Order.asc("fechaCreacion"),
                        Sort.Order.asc("id"))));

        assertIds(resultado, seguimientoAlpha.getId(), seguimientoBeta.getId());
        assertTrue(seguimientoAlpha.getId() < seguimientoBeta.getId());
    }

    @Test
    void projectionExponeCamposEsperados() {
        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                "empate alpha", null, null, null, null, null, PageRequest.of(0, 10, sortIdDesc()));

        SeguimientoResumenProjection projection = resultado.getContent().getFirst();

        assertEquals(seguimientoAlpha.getId(), projection.getId());
        assertNotNull(projection.getVersion());
        assertEquals("empate estable empate alpha", projection.getDescripcion());
        assertEquals(LocalDate.of(2026, 2, 1), projection.getFechaEntrega());
        assertEquals(2, projection.getDiasNotificacion());
        assertEquals(true, projection.getNotificarPartes());
        assertEquals(true, projection.getNotificarEstudiante());
        assertEquals(false, projection.getAlertaDisciplinaria());
        assertEquals(EstadoSeguimiento.PENDIENTE, projection.getEstado());
        assertEquals(categoriaAudiencia.getId(), projection.getCategoriaSeguimientoId());
        assertEquals("Audiencia", projection.getCategoriaSeguimientoNombre());
        assertEquals(consultaAlpha.getId(), projection.getConsultaId());
        assertEquals(autorA.getId(), projection.getAutorId());
        assertEquals("autor.a@example.test", projection.getAutorUsername());
        assertEquals(LocalDateTime.of(2026, 1, 10, 9, 0), projection.getFechaCreacion());
        assertNull(projection.getFechaActualizacion());
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

        Page<SeguimientoResumenProjection> resultado = buscarGlobal(
                null, null, null, null, null, null, PageRequest.of(0, 2, sortIdDesc()));

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getVersion();
            item.getDescripcion();
            item.getFechaEntrega();
            item.getDiasNotificacion();
            item.getNotificarPartes();
            item.getNotificarEstudiante();
            item.getAlertaDisciplinaria();
            item.getEstado();
            item.getCategoriaSeguimientoId();
            item.getCategoriaSeguimientoNombre();
            item.getConsultaId();
            item.getAutorId();
            item.getAutorUsername();
            item.getFechaCreacion();
            item.getFechaActualizacion();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    private Page<SeguimientoResumenProjection> buscarGlobal(
            String search,
            EstadoSeguimiento estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long consultaId,
            Long autorId,
            PageRequest pageable) {
        return seguimientoRepository.buscarResumenPaginado(
                search,
                estado,
                fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null,
                consultaId,
                autorId,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Page<SeguimientoResumenProjection> buscarRestringido(
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return seguimientoRepository.buscarResumenPaginado(
                null,
                null,
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

    private List<Long> ids(Page<SeguimientoResumenProjection> page) {
        return page.getContent().stream()
                .map(SeguimientoResumenProjection::getId)
                .toList();
    }

    private void assertIds(Page<SeguimientoResumenProjection> page, Long... expectedIds) {
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
        rolSistema.setNombre("ROL_SEGUIMIENTO_TEST");
        rolSistema.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rolSistema);

        autorA = crearUsuario("autor.a@example.test");
        autorB = crearUsuario("autor.b@example.test");

        categoriaAudiencia = crearCategoria("Audiencia");
        categoriaDocumento = crearCategoria("Documento");
    }

    private CategoriaSeguimiento crearCategoria(String nombre) {
        CategoriaSeguimiento categoria = new CategoriaSeguimiento();
        categoria.setNombre(nombre);
        categoria.setActivo(true);
        entityManager.persist(categoria);
        return categoria;
    }

    private UsuarioSistema crearUsuario(String username) {
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername(username);
        usuario.setPasswordHash("hash");
        usuario.setActivo(true);
        usuario.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        usuario.setRol(rolSistema);
        entityManager.persist(usuario);
        return usuario;
    }

    private void crearPerfiles() {
        asesorA = crearAsesor("Asesor A", "ASE-A", "A-001", "asesor.a");
        asesorB = crearAsesor("Asesor B", "ASE-B", "A-002", "asesor.b");
        estudianteA = crearEstudiante("Estudiante A", "EST-A", "E-001", "estudiante.a", asesorA);
        estudianteB = crearEstudiante("Estudiante B", "EST-B", "E-002", "estudiante.b", asesorB);
        monitorA = crearMonitor("Monitor A", "MON-A", "M-001", "monitor.a");
        monitorB = crearMonitor("Monitor B", "MON-B", "M-002", "monitor.b");
    }

    private void crearSeguimientosBase() {
        consultaAlpha = crearConsulta(crearPersona("Ana", "Alvarez", "DOC-001"),
                EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                "consulta alpha", LocalDate.of(2026, 1, 10));
        consultaBeta = crearConsulta(crearPersona("Bruno", "Bermudez", "DOC-002"),
                EstadoConsulta.ACTIVO, asesorB, estudianteA, monitorB,
                "consulta beta asesorado", LocalDate.of(2026, 1, 11));
        consultaGamma = crearConsulta(crearPersona("Carla", "Castillo", "DOC-003"),
                EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                "consulta gamma", LocalDate.of(2026, 1, 12));
        consultaDelta = crearConsulta(crearPersona("Diana", "Duarte", "DOC-004"),
                EstadoConsulta.ACTIVO, asesorA, estudianteB, monitorA,
                "consulta directa asesor", LocalDate.of(2026, 1, 13));
        Consulta consultaInactiva = crearConsulta(crearPersona("Elena", "Escalante", "DOC-005"),
                EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                "consulta inactiva", LocalDate.of(2026, 1, 14));
        Consulta consultaArchivada = crearConsulta(crearPersona("Fabio", "Fuentes", "DOC-006"),
                EstadoConsulta.ARCHIVADO, asesorA, estudianteA, monitorA,
                "consulta archivada", LocalDate.of(2026, 1, 15));

        seguimientoAlpha = crearSeguimiento(consultaAlpha, categoriaAudiencia, autorA,
                "empate estable empate alpha", EstadoSeguimiento.PENDIENTE, true, true,
                LocalDateTime.of(2026, 1, 10, 9, 0));
        seguimientoBeta = crearSeguimiento(consultaBeta, categoriaDocumento, autorB,
                "empate estable beta", EstadoSeguimiento.COMPLETADO, true, false,
                LocalDateTime.of(2026, 1, 10, 9, 0));
        seguimientoGamma = crearSeguimiento(consultaGamma, categoriaAudiencia, autorA,
                "seguimiento gamma", EstadoSeguimiento.CANCELADO, true, true,
                LocalDateTime.of(2026, 1, 11, 9, 0));
        seguimientoDelta = crearSeguimiento(consultaDelta, categoriaDocumento, autorB,
                "seguimiento directo asesor", EstadoSeguimiento.PENDIENTE, true, false,
                LocalDateTime.of(2026, 1, 12, 9, 0));
        seguimientoInactivo = crearSeguimiento(consultaInactiva, categoriaAudiencia, autorA,
                "seguimiento inactivo", EstadoSeguimiento.PENDIENTE, false, true,
                LocalDateTime.of(2026, 1, 13, 9, 0));
        seguimientoArchivado = crearSeguimiento(consultaArchivada, categoriaAudiencia, autorA,
                "seguimiento archivado", EstadoSeguimiento.PENDIENTE, true, true,
                LocalDateTime.of(2026, 1, 14, 9, 0));
    }

    private Seguimiento crearSeguimiento(
            Consulta consulta,
            CategoriaSeguimiento categoria,
            UsuarioSistema autor,
            String descripcion,
            EstadoSeguimiento estado,
            boolean activo,
            boolean notificarEstudiante,
            LocalDateTime fechaCreacion) {
        Seguimiento seguimiento = new Seguimiento();
        seguimiento.setConsulta(consulta);
        seguimiento.setCategoriaSeguimiento(categoria);
        seguimiento.setAutor(autor);
        seguimiento.setDescripcion(descripcion);
        seguimiento.setFechaEntrega(LocalDate.of(2026, 2, 1));
        seguimiento.setDiasNotificacion(2);
        seguimiento.setNotificarPartes(true);
        seguimiento.setNotificarEstudiante(notificarEstudiante);
        seguimiento.setAlertaDisciplinaria(false);
        seguimiento.setEstado(estado);
        seguimiento.setActivo(activo);
        seguimiento.setFechaCreacion(fechaCreacion);
        entityManager.persist(seguimiento);
        return seguimiento;
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
    // =========================================================================
    // BLOQUE B – SCRUM-269: buscarParaAgenda, buscarPorAutorConScope,
    //                        buscarPorFechaEntregaConScope,
    //                        buscarParaCalendarioPorRangoConScope
    // =========================================================================

    // --- buscarParaAgenda ---

    @Test
    void agendaGlobalDebeIncluirSeguimientosConFechaEntregaEnRango() {
        Seguimiento dentroRango = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "agenda dentro rango",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 15));
        Seguimiento fueraRango = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "agenda fuera rango",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 5, 1));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertTrue(ids.contains(dentroRango.getId()));
        assertFalse(ids.contains(fueraRango.getId()));
    }

    @Test
    void agendaGlobalDebeExcluirSeguimientosSinFechaEntrega() {
        Seguimiento sinFecha = crearSeguimientoConFechaEntrega(
                consultaAlpha,
                categoriaAudiencia,
                autorA,
                "agenda sin fecha",
                EstadoSeguimiento.PENDIENTE,
                LocalDate.of(2026, 3, 15));

        sinFecha.setFechaEntrega(null);

        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();

        assertFalse(ids.contains(sinFecha.getId()));
    }

    @Test
    void agendaScopeAsesorDebeVerSoloSusConsultas() {
        Seguimiento suyoAsesorA = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "agenda asesorA",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 10));
        Seguimiento ajenoAsesorB = crearSeguimientoConFechaEntrega(
                consultaGamma, categoriaAudiencia, autorB, "agenda asesorB",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 11));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                false, "ASESOR", asesorA.getId(),
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertTrue(ids.contains(suyoAsesorA.getId()));
        assertFalse(ids.contains(ajenoAsesorB.getId()));
    }

    @Test
    void agendaProjectionExponeCamposMinimos() {
        Seguimiento seg = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "Descripcion agenda test",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 5));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        var projection = resultado.stream()
                .filter(p -> p.getId().equals(seg.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(seg.getId(), projection.getId());
        assertEquals(consultaAlpha.getId(), projection.getConsultaId());
        assertEquals("Descripcion agenda test", projection.getDescripcion());
        assertEquals(EstadoSeguimiento.PENDIENTE, projection.getEstado());
        assertEquals(LocalDate.of(2026, 3, 5), projection.getFechaEntrega());
    }

    @Test
    void agendaScopeEstudianteDebeVerSoloNotificarEstudianteTrue() {
        Seguimiento visible = crearSeguimientoConFechaEntregaYNotificar(
                consultaAlpha, categoriaAudiencia, autorA, "visible estudiante",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 8), true);
        Seguimiento noVisible = crearSeguimientoConFechaEntregaYNotificar(
                consultaAlpha, categoriaAudiencia, autorA, "no visible estudiante",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 9), false);
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                false, "ESTUDIANTE", estudianteA.getId(),
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertTrue(ids.contains(visible.getId()));
        assertFalse(ids.contains(noVisible.getId()));
    }

    @Test
    void agendaRangoExcluyeHastaExclusiva() {
        LocalDate dentroRango = LocalDate.of(2026, 3, 31);
        LocalDate hastaExclusiva = LocalDate.of(2026, 4, 1); // no incluido
        Seguimiento dentro = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "fecha limite dentro",
                EstadoSeguimiento.PENDIENTE, dentroRango);
        Seguimiento fuera = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "fecha limite fuera",
                EstadoSeguimiento.PENDIENTE, hastaExclusiva);
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                hastaExclusiva,
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertTrue(ids.contains(dentro.getId()));
        assertFalse(ids.contains(fuera.getId()));
    }

    @Test
    void agendaExcluyeSeguimientoInactivo() {
        Seguimiento inactivo = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "agenda inactivo",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 10));
        inactivo.setActivo(false);
        entityManager.persist(inactivo);
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertFalse(ids.contains(inactivo.getId()));
    }

    @Test
    void agendaExcluyeSeguimientoDeConsultaArchivada() {
        Consulta consultaArchivada = crearConsulta(
                crearPersona("Agenda", "Archivada", "DOC-AGENDA-ARCH"),
                EstadoConsulta.ARCHIVADO,
                asesorA,
                estudianteA,
                monitorA,
                "consulta archivada agenda",
                LocalDate.of(2026, 3, 1));

        Seguimiento archivado = crearSeguimientoConFechaEntrega(
                consultaArchivada, categoriaAudiencia, autorA, "agenda consulta archivada",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 10));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertFalse(ids.contains(archivado.getId()));
    }

    @Test
    void agendaScopeAsesorPorEstudianteDebeIncluirConsulta() {
        // consultaBeta tiene asesor directo asesorB y estudianteA, cuyo asesor es asesorA.
        Seguimiento indirecto = crearSeguimientoConFechaEntrega(
                consultaBeta, categoriaAudiencia, autorB, "agenda asesor indirecto",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 10));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                false, "ASESOR", asesorA.getId(),
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertTrue(ids.contains(indirecto.getId()));
    }

    @Test
    void agendaScopeMonitorDebeVerUnicamenteConsultasAsignadas() {
        Seguimiento monitorSeg = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "agenda monitor",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 10));
        Seguimiento ajenoSeg = crearSeguimientoConFechaEntrega(
                consultaGamma, categoriaAudiencia, autorB, "agenda monitor ajeno",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 11));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                false, "MONITOR", monitorA.getId(),
                EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(SeguimientoAgendaProjection::getId)
                .toList();
        assertTrue(ids.contains(monitorSeg.getId()));
        assertFalse(ids.contains(ajenoSeg.getId()));
    }

    @Test
    void agendaScopeConciliadorDebeSerFailClosed() {
        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                false, "CONCILIADOR", 999L,
                EstadoConsulta.ARCHIVADO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void agendaPerfilNoSoportadoDebeSerFailClosed() {
        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                false, "ADMINISTRATIVO", 999L,
                EstadoConsulta.ARCHIVADO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void agendaDebeOrdenarPorFechaEntregaAscEIdAsc() {
        Seguimiento seg1 = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "orden 1",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 15));
        Seguimiento seg3 = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "orden 3",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 20));
        Seguimiento seg2 = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "orden 2",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 15));

        // forzar ids si es necesario, pero el orden de persistencia ya garantiza que seg1.id < seg2.id
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        assertEquals(3, resultado.size());
        assertEquals(seg1.getId(), resultado.get(0).getId());
        assertEquals(seg2.getId(), resultado.get(1).getId());
        assertEquals(seg3.getId(), resultado.get(2).getId());
    }

    @Test
    void accesoAGettersDeAgendaProjectionNoDebeGenerarNMasUno() {
        crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "n+1 test 1",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 10));
        crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "n+1 test 2",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 3, 11));

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        var resultado = seguimientoRepository.buscarParaAgenda(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 1),
                true, null, null,
                EstadoConsulta.ARCHIVADO);

        resultado.forEach(item -> {
            item.getId();
            item.getConsultaId();
            item.getDescripcion();
            item.getEstado();
            item.getFechaEntrega();
        });

        assertEquals(1, statistics.getPrepareStatementCount());
    }


    // --- buscarPorAutorConScope ---

    @Test
    void buscarPorAutorConScopeGlobalRetornaTodasLasConsultas() {
        var resultado = seguimientoRepository.buscarPorAutorConScope(
                autorA.getId(), true, null, null, EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream().map(Seguimiento::getId).toList();
        assertTrue(ids.contains(seguimientoAlpha.getId()));
        assertTrue(ids.contains(seguimientoGamma.getId()));
        assertFalse(ids.contains(seguimientoBeta.getId())); // autorB
    }

    @Test
    void buscarPorAutorConScopeAsesorDebeExcluirConsultasAjenas() {
        var resultado = seguimientoRepository.buscarPorAutorConScope(
                autorA.getId(), false, "ASESOR", asesorB.getId(), EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream()
                .map(Seguimiento::getId)
                .toList();

        assertFalse(ids.contains(seguimientoAlpha.getId()));
        assertTrue(ids.contains(seguimientoGamma.getId()));
    }

    @Test
    void buscarPorAutorConScopeAsesorRetornaConsultasPropias() {
        var resultado = seguimientoRepository.buscarPorAutorConScope(
                autorA.getId(), false, "ASESOR", asesorA.getId(), EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream().map(Seguimiento::getId).toList();
        assertTrue(ids.contains(seguimientoAlpha.getId()));
    }

    // --- buscarPorFechaEntregaConScope ---

    @Test
    void buscarPorFechaEntregaConScopeGlobalRetornaCoincidentes() {
        LocalDate fecha = LocalDate.of(2026, 4, 20);
        Seguimiento seg = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "fecha entrega exacta",
                EstadoSeguimiento.PENDIENTE, fecha);
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarPorFechaEntregaConScope(
                fecha, true, null, null, EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream().map(Seguimiento::getId).toList();
        assertTrue(ids.contains(seg.getId()));
    }

    @Test
    void buscarPorFechaEntregaConScopeDebeExcluirFechaDistinta() {
        LocalDate fecha = LocalDate.of(2026, 4, 20);
        LocalDate otraFecha = LocalDate.of(2026, 4, 21);
        Seguimiento segOtra = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "fecha entrega otra",
                EstadoSeguimiento.PENDIENTE, otraFecha);
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarPorFechaEntregaConScope(
                fecha, true, null, null, EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream().map(Seguimiento::getId).toList();
        assertFalse(ids.contains(segOtra.getId()));
    }

    // --- buscarParaCalendarioPorRangoConScope ---

    @Test
    void buscarCalendarioRangoGlobalRetornaSeguimientosEnRango() {
        Seguimiento dentro = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "calendario dentro",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 5, 10));
        Seguimiento fuera = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "calendario fuera",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 7, 1));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaCalendarioPorRangoConScope(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                true, null, null, EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream().map(Seguimiento::getId).toList();
        assertTrue(ids.contains(dentro.getId()));
        assertFalse(ids.contains(fuera.getId()));
    }

    @Test
    void buscarCalendarioRangoScopeMonitorDebeVerSoloSusConsultas() {
        Seguimiento suyo = crearSeguimientoConFechaEntrega(
                consultaAlpha, categoriaAudiencia, autorA, "calendario monitor suyo",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 5, 15));
        Seguimiento ajeno = crearSeguimientoConFechaEntrega(
                consultaGamma, categoriaAudiencia, autorB, "calendario monitor ajeno",
                EstadoSeguimiento.PENDIENTE, LocalDate.of(2026, 5, 16));
        entityManager.flush();
        entityManager.clear();

        var resultado = seguimientoRepository.buscarParaCalendarioPorRangoConScope(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                false, "MONITOR", monitorA.getId(), EstadoConsulta.ARCHIVADO);

        var ids = resultado.stream().map(Seguimiento::getId).toList();
        assertTrue(ids.contains(suyo.getId()));
        assertFalse(ids.contains(ajeno.getId()));
    }

    // =========================================================================
    // Helpers de creación para Bloque B
    // =========================================================================

    private Seguimiento crearSeguimientoConFechaEntrega(
            Consulta consulta,
            CategoriaSeguimiento categoria,
            UsuarioSistema autor,
            String descripcion,
            EstadoSeguimiento estado,
            LocalDate fechaEntrega) {
        Seguimiento s = new Seguimiento();
        s.setConsulta(consulta);
        s.setCategoriaSeguimiento(categoria);
        s.setAutor(autor);
        s.setDescripcion(descripcion);
        s.setEstado(estado);
        s.setFechaEntrega(fechaEntrega);
        s.setDiasNotificacion(1);
        s.setNotificarPartes(false);
        s.setNotificarEstudiante(false);
        s.setAlertaDisciplinaria(false);
        s.setActivo(true);
        s.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 8, 0));
        entityManager.persist(s);
        return s;
    }

    private Seguimiento crearSeguimientoConFechaEntregaYNotificar(
            Consulta consulta,
            CategoriaSeguimiento categoria,
            UsuarioSistema autor,
            String descripcion,
            EstadoSeguimiento estado,
            LocalDate fechaEntrega,
            boolean notificarEstudiante) {
        Seguimiento s = crearSeguimientoConFechaEntrega(
                consulta, categoria, autor, descripcion, estado, fechaEntrega);
        s.setNotificarEstudiante(notificarEstudiante);
        return s;
    }
}
