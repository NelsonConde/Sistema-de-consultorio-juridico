package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_RESPONSABLES_CONSULTA;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * QA verification test suite for SCRUM-306:
 * - Immutable reassignment history
 * - Transactional consistency and rollback on failure
 * - Ordering (ocurrido_en DESC, id DESC), actor, date, and reason persistence
 * - Single and multiple sequential reassignments
 * - Authorized read access control
 * - Trigger immutability (SQLSTATE 55000 on UPDATE or DELETE)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfEnvironmentVariable(named = "LEGAL_CASES_QA_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsultaReasignacionHistorialQaPostgreSqlTest {

    private static final String SCHEMA = "\"DB_consultorioJuridico\"";
    private static final String TABLA = SCHEMA + ".consulta_reasignacion_historial";
    private static final Path MIGRACION_DDL = Path.of("db", "migration", "V20261011_01__create_consulta_reasignacion_historial.sql");
    private static final Path MIGRACION_VERIFY = Path.of("db", "migration", "V20261011_01__create_consulta_reasignacion_historial_verify.sql");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private String adminUsername;
    private String unauthorizedUsername;
    private Long consultaId;
    private Long asesor1Id;
    private Long asesor2Id;
    private Long asesor3Id;
    private Long estudiante1Id;
    private Long estudiante2Id;
    private Long monitor1Id;
    private Long monitor2Id;
    private Long currentVersion;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("LEGAL_CASES_QA_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/legal_cases_qa_[a-z0-9_]+")) {
            throw new IllegalArgumentException("QA requires a disposable loopback legal_cases_qa_ database");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "legal_cases_qa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "\"DB_consultorioJuridico\"");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @BeforeAll
    void setupScenario() {
        // 1. Ensure table and triggers match the versioned Flyway migration script
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + TABLA + " CASCADE");
        ejecutarScript(MIGRACION_DDL);

        // 2. Seed test fixtures
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedData());
    }

    @Test
    @Order(1)
    void test01_verifyMigrationScriptAndSchemaIntegrity() {
        // Runs the official verify script which asserts all 8 columns, 8 constraints, and 2 indexes
        ejecutarScript(MIGRACION_VERIFY);
    }

    @Test
    @Order(2)
    void test02_singleAndMultipleSequentialReassignments() throws Exception {
        Credentials adminAuth = credentials(adminUsername);

        // Event 1: Reassign ASESOR (asesor1 -> asesor2)
        ConsultaReasignacionDTO dtoAsesor = new ConsultaReasignacionDTO();
        dtoAsesor.setTipoResponsable(TipoResponsableConsulta.ASESOR);
        dtoAsesor.setNuevoResponsableId(asesor2Id);
        dtoAsesor.setMotivo("Reasignación operativa de asesor de caso");
        dtoAsesor.setVersion(currentVersion);

        var patch1 = mockMvc.perform(patch("/api/consultas/" + consultaId + "/responsable")
                .cookie(adminAuth.cookies())
                .header(adminAuth.header(), adminAuth.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dtoAsesor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asesorId").value(asesor2Id))
                .andExpect(jsonPath("$.version").value(currentVersion + 1))
                .andReturn();

        var resNode1 = jsonMapper.readTree(patch1.getResponse().getContentAsString());
        currentVersion = resNode1.get("version").asLong();

        // Event 2: Reassign MONITOR (monitor1 -> monitor2)
        ConsultaReasignacionDTO dtoMonitor = new ConsultaReasignacionDTO();
        dtoMonitor.setTipoResponsable(TipoResponsableConsulta.MONITOR);
        dtoMonitor.setNuevoResponsableId(monitor2Id);
        dtoMonitor.setMotivo("Reasignación de monitor de apoyo");
        dtoMonitor.setVersion(currentVersion);

        var patch2 = mockMvc.perform(patch("/api/consultas/" + consultaId + "/responsable")
                .cookie(adminAuth.cookies())
                .header(adminAuth.header(), adminAuth.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dtoMonitor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitorId").value(monitor2Id))
                .andExpect(jsonPath("$.version").value(currentVersion + 1))
                .andReturn();

        var resNode2 = jsonMapper.readTree(patch2.getResponse().getContentAsString());
        currentVersion = resNode2.get("version").asLong();

        // Event 3: Reassign ASESOR again (asesor2 -> asesor3)
        ConsultaReasignacionDTO dtoAsesor2 = new ConsultaReasignacionDTO();
        dtoAsesor2.setTipoResponsable(TipoResponsableConsulta.ASESOR);
        dtoAsesor2.setNuevoResponsableId(asesor3Id);
        dtoAsesor2.setMotivo("Reasignación por traslado a asesor especialista");
        dtoAsesor2.setVersion(currentVersion);

        var patch3 = mockMvc.perform(patch("/api/consultas/" + consultaId + "/responsable")
                .cookie(adminAuth.cookies())
                .header(adminAuth.header(), adminAuth.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dtoAsesor2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asesorId").value(asesor3Id))
                .andExpect(jsonPath("$.version").value(currentVersion + 1))
                .andReturn();

        var resNode3 = jsonMapper.readTree(patch3.getResponse().getContentAsString());
        currentVersion = resNode3.get("version").asLong();

        // Verify count of immutable history entries in database
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLA + " WHERE consulta_id = ?",
                Long.class, consultaId);
        assertEquals(3L, count);

        // Verify sequential chain of changes in SQL
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                "SELECT tipo_responsable, responsable_anterior_id, responsable_nuevo_id, motivo "
                        + "FROM " + TABLA + " WHERE consulta_id = ? ORDER BY id ASC",
                consultaId);

        assertEquals("ASESOR", filas.get(0).get("tipo_responsable"));
        assertEquals(asesor1Id, ((Number) filas.get(0).get("responsable_anterior_id")).longValue());
        assertEquals(asesor2Id, ((Number) filas.get(0).get("responsable_nuevo_id")).longValue());

        assertEquals("MONITOR", filas.get(1).get("tipo_responsable"));
        assertEquals(monitor1Id, ((Number) filas.get(1).get("responsable_anterior_id")).longValue());
        assertEquals(monitor2Id, ((Number) filas.get(1).get("responsable_nuevo_id")).longValue());

        assertEquals("ASESOR", filas.get(2).get("tipo_responsable"));
        assertEquals(asesor2Id, ((Number) filas.get(2).get("responsable_anterior_id")).longValue());
        assertEquals(asesor3Id, ((Number) filas.get(2).get("responsable_nuevo_id")).longValue());
    }

    @Test
    @Order(3)
    void test03_orderActorDateReasonAndFormatting() throws Exception {
        Credentials adminAuth = credentials(adminUsername);

        var response = mockMvc.perform(get("/api/consultas/" + consultaId + "/responsables/historial")
                .param("page", "1")
                .param("size", "10")
                .cookie(adminAuth.cookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andReturn();

        var root = jsonMapper.readTree(response.getResponse().getContentAsString());
        var content = root.get("content");

        // The endpoint must return items ordered by ocurrido_en DESC, id DESC (most recent first)
        assertEquals("ASESOR", content.get(0).get("tipoResponsable").asText());
        assertEquals("Reasignación por traslado a asesor especialista", content.get(0).get("motivo").asText());
        assertTrue(content.get(0).get("responsableNuevoNombre").asText().contains("Asesor 3"));
        assertTrue(content.get(0).get("actorNombre").asText().contains("Administrador QA"));
        assertNotNull(content.get(0).get("ocurridoEn").asText());

        assertEquals("MONITOR", content.get(1).get("tipoResponsable").asText());
        assertEquals("Reasignación de monitor de apoyo", content.get(1).get("motivo").asText());

        assertEquals("ASESOR", content.get(2).get("tipoResponsable").asText());
        assertEquals("Reasignación operativa de asesor de caso", content.get(2).get("motivo").asText());
    }

    @Test
    @Order(4)
    void test04_authorizedReadAndAccessControl() throws Exception {
        // 1. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(get("/api/consultas/" + consultaId + "/responsables/historial"))
                .andExpect(status().isUnauthorized());

        // 2. Authenticated user without VER_CONSULTAS -> 403 Forbidden
        Credentials unauth = credentials(unauthorizedUsername);
        mockMvc.perform(get("/api/consultas/" + consultaId + "/responsables/historial")
                .cookie(unauth.cookies()))
                .andExpect(status().isForbidden());

        // 3. Authorized user with VER_CONSULTAS -> 200 OK
        Credentials adminAuth = credentials(adminUsername);
        mockMvc.perform(get("/api/consultas/" + consultaId + "/responsables/historial")
                .cookie(adminAuth.cookies()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void test05_historicalImmutabilityTriggerRejectsUpdateAndDelete() {
        Long historyId = jdbcTemplate.queryForObject(
                "SELECT id FROM " + TABLA + " WHERE consulta_id = ? ORDER BY id ASC LIMIT 1",
                Long.class, consultaId);
        assertNotNull(historyId);

        // Attempt direct SQL UPDATE on history record
        DataAccessException updateEx = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update("UPDATE " + TABLA + " SET motivo = 'manipulado' WHERE id = ?", historyId)
        );
        String updateMsg = extractSqlErrorMessage(updateEx);
        assertTrue(updateMsg.contains("inmutable") || updateMsg.contains("55000"),
                "Expected trigger immutability failure, but got: " + updateMsg);

        // Attempt direct SQL DELETE on history record
        DataAccessException deleteEx = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update("DELETE FROM " + TABLA + " WHERE id = ?", historyId)
        );
        String deleteMsg = extractSqlErrorMessage(deleteEx);
        assertTrue(deleteMsg.contains("inmutable") || deleteMsg.contains("55000"),
                "Expected trigger immutability failure, but got: " + deleteMsg);

        // Verify total row count remains exactly 3
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLA + " WHERE consulta_id = ?",
                Long.class, consultaId);
        assertEquals(3L, total);
    }

    @Test
    @Order(6)
    void test06_transactionalConsistencyAndRollbackOnError() throws Exception {
        Credentials adminAuth = credentials(adminUsername);

        Long countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLA + " WHERE consulta_id = ?",
                Long.class, consultaId);

        // Attempt reassignment with a non-existent responsible ID (foreign key / trigger rejection)
        ConsultaReasignacionDTO invalidDto = new ConsultaReasignacionDTO();
        invalidDto.setTipoResponsable(TipoResponsableConsulta.ASESOR);
        invalidDto.setNuevoResponsableId(999999L);
        invalidDto.setMotivo("Intento inválido con asesor inexistente");
        invalidDto.setVersion(currentVersion);

        mockMvc.perform(patch("/api/consultas/" + consultaId + "/responsable")
                .cookie(adminAuth.cookies())
                .header(adminAuth.header(), adminAuth.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(invalidDto)))
                .andExpect(status().is4xxClientError());

        // Verify no row was inserted into history (atomic rollback)
        Long countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLA + " WHERE consulta_id = ?",
                Long.class, consultaId);
        assertEquals(countBefore, countAfter);

        // Verify consultation's current responsible and version were not altered
        Long currentAsesorInDb = jdbcTemplate.queryForObject(
                "SELECT asesor_id FROM " + SCHEMA + ".consulta WHERE id = ?",
                Long.class, consultaId);
        assertEquals(asesor3Id, currentAsesorInDb);

        Long currentVerInDb = jdbcTemplate.queryForObject(
                "SELECT version FROM " + SCHEMA + ".consulta WHERE id = ?",
                Long.class, consultaId);
        assertEquals(currentVersion, currentVerInDb);
    }

    private String extractSqlErrorMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof PSQLException psqlEx) {
                return psqlEx.getMessage() + " [SQLSTATE " + psqlEx.getSQLState() + "]";
            }
            cause = cause.getCause();
        }
        return ex.getMessage();
    }

    private void ejecutarScript(Path path) {
        try {
            jdbcTemplate.execute(Files.readString(path.toAbsolutePath()));
        } catch (IOException e) {
            throw new UncheckedIOException("Error leyendo script " + path, e);
        }
    }

    private Credentials credentials(String username) throws Exception {
        List<Cookie> cookies = new ArrayList<>();
        var request = get("/api/auth/csrf");
        if (username != null) {
            Cookie cookie = new Cookie("access_token", jwtService.generarToken(username));
            cookies.add(cookie);
            request.cookie(cookie);
        }
        var response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
        cookies.add(response.getCookie("XSRF-TOKEN"));
        var json = jsonMapper.readTree(response.getContentAsString());
        return new Credentials(cookies.toArray(Cookie[]::new), json.get("headerName").asText(), json.get("token").asText());
    }

    private record Credentials(Cookie[] cookies, String header, String token) {}

    private void seedData() {
        Sede campus = new Sede();
        campus.setNombre("Sede Central QA " + runId);
        entityManager.persist(campus);

        TipoDocumento docType = new TipoDocumento();
        docType.setNombre("CC QA " + runId);
        entityManager.persist(docType);

        Area area = new Area();
        area.setNombre("Área Civil QA " + runId);
        entityManager.persist(area);

        Tema tema = new Tema();
        tema.setNombre("Contratos QA " + runId);
        tema.setArea(area);
        entityManager.persist(tema);

        Departamento depto = new Departamento();
        depto.setNombre("Norte de Santander " + runId);
        entityManager.persist(depto);

        Municipio municipio = new Municipio();
        municipio.setNombre("Cúcuta " + runId);
        municipio.setDepartamento(depto);
        entityManager.persist(municipio);

        Barrio barrio = new Barrio();
        barrio.setNombre("Centro " + runId);
        barrio.setMunicipio(municipio);
        entityManager.persist(barrio);

        Nacionalidad nac = new Nacionalidad();
        nac.setNombre("Colombiana " + runId);
        entityManager.persist(nac);

        TipoPersona tipoPersona = new TipoPersona();
        tipoPersona.setNombre("Natural " + runId);
        entityManager.persist(tipoPersona);

        Condicion condicion = new Condicion();
        condicion.setNombre("Ninguna " + runId);
        entityManager.persist(condicion);

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setNombre("Empleado " + runId);
        entityManager.persist(ocupacion);

        Empresa empresa = new Empresa();
        empresa.setNombre("Particular " + runId);
        entityManager.persist(empresa);

        Persona persona = new Persona();
        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento("DOC-PER-" + runId);
        persona.setFechaExpedicion(LocalDate.of(2015, 5, 20));
        persona.setCiudadExpedicion("Cúcuta");
        persona.setNombres("Usuario");
        persona.setApellidos("Consultante QA");
        persona.setNombreIdentitario("Usuario QA");
        persona.setPronombre("él");
        persona.setSexo("hombre");
        persona.setGenero("masculino");
        persona.setOrientacionSexual("heterosexual");
        persona.setFechaNacimiento(LocalDate.of(1995, 3, 10));
        persona.setTelefono("3001234567");
        persona.setCorreo("consultante." + runId + "@qa.test");
        persona.setNacionalidad(nac);
        persona.setEstadoCivil("soltero");
        persona.setEscolaridad("profesional");
        persona.setGrupoEtnico("ninguno");
        persona.setCondicionActual(condicion);
        persona.setSabeLeerEscribir(true);
        persona.setDiscapacidad("ninguna");
        persona.setCaracterizacionPcd("no aplica");
        persona.setNecesitaAjustePcd(false);
        persona.setMunicipio(municipio);
        persona.setBarrio(barrio);
        persona.setDireccion("Avenida 0 # 1-23");
        persona.setComuna("1");
        persona.setLocalidad("Comuna 1");
        persona.setEstrato(3);
        persona.setTipoVivienda("propia");
        persona.setZona("urbana");
        persona.setTenencia("propietario");
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

        // 3 Asesores
        Asesor a1 = createAsesor("1", campus, area, docType);
        Asesor a2 = createAsesor("2", campus, area, docType);
        Asesor a3 = createAsesor("3", campus, area, docType);
        asesor1Id = a1.getId();
        asesor2Id = a2.getId();
        asesor3Id = a3.getId();

        // 2 Estudiantes
        Estudiante e1 = createEstudiante("1", campus, docType, a1);
        Estudiante e2 = createEstudiante("2", campus, docType, a1);
        estudiante1Id = e1.getId();
        estudiante2Id = e2.getId();

        // 2 Monitores
        Monitor m1 = createMonitor("1", campus, docType);
        Monitor m2 = createMonitor("2", campus, docType);
        monitor1Id = m1.getId();
        monitor2Id = m2.getId();

        // Permissions and Roles
        Permiso permAsignar = createPermiso(ASIGNAR_RESPONSABLES_CONSULTA);
        Permiso permVer = createPermiso(VER_CONSULTAS);
        Permiso permGestionar = createPermiso(GESTIONAR_CONSULTAS);

        Rol adminRole = new Rol();
        adminRole.setNombre("Rol Admin QA " + runId);
        adminRole.setDescripcion("Rol Admin para pruebas de reasignación");
        adminRole.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        adminRole.setCodigoBase(CodigoRolBase.ADMINISTRADOR);
        adminRole.setPermisos(Set.of(permAsignar, permVer, permGestionar));
        adminRole.setActivo(true);
        entityManager.persist(adminRole);

        adminUsername = "admin." + runId + "@qa.test";
        UsuarioSistema adminUser = new UsuarioSistema();
        adminUser.setUsername(adminUsername);
        adminUser.setPasswordHash("test-hash");
        adminUser.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        adminUser.setRol(adminRole);
        adminUser.setActivo(true);
        entityManager.persist(adminUser);

        Administrativo adminProfile = new Administrativo();
        adminProfile.setNombre("Administrador QA " + runId);
        adminProfile.setTipoDocumento(docType);
        adminProfile.setDocumento("DOC-ADM-" + runId);
        adminProfile.setEmail(adminUsername);
        adminProfile.setTelefono("3101112233");
        adminProfile.setUsuario("admin-" + runId);
        adminProfile.setCodigo("ADM-" + runId);
        adminProfile.setSede(campus);
        adminProfile.setDirectora(true);
        adminProfile.setActivo(true);
        adminProfile.setUsuarioSistema(adminUser);
        entityManager.persist(adminProfile);

        // Unauthorized user without permissions
        Rol emptyRole = new Rol();
        emptyRole.setNombre("Rol Vacio QA " + runId);
        emptyRole.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        emptyRole.setCodigoBase(null);
        emptyRole.setPermisos(new HashSet<>());
        emptyRole.setActivo(true);
        entityManager.persist(emptyRole);

        unauthorizedUsername = "unauthorized." + runId + "@qa.test";
        UsuarioSistema unauthUser = new UsuarioSistema();
        unauthUser.setUsername(unauthorizedUsername);
        unauthUser.setPasswordHash("test-hash");
        unauthUser.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        unauthUser.setRol(emptyRole);
        unauthUser.setActivo(true);
        entityManager.persist(unauthUser);

        Administrativo unauthProfile = new Administrativo();
        unauthProfile.setNombre("No Autorizado QA " + runId);
        unauthProfile.setTipoDocumento(docType);
        unauthProfile.setDocumento("DOC-UNAUTH-" + runId);
        unauthProfile.setEmail(unauthorizedUsername);
        unauthProfile.setTelefono("3109998877");
        unauthProfile.setUsuario("unauth-" + runId);
        unauthProfile.setCodigo("UNAUTH-" + runId);
        unauthProfile.setSede(campus);
        unauthProfile.setDirectora(false);
        unauthProfile.setActivo(true);
        unauthProfile.setUsuarioSistema(unauthUser);
        entityManager.persist(unauthProfile);

        // Initial Consulta
        Consulta c = new Consulta();
        c.setFecha(LocalDate.now());
        c.setDescripcion("Consulta inicial para pruebas de reasignación QA");
        c.setHechos("Hechos del caso QA");
        c.setPretensiones("Pretensiones del caso QA");
        c.setConceptoJuridico("Concepto inicial QA");
        c.setTramite("Asesoría");
        c.setEstado(EstadoConsulta.ACTIVO);
        c.setPersona(persona);
        c.setSede(campus);
        c.setArea(area);
        c.setTema(tema);
        c.setAsesor(a1);
        c.setEstudiante(null);
        c.setMonitor(m1);
        entityManager.persist(c);
        entityManager.flush();

        consultaId = c.getId();
        currentVersion = c.getVersion();
    }

    private Asesor createAsesor(String suffix, Sede sede, Area area, TipoDocumento docType) {
        Asesor a = new Asesor();
        a.setNombre("Asesor " + suffix + " QA " + runId);
        a.setTipoDocumento(docType);
        a.setDocumento("DOC-ASE-" + suffix + "-" + runId);
        a.setEmail("asesor." + suffix + "." + runId + "@qa.test");
        a.setTelefono("320111223" + suffix);
        a.setUsuario("asesor" + suffix + "-" + runId);
        a.setCodigo("ASE-" + suffix + "-" + runId);
        a.setSede(sede);
        a.setArea(area);
        a.setActivo(true);
        entityManager.persist(a);
        return a;
    }

    private Estudiante createEstudiante(String suffix, Sede sede, TipoDocumento docType, Asesor asesor) {
        Estudiante e = new Estudiante();
        e.setNombre("Estudiante " + suffix + " QA " + runId);
        e.setTipoDocumento(docType);
        e.setDocumento("DOC-EST-" + suffix + "-" + runId);
        e.setEmail("estudiante." + suffix + "." + runId + "@qa.test");
        e.setTelefono("315111223" + suffix);
        e.setUsuario("estudiante" + suffix + "-" + runId);
        e.setCodigo("EST-" + suffix + "-" + runId);
        e.setSede(sede);
        e.setAsesor(asesor);
        e.setActivo(true);
        entityManager.persist(e);
        return e;
    }

    private Monitor createMonitor(String suffix, Sede sede, TipoDocumento docType) {
        Monitor m = new Monitor();
        m.setNombre("Monitor " + suffix + " QA " + runId);
        m.setTipoDocumento(docType);
        m.setDocumento("DOC-MON-" + suffix + "-" + runId);
        m.setEmail("monitor." + suffix + "." + runId + "@qa.test");
        m.setTelefono("318111223" + suffix);
        m.setUsuario("monitor" + suffix + "-" + runId);
        m.setCodigo("MON-" + suffix + "-" + runId);
        m.setSede(sede);
        m.setActivo(true);
        entityManager.persist(m);
        return m;
    }

    private Permiso createPermiso(String nombre) {
        Permiso p = new Permiso();
        p.setNombre(nombre);
        p.setDescripcion("Permiso sintetico QA: " + nombre);
        p.setActivo(true);
        entityManager.persist(p);
        return p;
    }
}
