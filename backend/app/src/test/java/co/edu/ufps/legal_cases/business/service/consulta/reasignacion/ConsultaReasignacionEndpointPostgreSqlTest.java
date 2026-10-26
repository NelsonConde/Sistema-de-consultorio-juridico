package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_RESPONSABLES_CONSULTA;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaReasignacionDTO;
import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.service.consulta.ConsultaService;
import co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionHistorialWriter.RegistroReasignacion;
import co.edu.ufps.legal_cases.common.exception.ConcurrenciaOptimistaException;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ConsultaReasignacionEndpointPostgreSqlTest.HistorialControlConfiguration.class)
class ConsultaReasignacionEndpointPostgreSqlTest
        extends PostgreSqlIntegrationTest {

    private static final String ESQUEMA = "\"DB_consultorioJuridico\"";
    private static final String HISTORIAL = ESQUEMA + ".consulta_reasignacion_historial";
    private static final Path MIGRACION = Path.of(
            "db", "migration",
            "V20261011_01__create_consulta_reasignacion_historial.sql");
    private static final Pattern TOKEN_JSON = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ConsultaService consultaService;

    @Autowired
    private HistorialControlado historialControlado;

    private TransactionTemplate transactionTemplate;
    private ExecutorService executor;
    private Escenario escenario;

    @BeforeEach
    void prepararEscenario() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        executor = Executors.newFixedThreadPool(2);
        historialControlado.modoNormal();
        SecurityContextHolder.clearContext();

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + HISTORIAL + " CASCADE");
        ejecutarMigracion();
        escenario = transactionTemplate.execute(estado -> crearEscenario());
        assertNotNull(escenario);
    }

    @AfterEach
    void limpiarEscenario() {
        historialControlado.modoNormal();
        SecurityContextHolder.clearContext();
        executor.shutdownNow();
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + HISTORIAL + " CASCADE; "
                + "DROP FUNCTION IF EXISTS " + ESQUEMA
                + ".validar_responsables_reasignacion_consulta(); "
                + "DROP FUNCTION IF EXISTS " + ESQUEMA
                + ".prevenir_modificacion_historial_reasignacion();");
    }

    @ParameterizedTest
    @EnumSource(TipoResponsableConsulta.class)
    void reasignaExactamenteUnResponsableYRegistraUnEvento(
            TipoResponsableConsulta tipo) throws Exception {

        Consulta consulta = crearConsultaCompatible(tipo, EstadoConsulta.ACTIVO);
        EstadoPersistido antes = cargarEstado(consulta.getId());
        Long anterior = responsableActual(antes, tipo);
        Long nuevo = responsableNuevo(tipo);
        String motivo = "Cambio operativo de " + tipo.name().toLowerCase();

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                tipo, nuevo, motivo, antes.version())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(antes.version() + 1))
                .andExpect(jsonPath(campoJson(tipo)).value(nuevo));

        EstadoPersistido despues = cargarEstado(consulta.getId());
        assertEquals(antes.version() + 1, despues.version());
        assertEquals(nuevo, responsableActual(despues, tipo));
        assertCamposNoInvolucrados(antes, despues, tipo);

        Evento evento = cargarUnicoEvento(consulta.getId());
        assertEquals(tipo.name(), evento.tipo());
        assertEquals(anterior, evento.anteriorId());
        assertEquals(nuevo, evento.nuevoId());
        assertEquals(motivo, evento.motivo());
        assertEquals(escenario.adminUsuarioId(), evento.actorId());
        assertNotNull(evento.ocurridoEn());
        assertEquals(1L, contarEventos(consulta.getId()));
    }

    @Test
    void conservaEventosDeReasignacionesSucesivas() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        Long version = consulta.getVersion();

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Primer cambio", version)
                .andExpect(status().isOk());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorAlternoId(),
                "Segundo cambio", version + 1)
                .andExpect(status().isOk());

        EstadoPersistido persistido = cargarEstado(consulta.getId());
        assertEquals(version + 2, persistido.version());
        assertEquals(escenario.monitorAlternoId(), persistido.monitorId());
        assertEquals(2L, contarEventos(consulta.getId()));

        List<Long> anteriores = jdbcTemplate.queryForList(
                "SELECT responsable_anterior_id FROM " + HISTORIAL
                        + " WHERE consulta_id = ? ORDER BY id",
                Long.class, consulta.getId());
        assertEquals(List.of(
                escenario.monitorActualId(), escenario.monitorNuevoId()),
                anteriores);
    }

    @Test
    void consultaHistorialPaginadoConNombresYOrdenEstable() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        Long version = consulta.getVersion();

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Primer cambio", version)
                .andExpect(status().isOk());
        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorAlternoId(),
                "Segundo cambio", version + 1)
                .andExpect(status().isOk());

        String monitorNuevo = cargarNombreMonitor(escenario.monitorNuevoId());
        String monitorAlterno = cargarNombreMonitor(escenario.monitorAlternoId());
        String actor = jdbcTemplate.queryForObject(
                "SELECT nombre FROM " + ESQUEMA
                        + ".administrativo WHERE usuario_sistema_id = ?",
                String.class,
                escenario.adminUsuarioId());

        ejecutarGetHistorial(escenario.adminUsername(), consulta.getId(), 1, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].tipoResponsable").value("MONITOR"))
                .andExpect(jsonPath("$.content[0].responsableAnteriorNombre")
                        .value(monitorNuevo))
                .andExpect(jsonPath("$.content[0].responsableNuevoNombre")
                        .value(monitorAlterno))
                .andExpect(jsonPath("$.content[0].motivo").value("Segundo cambio"))
                .andExpect(jsonPath("$.content[0].actorNombre").value(actor))
                .andExpect(jsonPath("$.content[0].ocurridoEn").isString())
                .andExpect(jsonPath("$.content[0].consultaId").doesNotExist())
                .andExpect(jsonPath("$.content[0].responsableAnteriorId").doesNotExist())
                .andExpect(jsonPath("$.content[0].responsableNuevoId").doesNotExist())
                .andExpect(jsonPath("$.content[0].actorUsuarioId").doesNotExist());

        ejecutarGetHistorial(escenario.adminUsername(), consulta.getId(), 2, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].motivo").value("Primer cambio"));
    }

    @Test
    void historialVacioConservaMetadatosDePaginacion() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);

        ejecutarGetHistorial(escenario.adminUsername(), consulta.getId(), 1, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void historialRespetaAutenticacionAlcanceYPaginacionValida() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);

        mockMvc.perform(get(
                        "/api/consultas/{id}/responsables/historial",
                        consulta.getId()))
                .andExpect(status().isUnauthorized());

        ejecutarGetHistorial(
                escenario.monitorSinAlcanceUsername(), consulta.getId(), 1, 10)
                .andExpect(status().isForbidden());

        ejecutarGetHistorial(escenario.adminUsername(), consulta.getId(), 0, 10)
                .andExpect(status().isBadRequest());

        ejecutarGetHistorial(escenario.adminUsername(), consulta.getId(), 1, 51)
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConsulta.class, names = {
            "PENDIENTE", "ACTIVO", "EN_PROCESO", "URGENTE"
    })
    void permiteReasignarEnTodosLosEstadosOperativos(
            EstadoConsulta estado) throws Exception {

        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, estado);

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Cambio en estado operativo", consulta.getVersion())
                .andExpect(status().isOk());

        assertEquals(1L, contarEventos(consulta.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConsulta.class, names = {"CERRADO", "ARCHIVADO"})
    void rechazaEstadosNoOperativosSinModificarDatos(
            EstadoConsulta estado) throws Exception {

        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, estado);
        EstadoPersistido antes = cargarEstado(consulta.getId());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Cambio no permitido", antes.version())
                .andExpect(status().isBadRequest());

        assertEquals(antes, cargarEstado(consulta.getId()));
        assertEquals(0L, contarEventos(consulta.getId()));
    }

    @Test
    void aplicaAutenticacionPermisoYAlcanceReal() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        Long version = consulta.getVersion();
        Credenciales csrfSinUsuario = credenciales(null);

        patchConCredenciales(csrfSinUsuario, consulta.getId(), solicitudJson(
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Sin autenticación", version))
                .andExpect(status().isUnauthorized());

        ejecutarPatch(escenario.sinPermisoUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Sin permiso", version)
                .andExpect(status().isForbidden());

        ejecutarPatch(escenario.monitorSinAlcanceUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Sin alcance", version)
                .andExpect(status().isForbidden());

        assertEquals(cargarId("monitor_id", consulta.getId()),
                escenario.monitorActualId());
        assertEquals(0L, contarEventos(consulta.getId()));
    }

    @Test
    void actualizaLosAccesosDerivadosSinAmpliarElAlcance() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);

        ejecutarGet(escenario.monitorActualUsername(), consulta.getId())
                .andExpect(status().isOk());
        ejecutarGet(escenario.monitorNuevoUsername(), consulta.getId())
                .andExpect(status().isForbidden());
        ejecutarGet(escenario.monitorSinAlcanceUsername(), consulta.getId())
                .andExpect(status().isForbidden());

        ejecutarPatch(escenario.monitorActualUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Transferencia de alcance", consulta.getVersion())
                .andExpect(status().isOk());

        ejecutarGet(escenario.monitorActualUsername(), consulta.getId())
                .andExpect(status().isForbidden());
        ejecutarGet(escenario.monitorNuevoUsername(), consulta.getId())
                .andExpect(status().isOk());
        ejecutarGet(escenario.monitorSinAlcanceUsername(), consulta.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void rechazaResponsablesInexistentesInactivosEIncompatibles()
            throws Exception {

        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        EstadoPersistido antes = cargarEstado(consulta.getId());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, Long.MAX_VALUE,
                "Responsable inexistente", antes.version())
                .andExpect(status().isBadRequest());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorInactivoId(),
                "Responsable inactivo", antes.version())
                .andExpect(status().isBadRequest());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.ASESOR, escenario.asesorOtraAreaId(),
                "Asesor incompatible", antes.version())
                .andExpect(status().isBadRequest());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.ESTUDIANTE,
                escenario.estudianteIncompatibleId(),
                "Estudiante incompatible", antes.version())
                .andExpect(status().isBadRequest());

        assertEquals(antes, cargarEstado(consulta.getId()));
        assertEquals(0L, contarEventos(consulta.getId()));
    }

    @Test
    void validaMotivoVersionYResponsableActualEnElContratoHttp()
            throws Exception {

        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        EstadoPersistido antes = cargarEstado(consulta.getId());
        Credenciales credenciales = credenciales(escenario.adminUsername());

        patchConCredenciales(credenciales, consulta.getId(), """
                {"tipoResponsable":"MONITOR","nuevoResponsableId":%d,"version":%d}
                """.formatted(escenario.monitorNuevoId(), antes.version()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.motivo").exists());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "   ", antes.version())
                .andExpect(status().isBadRequest());

        patchConCredenciales(credenciales, consulta.getId(), """
                {"tipoResponsable":"MONITOR","nuevoResponsableId":%d,
                 "motivo":{},"version":%d}
                """.formatted(escenario.monitorNuevoId(), antes.version()))
                .andExpect(status().isBadRequest());

        patchConCredenciales(credenciales, consulta.getId(), """
                {"tipoResponsable":"MONITOR","nuevoResponsableId":%d,
                 "motivo":"Sin versión"}
                """.formatted(escenario.monitorNuevoId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.version").exists());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Versión negativa", -1L)
                .andExpect(status().isBadRequest());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Versión desactualizada", antes.version() + 1)
                .andExpect(status().isConflict());

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorActualId(),
                "Mismo responsable", antes.version())
                .andExpect(status().isBadRequest());

        assertEquals(antes, cargarEstado(consulta.getId()));
        assertEquals(0L, contarEventos(consulta.getId()));
    }

    @Test
    void revierteConsultaYVersionAnteFalloRealDelHistorial() throws Exception {
        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        EstadoPersistido antes = cargarEstado(consulta.getId());
        historialControlado.fallarConActorInexistente();

        ejecutarPatch(escenario.adminUsername(), consulta.getId(),
                TipoResponsableConsulta.MONITOR, escenario.monitorNuevoId(),
                "Debe revertirse", antes.version())
                .andExpect(status().isInternalServerError());

        historialControlado.modoNormal();
        assertEquals(antes, cargarEstado(consulta.getId()));
        assertEquals(0L, contarEventos(consulta.getId()));
    }

    @Test
    void dosReasignacionesConcurrentesNoProducenActualizacionesPerdidas()
            throws Exception {

        Consulta consulta = crearConsultaCompatible(
                TipoResponsableConsulta.MONITOR, EstadoConsulta.ACTIVO);
        Long version = consulta.getVersion();
        historialControlado.sincronizarDosEscrituras();

        Future<ResultadoConcurrente> primera = executor.submit(() -> reasignarConcurrentemente(
                consulta.getId(), escenario.monitorNuevoId(), version, "Operación concurrente A"));
        Future<ResultadoConcurrente> segunda = executor.submit(() -> reasignarConcurrentemente(
                consulta.getId(), escenario.monitorAlternoId(), version, "Operación concurrente B"));

        assertTrue(historialControlado.esperarEscritores(10, TimeUnit.SECONDS));
        historialControlado.liberarEscritores();

        List<ResultadoConcurrente> resultados = List.of(
                primera.get(15, TimeUnit.SECONDS),
                segunda.get(15, TimeUnit.SECONDS));

        assertEquals(1L, resultados.stream()
                .filter(ResultadoConcurrente.APLICADA::equals).count());
        assertEquals(1L, resultados.stream()
                .filter(ResultadoConcurrente.CONFLICTO::equals).count());

        EstadoPersistido persistido = cargarEstado(consulta.getId());
        assertEquals(version + 1, persistido.version());
        assertTrue(Set.of(
                escenario.monitorNuevoId(), escenario.monitorAlternoId())
                .contains(persistido.monitorId()));
        assertEquals(1L, contarEventos(consulta.getId()));
        assertEquals(persistido.monitorId(),
                cargarUnicoEvento(consulta.getId()).nuevoId());
    }

    private Consulta crearConsultaCompatible(
            TipoResponsableConsulta tipo,
            EstadoConsulta estado) {

        return transactionTemplate.execute(transaccion -> {
            Asesor asesor = referencia(Asesor.class, escenario.asesorActualId());
            Estudiante estudiante = referencia(
                    Estudiante.class, escenario.estudianteActualId());
            Monitor monitor = referencia(Monitor.class, escenario.monitorActualId());

            if (tipo == TipoResponsableConsulta.ASESOR) {
                estudiante = null;
            }

            Consulta consulta = new Consulta();
            consulta.setFecha(LocalDate.of(2026, 1, 15));
            consulta.setDescripcion("Consulta de integración para reasignación");
            consulta.setHechos("Hechos jurídicos que deben conservarse");
            consulta.setPretensiones("Pretensiones que deben conservarse");
            consulta.setConceptoJuridico("Concepto jurídico que debe conservarse");
            consulta.setTramite("Asesoría jurídica");
            consulta.setObservaciones("Observaciones inmutables en la operación");
            consulta.setTipoViolencia("No aplica");
            consulta.setResultado("Resultado conservado");
            consulta.setEstado(estado);
            consulta.setPersona(referencia(Persona.class, escenario.personaPrincipalId()));
            consulta.setSede(referencia(Sede.class, escenario.sedeId()));
            consulta.setArea(referencia(Area.class, escenario.areaId()));
            consulta.setTema(referencia(Tema.class, escenario.temaId()));
            consulta.setAsesor(asesor);
            consulta.setEstudiante(estudiante);
            consulta.setMonitor(monitor);
            consulta.setPartes(new ArrayList<>(List.of(
                    referencia(Persona.class, escenario.parteId()))));
            consulta.setContrapartes(new ArrayList<>(List.of(
                    referencia(Persona.class, escenario.contraparteId()))));
            entityManager.persist(consulta);
            entityManager.flush();
            return consulta;
        });
    }

    private org.springframework.test.web.servlet.ResultActions ejecutarPatch(
            String username,
            Long consultaId,
            TipoResponsableConsulta tipo,
            Long nuevoResponsableId,
            String motivo,
            Long version) throws Exception {

        return patchConCredenciales(
                credenciales(username),
                consultaId,
                solicitudJson(tipo, nuevoResponsableId, motivo, version));
    }

    private org.springframework.test.web.servlet.ResultActions patchConCredenciales(
            Credenciales credenciales,
            Long consultaId,
            String json) throws Exception {

        var solicitud = patch("/api/consultas/{id}/responsable", consultaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credenciales.csrfCookie())
                .header(credenciales.csrfHeader(), credenciales.csrfToken());

        if (credenciales.authCookie() != null) {
            solicitud.cookie(credenciales.authCookie());
        }

        return mockMvc.perform(solicitud);
    }

    private org.springframework.test.web.servlet.ResultActions ejecutarGet(
            String username,
            Long consultaId) throws Exception {

        return mockMvc.perform(get("/api/consultas/{id}", consultaId)
                .cookie(new Cookie(
                        "access_token",
                        jwtService.generarToken(username))));
    }

    private org.springframework.test.web.servlet.ResultActions ejecutarGetHistorial(
            String username,
            Long consultaId,
            int page,
            int size) throws Exception {
        return mockMvc.perform(get(
                        "/api/consultas/{id}/responsables/historial",
                        consultaId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .cookie(new Cookie(
                        "access_token",
                        jwtService.generarToken(username))));
    }

    private String cargarNombreMonitor(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT nombre FROM " + ESQUEMA + ".monitor WHERE id = ?",
                String.class,
                id);
    }

    private Credenciales credenciales(String username) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Matcher matcher = TOKEN_JSON.matcher(
                resultado.getResponse().getContentAsString());
        assertTrue(matcher.find(), "La respuesta CSRF debe incluir el token");

        Cookie csrfCookie = resultado.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        Cookie authCookie = username == null
                ? null
                : new Cookie("access_token", jwtService.generarToken(username));

        return new Credenciales(
                authCookie,
                csrfCookie,
                "X-XSRF-TOKEN",
                matcher.group(1));
    }

    private String solicitudJson(
            TipoResponsableConsulta tipo,
            Long nuevoResponsableId,
            String motivo,
            Long version) {

        return """
                {
                  "tipoResponsable":"%s",
                  "nuevoResponsableId":%d,
                  "motivo":"%s",
                  "version":%d
                }
                """.formatted(tipo, nuevoResponsableId, motivo, version);
    }

    private ResultadoConcurrente reasignarConcurrentemente(
            Long consultaId,
            Long monitorId,
            Long version,
            String motivo) {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        escenario.adminUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                ASIGNAR_RESPONSABLES_CONSULTA))));
        try {
            ConsultaReasignacionDTO solicitud = new ConsultaReasignacionDTO();
            solicitud.setTipoResponsable(TipoResponsableConsulta.MONITOR);
            solicitud.setNuevoResponsableId(monitorId);
            solicitud.setMotivo(motivo);
            solicitud.setVersion(version);
            consultaService.reasignarResponsable(consultaId, solicitud);
            return ResultadoConcurrente.APLICADA;
        } catch (RuntimeException exception) {
            if (esConflicto(exception)) {
                return ResultadoConcurrente.CONFLICTO;
            }
            throw exception;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean esConflicto(Throwable throwable) {
        Throwable actual = throwable;
        while (actual != null) {
            if (actual instanceof ConcurrenciaOptimistaException
                    || actual instanceof OptimisticLockException
                    || actual instanceof OptimisticLockingFailureException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private EstadoPersistido cargarEstado(Long consultaId) {
        return jdbcTemplate.queryForObject("""
                SELECT c.version, c.estudiante_id, c.asesor_id, c.monitor_id,
                       c.persona_id, c.sede_id, c.area_id, c.tema_id, c.tipo_id,
                       c.fecha, c.descripcion, c.hechos, c.pretensiones,
                       c.concepto_juridico, c.tramite, c.observaciones,
                       c.tipo_violencia, c.resultado, c.estado,
                       (SELECT COUNT(*) FROM "DB_consultorioJuridico".consulta_parte cp
                        WHERE cp.consulta_id = c.id) AS partes,
                       (SELECT COUNT(*) FROM "DB_consultorioJuridico".consulta_contraparte cc
                        WHERE cc.consulta_id = c.id) AS contrapartes
                FROM "DB_consultorioJuridico".consulta c
                WHERE c.id = ?
                """, (rs, fila) -> new EstadoPersistido(
                rs.getLong("version"),
                nullableLong(rs, "estudiante_id"),
                nullableLong(rs, "asesor_id"),
                nullableLong(rs, "monitor_id"),
                rs.getLong("persona_id"),
                rs.getLong("sede_id"),
                rs.getLong("area_id"),
                rs.getLong("tema_id"),
                nullableLong(rs, "tipo_id"),
                rs.getObject("fecha", LocalDate.class),
                rs.getString("descripcion"),
                rs.getString("hechos"),
                rs.getString("pretensiones"),
                rs.getString("concepto_juridico"),
                rs.getString("tramite"),
                rs.getString("observaciones"),
                rs.getString("tipo_violencia"),
                rs.getString("resultado"),
                rs.getString("estado"),
                rs.getLong("partes"),
                rs.getLong("contrapartes")),
                consultaId);
    }

    private Evento cargarUnicoEvento(Long consultaId) {
        return jdbcTemplate.queryForObject("""
                SELECT tipo_responsable, responsable_anterior_id,
                       responsable_nuevo_id, motivo, actor_usuario_id, ocurrido_en
                FROM "DB_consultorioJuridico".consulta_reasignacion_historial
                WHERE consulta_id = ?
                """, (rs, fila) -> new Evento(
                rs.getString("tipo_responsable"),
                nullableLong(rs, "responsable_anterior_id"),
                rs.getLong("responsable_nuevo_id"),
                rs.getString("motivo"),
                rs.getLong("actor_usuario_id"),
                rs.getTimestamp("ocurrido_en").toInstant()),
                consultaId);
    }

    private long contarEventos(Long consultaId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + HISTORIAL + " WHERE consulta_id = ?",
                Long.class, consultaId);
    }

    private Long cargarId(String columna, Long consultaId) {
        return jdbcTemplate.queryForObject(
                "SELECT " + columna + " FROM " + ESQUEMA + ".consulta WHERE id = ?",
                Long.class, consultaId);
    }

    private Long responsableActual(
            EstadoPersistido estado,
            TipoResponsableConsulta tipo) {
        return switch (tipo) {
            case ESTUDIANTE -> estado.estudianteId();
            case ASESOR -> estado.asesorId();
            case MONITOR -> estado.monitorId();
        };
    }

    private Long responsableNuevo(TipoResponsableConsulta tipo) {
        return switch (tipo) {
            case ESTUDIANTE -> escenario.estudianteNuevoId();
            case ASESOR -> escenario.asesorNuevoId();
            case MONITOR -> escenario.monitorNuevoId();
        };
    }

    private String campoJson(TipoResponsableConsulta tipo) {
        return switch (tipo) {
            case ESTUDIANTE -> "$.estudianteId";
            case ASESOR -> "$.asesorId";
            case MONITOR -> "$.monitorId";
        };
    }

    private void assertCamposNoInvolucrados(
            EstadoPersistido antes,
            EstadoPersistido despues,
            TipoResponsableConsulta tipo) {

        assertEquals(antes.personaId(), despues.personaId());
        assertEquals(antes.sedeId(), despues.sedeId());
        assertEquals(antes.areaId(), despues.areaId());
        assertEquals(antes.temaId(), despues.temaId());
        assertEquals(antes.tipoId(), despues.tipoId());
        assertEquals(antes.fecha(), despues.fecha());
        assertEquals(antes.descripcion(), despues.descripcion());
        assertEquals(antes.hechos(), despues.hechos());
        assertEquals(antes.pretensiones(), despues.pretensiones());
        assertEquals(antes.conceptoJuridico(), despues.conceptoJuridico());
        assertEquals(antes.tramite(), despues.tramite());
        assertEquals(antes.observaciones(), despues.observaciones());
        assertEquals(antes.tipoViolencia(), despues.tipoViolencia());
        assertEquals(antes.resultado(), despues.resultado());
        assertEquals(antes.estado(), despues.estado());
        assertEquals(antes.partes(), despues.partes());
        assertEquals(antes.contrapartes(), despues.contrapartes());

        if (tipo != TipoResponsableConsulta.ESTUDIANTE) {
            assertEquals(antes.estudianteId(), despues.estudianteId());
        }
        if (tipo != TipoResponsableConsulta.ASESOR) {
            assertEquals(antes.asesorId(), despues.asesorId());
        }
        if (tipo != TipoResponsableConsulta.MONITOR) {
            assertEquals(antes.monitorId(), despues.monitorId());
        }
    }

    private Long nullableLong(java.sql.ResultSet resultSet, String columna)
            throws java.sql.SQLException {
        long valor = resultSet.getLong(columna);
        return resultSet.wasNull() ? null : valor;
    }

    private <T> T referencia(Class<T> tipo, Long id) {
        return entityManager.getReference(tipo, id);
    }

    private void ejecutarMigracion() {
        try {
            jdbcTemplate.execute(Files.readString(MIGRACION.toAbsolutePath()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Escenario crearEscenario() {
        String lote = UUID.randomUUID().toString().substring(0, 8);

        Sede sede = new Sede();
        sede.setNombre("Sede integración " + lote);
        entityManager.persist(sede);

        Area area = new Area();
        area.setNombre("Área principal " + lote);
        entityManager.persist(area);

        Area otraArea = new Area();
        otraArea.setNombre("Área alterna " + lote);
        entityManager.persist(otraArea);

        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setNombre("Documento integración " + lote);
        entityManager.persist(tipoDocumento);

        Tema tema = new Tema();
        tema.setNombre("Tema integración " + lote);
        tema.setArea(area);
        entityManager.persist(tema);

        Asesor asesorActual = crearAsesor(
                "actual-" + lote, sede, area, tipoDocumento);
        Asesor asesorNuevo = crearAsesor(
                "nuevo-" + lote, sede, area, tipoDocumento);
        Asesor asesorOtraArea = crearAsesor(
                "otra-area-" + lote, sede, otraArea, tipoDocumento);

        Estudiante estudianteActual = crearEstudiante(
                "actual-" + lote, sede, tipoDocumento, asesorActual);
        Estudiante estudianteNuevo = crearEstudiante(
                "nuevo-" + lote, sede, tipoDocumento, asesorActual);
        Estudiante estudianteIncompatible = crearEstudiante(
                "incompatible-" + lote, sede, tipoDocumento, asesorNuevo);

        Permiso asignar = obtenerOCrearPermiso(ASIGNAR_RESPONSABLES_CONSULTA);
        Permiso ver = obtenerOCrearPermiso(VER_CONSULTAS);

        Rol rolAdmin = crearRol(
                "Administrador reasignación " + lote,
                TipoPerfilUsuario.ADMINISTRATIVO,
                Set.of(asignar, ver));
        Rol rolSinPermiso = crearRol(
                "Administrativo consulta " + lote,
                TipoPerfilUsuario.ADMINISTRATIVO,
                Set.of(ver));
        Rol rolMonitor = crearRol(
                "Monitor reasignación " + lote,
                TipoPerfilUsuario.MONITOR,
                Set.of(asignar, ver));

        UsuarioSistema admin = crearUsuario(
                "admin." + lote + "@integracion.test",
                TipoPerfilUsuario.ADMINISTRATIVO,
                rolAdmin);
        crearAdministrativo("admin-" + lote, sede, tipoDocumento, admin);

        UsuarioSistema sinPermiso = crearUsuario(
                "consulta." + lote + "@integracion.test",
                TipoPerfilUsuario.ADMINISTRATIVO,
                rolSinPermiso);
        crearAdministrativo("consulta-" + lote, sede, tipoDocumento, sinPermiso);

        UsuarioSistema usuarioMonitorActual = crearUsuario(
                "monitor.actual." + lote + "@integracion.test",
                TipoPerfilUsuario.MONITOR,
                rolMonitor);
        Monitor monitorActual = crearMonitor(
                "actual-" + lote, sede, tipoDocumento, true, usuarioMonitorActual);

        UsuarioSistema usuarioMonitorNuevo = crearUsuario(
                "monitor.nuevo." + lote + "@integracion.test",
                TipoPerfilUsuario.MONITOR,
                rolMonitor);
        Monitor monitorNuevo = crearMonitor(
                "nuevo-" + lote, sede, tipoDocumento, true, usuarioMonitorNuevo);

        UsuarioSistema usuarioSinAlcance = crearUsuario(
                "monitor.ajeno." + lote + "@integracion.test",
                TipoPerfilUsuario.MONITOR,
                rolMonitor);
        crearMonitor(
                "ajeno-" + lote, sede, tipoDocumento, true, usuarioSinAlcance);

        Monitor monitorAlterno = crearMonitor(
                "alterno-" + lote, sede, tipoDocumento, true, null);
        Monitor monitorInactivo = crearMonitor(
                "inactivo-" + lote, sede, tipoDocumento, false, null);

        Persona principal = crearPersona("principal-" + lote);
        Persona parte = crearPersona("parte-" + lote);
        Persona contraparte = crearPersona("contraparte-" + lote);

        entityManager.flush();

        return new Escenario(
                sede.getId(), area.getId(), tema.getId(),
                principal.getId(), parte.getId(), contraparte.getId(),
                asesorActual.getId(), asesorNuevo.getId(), asesorOtraArea.getId(),
                estudianteActual.getId(), estudianteNuevo.getId(),
                estudianteIncompatible.getId(),
                monitorActual.getId(), monitorNuevo.getId(),
                monitorAlterno.getId(), monitorInactivo.getId(),
                admin.getId(), admin.getUsername(), sinPermiso.getUsername(),
                usuarioMonitorActual.getUsername(), usuarioMonitorNuevo.getUsername(),
                usuarioSinAlcance.getUsername());
    }

    private Permiso obtenerOCrearPermiso(String nombre) {
        return entityManager.createQuery(
                        "SELECT p FROM Permiso p WHERE p.nombre = :nombre",
                        Permiso.class)
                .setParameter("nombre", nombre)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Permiso permiso = new Permiso();
                    permiso.setNombre(nombre);
                    permiso.setDescripcion("Permiso requerido por la prueba de integracion");
                    permiso.setActivo(true);
                    entityManager.persist(permiso);
                    return permiso;
                });
    }

    private Rol crearRol(
            String nombre,
            TipoPerfilUsuario tipoPerfil,
            Set<Permiso> permisos) {

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion("Rol de integración para reasignaciones");
        rol.setTipoPerfil(tipoPerfil);
        rol.setPermisos(new java.util.HashSet<>(permisos));
        entityManager.persist(rol);
        return rol;
    }

    private UsuarioSistema crearUsuario(
            String username,
            TipoPerfilUsuario tipoPerfil,
            Rol rol) {

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername(username);
        usuario.setPasswordHash("hash-de-integracion");
        usuario.setTipoPerfilActual(tipoPerfil);
        usuario.setRol(rol);
        entityManager.persist(usuario);
        return usuario;
    }

    private Administrativo crearAdministrativo(
            String sufijo,
            Sede sede,
            TipoDocumento tipoDocumento,
            UsuarioSistema usuario) {

        Administrativo administrativo = new Administrativo();
        administrativo.setNombre("Administrativo " + sufijo);
        administrativo.setTipoDocumento(tipoDocumento);
        administrativo.setDocumento("ADM-DOC-" + sufijo);
        administrativo.setEmail("adm." + sufijo + "@integracion.test");
        administrativo.setTelefono("ADM-TEL-" + sufijo);
        administrativo.setUsuario("adm-" + sufijo);
        administrativo.setCodigo("ADM-COD-" + sufijo);
        administrativo.setSede(sede);
        administrativo.setUsuarioSistema(usuario);
        entityManager.persist(administrativo);
        return administrativo;
    }

    private Asesor crearAsesor(
            String sufijo,
            Sede sede,
            Area area,
            TipoDocumento tipoDocumento) {

        Asesor asesor = new Asesor();
        asesor.setNombre("Asesor " + sufijo);
        asesor.setTipoDocumento(tipoDocumento);
        asesor.setDocumento("ASE-DOC-" + sufijo);
        asesor.setEmail("asesor." + sufijo + "@integracion.test");
        asesor.setTelefono("ASE-TEL-" + sufijo);
        asesor.setUsuario("asesor-" + sufijo);
        asesor.setSede(sede);
        asesor.setCodigo("ASE-COD-" + sufijo);
        asesor.setArea(area);
        entityManager.persist(asesor);
        return asesor;
    }

    private Estudiante crearEstudiante(
            String sufijo,
            Sede sede,
            TipoDocumento tipoDocumento,
            Asesor asesor) {

        Estudiante estudiante = new Estudiante();
        estudiante.setNombre("Estudiante " + sufijo);
        estudiante.setTipoDocumento(tipoDocumento);
        estudiante.setDocumento("EST-DOC-" + sufijo);
        estudiante.setEmail("estudiante." + sufijo + "@integracion.test");
        estudiante.setTelefono("EST-TEL-" + sufijo);
        estudiante.setUsuario("estudiante-" + sufijo);
        estudiante.setSede(sede);
        estudiante.setCodigo("EST-COD-" + sufijo);
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);
        return estudiante;
    }

    private Monitor crearMonitor(
            String sufijo,
            Sede sede,
            TipoDocumento tipoDocumento,
            boolean activo,
            UsuarioSistema usuario) {

        Monitor monitor = new Monitor();
        monitor.setNombre("Monitor " + sufijo);
        monitor.setTipoDocumento(tipoDocumento);
        monitor.setDocumento("MON-DOC-" + sufijo);
        monitor.setEmail("monitor." + sufijo + "@integracion.test");
        monitor.setTelefono("MON-TEL-" + sufijo);
        monitor.setUsuario("monitor-" + sufijo);
        monitor.setCodigo("MON-COD-" + sufijo);
        monitor.setSede(sede);
        monitor.setActivo(activo);
        monitor.setUsuarioSistema(usuario);
        entityManager.persist(monitor);
        return monitor;
    }

    private Persona crearPersona(String sufijo) {
        Departamento departamento = new Departamento();
        departamento.setNombre("Departamento " + sufijo);
        entityManager.persist(departamento);

        Municipio municipio = new Municipio();
        municipio.setNombre("Municipio " + sufijo);
        municipio.setDepartamento(departamento);
        entityManager.persist(municipio);

        Barrio barrio = new Barrio();
        barrio.setNombre("Barrio " + sufijo);
        barrio.setMunicipio(municipio);
        entityManager.persist(barrio);

        Nacionalidad nacionalidad = new Nacionalidad();
        nacionalidad.setNombre("Nacionalidad " + sufijo);
        entityManager.persist(nacionalidad);

        TipoPersona tipoPersona = new TipoPersona();
        tipoPersona.setNombre("Solicitante " + sufijo);
        entityManager.persist(tipoPersona);

        Condicion condicion = new Condicion();
        condicion.setNombre("Condición " + sufijo);
        entityManager.persist(condicion);

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setNombre("Ocupación " + sufijo);
        entityManager.persist(ocupacion);

        Empresa empresa = new Empresa();
        empresa.setNombre("Empresa " + sufijo);
        entityManager.persist(empresa);

        Persona persona = new Persona();
        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento("DOC-" + sufijo);
        persona.setFechaExpedicion(LocalDate.of(2010, 1, 1));
        persona.setCiudadExpedicion("Cúcuta");
        persona.setNombres("Persona " + sufijo);
        persona.setApellidos("Integración");
        persona.setNombreIdentitario("Persona");
        persona.setPronombre("ella");
        persona.setSexo("mujer");
        persona.setGenero("femenino");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("TEL-" + sufijo);
        persona.setCorreo("persona." + sufijo + "@integracion.test");
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
        persona.setDireccion("Dirección de prueba");
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

    @TestConfiguration(proxyBeanMethods = false)
    static class HistorialControlConfiguration {

        @Bean
        @Primary
        HistorialControlado historialControlado(
                JpaConsultaReasignacionHistorialWriter writerReal) {
            return new HistorialControlado(writerReal);
        }
    }

    public static class HistorialControlado
            implements ConsultaReasignacionHistorialWriter {

        private final JpaConsultaReasignacionHistorialWriter writerReal;
        private final AtomicReference<ModoHistorial> modo =
                new AtomicReference<>(ModoHistorial.NORMAL);
        private volatile CountDownLatch escritores;
        private volatile CountDownLatch liberar;

        public HistorialControlado(JpaConsultaReasignacionHistorialWriter writerReal) {
            this.writerReal = writerReal;
        }

        @Override
        public void registrar(RegistroReasignacion registro) {
            if (modo.get() == ModoHistorial.FALLAR) {
                writerReal.registrar(new RegistroReasignacion(
                        registro.consultaId(),
                        registro.tipoResponsable(),
                        registro.responsableAnteriorId(),
                        registro.responsableNuevoId(),
                        registro.motivo(),
                        Long.MAX_VALUE));
                return;
            }

            if (modo.get() == ModoHistorial.SINCRONIZAR) {
                escritores.countDown();
                esperar(liberar);
            }

            writerReal.registrar(registro);
        }

        void modoNormal() {
            modo.set(ModoHistorial.NORMAL);
            liberarEscritores();
        }

        void fallarConActorInexistente() {
            modo.set(ModoHistorial.FALLAR);
        }

        void sincronizarDosEscrituras() {
            escritores = new CountDownLatch(2);
            liberar = new CountDownLatch(1);
            modo.set(ModoHistorial.SINCRONIZAR);
        }

        boolean esperarEscritores(long tiempo, TimeUnit unidad)
                throws InterruptedException {
            return escritores.await(tiempo, unidad);
        }

        void liberarEscritores() {
            CountDownLatch latch = liberar;
            if (latch != null) {
                latch.countDown();
            }
        }

        private void esperar(CountDownLatch latch) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Tiempo agotado esperando la sincronización concurrente");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "La sincronización concurrente fue interrumpida", exception);
            }
        }
    }

    private enum ModoHistorial {
        NORMAL,
        FALLAR,
        SINCRONIZAR
    }

    private enum ResultadoConcurrente {
        APLICADA,
        CONFLICTO
    }

    private record Credenciales(
            Cookie authCookie,
            Cookie csrfCookie,
            String csrfHeader,
            String csrfToken) {
    }

    private record Evento(
            String tipo,
            Long anteriorId,
            Long nuevoId,
            String motivo,
            Long actorId,
            java.time.Instant ocurridoEn) {
    }

    private record EstadoPersistido(
            Long version,
            Long estudianteId,
            Long asesorId,
            Long monitorId,
            Long personaId,
            Long sedeId,
            Long areaId,
            Long temaId,
            Long tipoId,
            LocalDate fecha,
            String descripcion,
            String hechos,
            String pretensiones,
            String conceptoJuridico,
            String tramite,
            String observaciones,
            String tipoViolencia,
            String resultado,
            String estado,
            Long partes,
            Long contrapartes) {
    }

    private record Escenario(
            Long sedeId,
            Long areaId,
            Long temaId,
            Long personaPrincipalId,
            Long parteId,
            Long contraparteId,
            Long asesorActualId,
            Long asesorNuevoId,
            Long asesorOtraAreaId,
            Long estudianteActualId,
            Long estudianteNuevoId,
            Long estudianteIncompatibleId,
            Long monitorActualId,
            Long monitorNuevoId,
            Long monitorAlternoId,
            Long monitorInactivoId,
            Long adminUsuarioId,
            String adminUsername,
            String sinPermisoUsername,
            String monitorActualUsername,
            String monitorNuevoUsername,
            String monitorSinAlcanceUsername) {
    }
}



