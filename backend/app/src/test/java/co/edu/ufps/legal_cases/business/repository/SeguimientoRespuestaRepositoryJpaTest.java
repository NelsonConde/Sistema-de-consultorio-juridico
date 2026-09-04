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
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.EstadoRespuestaSeguimiento;
import co.edu.ufps.legal_cases.business.model.seguimiento.respuesta.SeguimientoRespuesta;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaPendienteProjection;
import co.edu.ufps.legal_cases.business.repository.seguimiento.respuesta.SeguimientoRespuestaRepository;
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
class SeguimientoRespuestaRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private SeguimientoRespuestaRepository seguimientoRespuestaRepository;

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
    private UsuarioSistema revisor;
    private CategoriaSeguimiento categoriaAudiencia;
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
    private SeguimientoRespuesta respuestaAlpha;
    private SeguimientoRespuesta respuestaBeta;
    private SeguimientoRespuesta respuestaGamma;
    private SeguimientoRespuesta respuestaDelta;
    private SeguimientoRespuesta respuestaAprobada;
    private SeguimientoRespuesta respuestaInactiva;
    private SeguimientoRespuesta respuestaSeguimientoInactivo;
    private SeguimientoRespuesta respuestaArchivada;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearRespuestasBase();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void soloDevuelvePendientesValidas() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaDelta.getId(), respuestaGamma.getId(),
                respuestaBeta.getId(), respuestaAlpha.getId());
        assertEquals(4, resultado.getTotalElements());
        assertFalse(ids(resultado).contains(respuestaAprobada.getId()));
    }

    @Test
    void excluyeRespuestaInactiva() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                "respuesta inactiva", PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void excluyeSeguimientoInactivo() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                "respuesta seguimiento inactivo", PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void excluyeConsultaArchivada() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                "respuesta archivada", PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void scopeAdministradorDebeVerTodosLosPendientesValidos() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(PageRequest.of(0, 10, sortIdDesc()));

        assertEquals(4, resultado.getTotalElements());
        assertIds(resultado, respuestaDelta.getId(), respuestaGamma.getId(),
                respuestaBeta.getId(), respuestaAlpha.getId());
    }

    @Test
    void scopeAsesorDebeVerSoloPendientesDentroDeSuAlcance() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarRestringido(
                "ASESOR", asesorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaDelta.getId(), respuestaBeta.getId(), respuestaAlpha.getId());
        assertFalse(ids(resultado).contains(respuestaGamma.getId()));
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeVerSoloPendientesDentroDeSuAlcance() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarRestringido(
                "MONITOR", monitorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaDelta.getId(), respuestaAlpha.getId());
        assertFalse(ids(resultado).contains(respuestaBeta.getId()));
        assertFalse(ids(resultado).contains(respuestaGamma.getId()));
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void perfilesNoAutorizadosONoSoportadosDebenSerFailClosed() {
        Page<SeguimientoRespuestaPendienteProjection> estudiante = buscarRestringido(
                "ESTUDIANTE", estudianteA.getId(), PageRequest.of(0, 10, sortIdDesc()));
        Page<SeguimientoRespuestaPendienteProjection> conciliador = buscarRestringido(
                "CONCILIADOR", 999L, PageRequest.of(0, 10, sortIdDesc()));
        Page<SeguimientoRespuestaPendienteProjection> noSoportado = buscarRestringido(
                "ADMINISTRATIVO", 999L, PageRequest.of(0, 10, sortIdDesc()));
        Page<SeguimientoRespuestaPendienteProjection> nulo = buscarRestringido(
                null, null, PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(estudiante.getContent().isEmpty());
        assertTrue(conciliador.getContent().isEmpty());
        assertTrue(noSoportado.getContent().isEmpty());
        assertTrue(nulo.getContent().isEmpty());
    }

    @Test
    void scopeAsesorNuncaIncluyeConsultaFueraDeAlcance() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarRestringido(
                "ASESOR", asesorA.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertFalse(ids(resultado).contains(respuestaGamma.getId()));
        assertTrue(ids(resultado).contains(respuestaBeta.getId()));
    }

    @Test
    void paginacionDebeRespetarSize() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(4, resultado.getTotalElements());
    }

    @Test
    void segundaPaginaDebeRetornarContenidoCorrecto() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(PageRequest.of(1, 2, sortIdDesc()));

        assertIds(resultado, respuestaBeta.getId(), respuestaAlpha.getId());
        assertEquals(4, resultado.getTotalElements());
    }

    @Test
    void totalElementsYTotalPagesDebenCorresponderSoloARegistrosVisibles() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarRestringido(
                "MONITOR", monitorA.getId(), PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(2, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());
    }

    @Test
    void busquedaDebeCoincidirPorContenido() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                "gamma monitor", PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaGamma.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void busquedaDebeCoincidirPorNombreDelEstudiante() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                "Estudiante A", PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaBeta.getId(), respuestaAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void busquedaCombinadaConScopeDebeExcluirCoincidenciasFueraDeAlcance() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarConScopeYSearch(
                "Estudiante A", "MONITOR", monitorB.getId(), PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaBeta.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void fechaDesdeDebeAplicarseSobreFechaCreacion() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                null,
                LocalDate.of(2026, 1, 11),
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaDelta.getId(), respuestaGamma.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void fechaHastaDebeSerInclusivaADiaMedianteLimiteExclusivo() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                null,
                null,
                LocalDate.of(2026, 1, 10),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaBeta.getId(), respuestaAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void rangoCombinadoDebeFiltrarFechaCreacion() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                null,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 11),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, respuestaGamma.getId(), respuestaBeta.getId(), respuestaAlpha.getId());
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void ordenFechaCreacionAscDescDebeRespetarse() {
        Page<SeguimientoRespuestaPendienteProjection> asc = buscarGlobal(
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("fechaCreacion"), Sort.Order.asc("id"))));
        Page<SeguimientoRespuestaPendienteProjection> desc = buscarGlobal(
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.asc("id"))));

        assertIds(asc, respuestaAlpha.getId(), respuestaBeta.getId(),
                respuestaGamma.getId(), respuestaDelta.getId());
        assertIds(desc, respuestaDelta.getId(), respuestaGamma.getId(),
                respuestaAlpha.getId(), respuestaBeta.getId());
    }

    @Test
    void ordenPorEstudianteNombreDebeSerPermitido() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("estudiante.nombre"), Sort.Order.asc("id"))));

        assertEquals(estudianteA.getNombre(), resultado.getContent().get(0).getEstudianteNombre());
        assertEquals(estudianteA.getNombre(), resultado.getContent().get(1).getEstudianteNombre());
        assertEquals(estudianteB.getNombre(), resultado.getContent().get(2).getEstudianteNombre());
        assertEquals(estudianteB.getNombre(), resultado.getContent().get(3).getEstudianteNombre());
    }

    @Test
    void ordenEstableDebeUsarIdAscComoDesempate() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("fechaCreacion"), Sort.Order.asc("id"))));

        assertIds(resultado, respuestaAlpha.getId(), respuestaBeta.getId(),
                respuestaGamma.getId(), respuestaDelta.getId());
        assertTrue(respuestaAlpha.getId() < respuestaBeta.getId());
    }

    @Test
    void projectionExponeCamposEsperados() {
        Page<SeguimientoRespuestaPendienteProjection> resultado = buscarGlobal(
                "alpha contenido", PageRequest.of(0, 10, sortIdDesc()));

        SeguimientoRespuestaPendienteProjection projection = resultado.getContent().get(0);

        assertEquals(respuestaAlpha.getId(), projection.getId());
        assertNotNull(projection.getVersion());
        assertEquals(seguimientoAlpha.getId(), projection.getSeguimientoId());
        assertEquals(consultaAlpha.getId(), projection.getConsultaId());
        assertEquals(estudianteA.getId(), projection.getEstudianteId());
        assertEquals("Estudiante A", projection.getEstudianteNombre());
        assertEquals("alpha contenido pendiente", projection.getContenido());
        assertEquals(EstadoRespuestaSeguimiento.PENDIENTE, projection.getEstado());
        assertEquals(false, projection.getFueraPlazo());
        assertNull(projection.getObservacionRevision());
        assertNull(projection.getRevisadoPorId());
        assertNull(projection.getRevisadoPorUsername());
        assertEquals(true, projection.getActivo());
        assertEquals(LocalDateTime.of(2026, 1, 10, 9, 0), projection.getFechaCreacion());
        assertNull(projection.getFechaActualizacion());
        assertNull(projection.getFechaDecision());
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

        Page<SeguimientoRespuestaPendienteProjection> resultado =
                buscarGlobal(PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, statistics.getPrepareStatementCount());

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getVersion();
            item.getSeguimientoId();
            item.getConsultaId();
            item.getEstudianteId();
            item.getEstudianteNombre();
            item.getContenido();
            item.getEstado();
            item.getFueraPlazo();
            item.getObservacionRevision();
            item.getRevisadoPorId();
            item.getRevisadoPorUsername();
            item.getActivo();
            item.getFechaCreacion();
            item.getFechaActualizacion();
            item.getFechaDecision();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    private Page<SeguimientoRespuestaPendienteProjection> buscarGlobal(PageRequest pageable) {
        return buscarGlobal(null, null, null, pageable);
    }

    private Page<SeguimientoRespuestaPendienteProjection> buscarGlobal(String search, PageRequest pageable) {
        return buscarGlobal(search, null, null, pageable);
    }

    private Page<SeguimientoRespuestaPendienteProjection> buscarGlobal(
            String search,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            PageRequest pageable) {
        return seguimientoRespuestaRepository.buscarPendientesPaginado(
                search,
                EstadoRespuestaSeguimiento.PENDIENTE,
                fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Page<SeguimientoRespuestaPendienteProjection> buscarRestringido(
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return buscarConScopeYSearch(null, tipoPerfil, perfilId, pageable);
    }

    private Page<SeguimientoRespuestaPendienteProjection> buscarConScopeYSearch(
            String search,
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return seguimientoRespuestaRepository.buscarPendientesPaginado(
                search,
                EstadoRespuestaSeguimiento.PENDIENTE,
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

    private List<Long> ids(Page<SeguimientoRespuestaPendienteProjection> page) {
        return page.getContent().stream()
                .map(SeguimientoRespuestaPendienteProjection::getId)
                .toList();
    }

    private void assertIds(Page<SeguimientoRespuestaPendienteProjection> page, Long... expectedIds) {
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
        rolSistema.setNombre("ROL_RESPUESTA_TEST");
        rolSistema.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rolSistema);

        autorA = crearUsuario("autor.respuesta.a@example.test");
        autorB = crearUsuario("autor.respuesta.b@example.test");
        revisor = crearUsuario("revisor.respuesta@example.test");

        categoriaAudiencia = new CategoriaSeguimiento();
        categoriaAudiencia.setNombre("Audiencia");
        categoriaAudiencia.setActivo(true);
        entityManager.persist(categoriaAudiencia);
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
        asesorA = crearAsesor("Asesor A", "ASE-A", "A-001", "asesor.respuesta.a");
        asesorB = crearAsesor("Asesor B", "ASE-B", "A-002", "asesor.respuesta.b");
        estudianteA = crearEstudiante("Estudiante A", "EST-A", "E-001", "estudiante.respuesta.a", asesorA);
        estudianteB = crearEstudiante("Estudiante B", "EST-B", "E-002", "estudiante.respuesta.b", asesorB);
        monitorA = crearMonitor("Monitor A", "MON-A", "M-001", "monitor.respuesta.a");
        monitorB = crearMonitor("Monitor B", "MON-B", "M-002", "monitor.respuesta.b");
    }

    private void crearRespuestasBase() {
        consultaAlpha = crearConsulta(crearPersona("Ana", "Alvarez", "DOC-RESP-001"),
                EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                "consulta alpha respuesta", LocalDate.of(2026, 1, 10));
        consultaBeta = crearConsulta(crearPersona("Bruno", "Bermudez", "DOC-RESP-002"),
                EstadoConsulta.ACTIVO, asesorB, estudianteA, monitorB,
                "consulta beta respuesta", LocalDate.of(2026, 1, 11));
        consultaGamma = crearConsulta(crearPersona("Carla", "Castillo", "DOC-RESP-003"),
                EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                "consulta gamma respuesta", LocalDate.of(2026, 1, 12));
        consultaDelta = crearConsulta(crearPersona("Diana", "Duarte", "DOC-RESP-004"),
                EstadoConsulta.ACTIVO, asesorA, estudianteB, monitorA,
                "consulta delta respuesta", LocalDate.of(2026, 1, 13));
        Consulta consultaArchivada = crearConsulta(crearPersona("Elena", "Escalante", "DOC-RESP-005"),
                EstadoConsulta.ARCHIVADO, asesorA, estudianteA, monitorA,
                "consulta archivada respuesta", LocalDate.of(2026, 1, 14));

        seguimientoAlpha = crearSeguimiento(consultaAlpha, autorA, "seguimiento alpha respuesta", true);
        seguimientoBeta = crearSeguimiento(consultaBeta, autorB, "seguimiento beta respuesta", true);
        seguimientoGamma = crearSeguimiento(consultaGamma, autorA, "seguimiento gamma respuesta", true);
        seguimientoDelta = crearSeguimiento(consultaDelta, autorB, "seguimiento delta respuesta", true);
        seguimientoInactivo = crearSeguimiento(consultaAlpha, autorA, "seguimiento inactivo respuesta", false);
        seguimientoArchivado = crearSeguimiento(consultaArchivada, autorA, "seguimiento archivado respuesta", true);

        respuestaAlpha = crearRespuesta(seguimientoAlpha, estudianteA, "alpha contenido pendiente",
                EstadoRespuestaSeguimiento.PENDIENTE, true, LocalDateTime.of(2026, 1, 10, 9, 0));
        respuestaBeta = crearRespuesta(seguimientoBeta, estudianteA, "beta contenido pendiente",
                EstadoRespuestaSeguimiento.PENDIENTE, true, LocalDateTime.of(2026, 1, 10, 9, 0));
        respuestaGamma = crearRespuesta(seguimientoGamma, estudianteB, "gamma monitor contenido pendiente",
                EstadoRespuestaSeguimiento.PENDIENTE, true, LocalDateTime.of(2026, 1, 11, 9, 0));
        respuestaDelta = crearRespuesta(seguimientoDelta, estudianteB, "delta contenido pendiente",
                EstadoRespuestaSeguimiento.PENDIENTE, true, LocalDateTime.of(2026, 1, 12, 9, 0));
        respuestaAprobada = crearRespuesta(seguimientoAlpha, estudianteA, "respuesta aprobada",
                EstadoRespuestaSeguimiento.APROBADA, true, LocalDateTime.of(2026, 1, 13, 9, 0));
        respuestaAprobada.setRevisadoPor(revisor);
        respuestaAprobada.setFechaDecision(LocalDateTime.of(2026, 1, 14, 9, 0));
        respuestaInactiva = crearRespuesta(seguimientoAlpha, estudianteA, "respuesta inactiva",
                EstadoRespuestaSeguimiento.PENDIENTE, false, LocalDateTime.of(2026, 1, 14, 9, 0));
        respuestaSeguimientoInactivo = crearRespuesta(seguimientoInactivo, estudianteA,
                "respuesta seguimiento inactivo", EstadoRespuestaSeguimiento.PENDIENTE,
                true, LocalDateTime.of(2026, 1, 15, 9, 0));
        respuestaArchivada = crearRespuesta(seguimientoArchivado, estudianteA, "respuesta archivada",
                EstadoRespuestaSeguimiento.PENDIENTE, true, LocalDateTime.of(2026, 1, 16, 9, 0));
    }

    private Seguimiento crearSeguimiento(
            Consulta consulta,
            UsuarioSistema autor,
            String descripcion,
            boolean activo) {
        Seguimiento seguimiento = new Seguimiento();
        seguimiento.setConsulta(consulta);
        seguimiento.setCategoriaSeguimiento(categoriaAudiencia);
        seguimiento.setAutor(autor);
        seguimiento.setDescripcion(descripcion);
        seguimiento.setFechaEntrega(LocalDate.of(2026, 2, 1));
        seguimiento.setDiasNotificacion(2);
        seguimiento.setNotificarPartes(true);
        seguimiento.setNotificarEstudiante(true);
        seguimiento.setAlertaDisciplinaria(false);
        seguimiento.setEstado(EstadoSeguimiento.PENDIENTE);
        seguimiento.setActivo(activo);
        seguimiento.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 8, 0));
        entityManager.persist(seguimiento);
        return seguimiento;
    }

    private SeguimientoRespuesta crearRespuesta(
            Seguimiento seguimiento,
            Estudiante estudiante,
            String contenido,
            EstadoRespuestaSeguimiento estado,
            boolean activo,
            LocalDateTime fechaCreacion) {
        SeguimientoRespuesta respuesta = new SeguimientoRespuesta();
        respuesta.setSeguimiento(seguimiento);
        respuesta.setEstudiante(estudiante);
        respuesta.setContenido(contenido);
        respuesta.setEstado(estado);
        respuesta.setFueraPlazo(false);
        respuesta.setActivo(activo);
        respuesta.setFechaCreacion(fechaCreacion);
        entityManager.persist(respuesta);
        return respuesta;
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
}
