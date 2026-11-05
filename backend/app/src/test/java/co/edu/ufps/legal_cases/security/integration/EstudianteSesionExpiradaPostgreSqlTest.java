package co.edu.ufps.legal_cases.security.integration;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_USUARIOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.common.observability.CorrelationIdContext;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "app.jwt.expiration-ms=2000")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EstudianteSesionExpiradaPostgreSqlTest extends PostgreSqlIntegrationTest {

    private static final String SCHEMA = "\"DB_consultorioJuridico\"";
    private static final String ENDPOINT = "/api/estudiantes";

    // Testcontainers 2.x no inicia el @Container heredado antes de que Spring
    // resuelva las propiedades dinamicas. Se expone el mismo objeto, no otro
    // contenedor, y se arranca durante la inicializacion de la clase para que
    // DynamicPropertySource pueda leer su URL al construir el contexto.
    @Container
    static final PostgreSQLContainer TEST_POSTGRESQL = startPostgreSql();

    private static PostgreSQLContainer startPostgreSql() {
        if (!POSTGRESQL.isRunning()) {
            POSTGRESQL.start();
        }
        return POSTGRESQL;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final String executionId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final Map<String, TestStudent> students = new LinkedHashMap<>();
    private final Map<String, String> rejectedCorrelationIds = new LinkedHashMap<>();

    private String administratorUsername;
    private Long documentTypeId;
    private Long campusId;
    private Long advisorId;

    @BeforeAll
    void prepareIsolatedScenario() throws Exception {
        assertTrue(POSTGRESQL.isRunning(), "PostgreSQL Testcontainer must be running");

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(
                    POSTGRESQL.getJdbcUrl(),
                    connection.getMetaData().getURL(),
                    "The Spring datasource must point exclusively to the Testcontainer");
            assertEquals("legal_cases_test", connection.getCatalog());
        }

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> seedScenario());

        students.put("CP-01", student("cp01"));
        students.put("CP-02", student("cp02"));
        students.put("CP-03", student("cp03"));
        students.put("CP-04", student("cp04"));
        students.put("CP-05", student("cp05"));
        students.put("CP-06", student("cp06"));
    }

    @Test
    @Order(1)
    @DisplayName("CP-01: JWT vigente y CSRF valido crean perfil y cuenta atomicos")
    void cp01AuthorizedCreation() throws Exception {
        TestStudent student = students.get("CP-01");
        Credentials credentials = credentials(jwtService.generarToken(administratorUsername));

        MvcResult result = performPost(student, credentials, true)
                .andExpect(status().isCreated())
                .andReturn();

        assertCreated(student);
        assertSuccessfulCreationAudit(correlationId(result));
    }

    @Test
    @Order(2)
    @DisplayName("CP-02: CSRF valido sin access_token rechaza y no persiste")
    void cp02MissingAuthentication() throws Exception {
        TestStudent student = students.get("CP-02");
        Credentials credentials = credentials(null);

        MvcResult result = performPost(student, credentials, true)
                .andExpect(status().isUnauthorized())
                .andReturn();

        recordRejected("CP-02", result);
        assertNotCreated(student);
        assertDeniedAudit(correlationId(result));
    }

    @Test
    @Order(3)
    @DisplayName("CP-03: JWT real firmado pero expirado rechaza y no persiste")
    void cp03ExpiredJwt() throws Exception {
        TestStudent student = students.get("CP-03");
        Credentials credentials = credentials(expiredSignedToken());

        MvcResult result = performPost(student, credentials, true)
                .andExpect(status().isUnauthorized())
                .andReturn();

        recordRejected("CP-03", result);
        assertNotCreated(student);
        assertDeniedAudit(correlationId(result));
    }

    @Test
    @Order(4)
    @DisplayName("CP-04: JWT invalido con CSRF valido rechaza y no persiste")
    void cp04InvalidJwt() throws Exception {
        TestStudent student = students.get("CP-04");
        Credentials credentials = credentials("invalid-test-jwt");

        MvcResult result = performPost(student, credentials, true)
                .andExpect(status().isUnauthorized())
                .andReturn();

        recordRejected("CP-04", result);
        assertNotCreated(student);
        assertDeniedAudit(correlationId(result));
    }

    @Test
    @Order(5)
    @DisplayName("CP-05: JWT vigente sin CSRF rechaza antes del controller y no persiste")
    void cp05MissingCsrf() throws Exception {
        TestStudent student = students.get("CP-05");
        String token = jwtService.generarToken(administratorUsername);

        MvcResult result = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(student))
                        .cookie(new Cookie("access_token", token)))
                .andExpect(status().isForbidden())
                .andReturn();

        recordRejected("CP-05", result);
        assertNotCreated(student);
        assertDeniedAudit(correlationId(result));
    }

    @Test
    @Order(6)
    @DisplayName("CP-06: formulario HTTP abierto y enviado tras expirar la sesion no persiste")
    void cp06OpenFormSubmittedAfterSessionExpiry() throws Exception {
        TestStudent student = students.get("CP-06");
        String shortLivedToken = jwtService.generarToken(administratorUsername);
        Cookie authCookie = new Cookie("access_token", shortLivedToken);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk());

        Credentials credentials = credentials(shortLivedToken);
        waitUntilExpired(shortLivedToken);

        MvcResult result = performPost(student, credentials, true)
                .andExpect(status().isUnauthorized())
                .andReturn();

        recordRejected("CP-06", result);
        assertNotCreated(student);
        assertDeniedAudit(correlationId(result));
    }

    @Test
    @Order(7)
    @DisplayName("CP-07: evidencia persistente separa alta valida, rechazos y auditoria")
    void cp07PersistentEffects() {
        assertCreated(students.get("CP-01"));

        for (String caseId : List.of("CP-02", "CP-03", "CP-04", "CP-05", "CP-06")) {
            assertNotCreated(students.get(caseId));

            String correlationId = rejectedCorrelationIds.get(caseId);
            assertNotNull(correlationId, "Missing correlation evidence for " + caseId);
            assertDeniedAudit(correlationId);
            assertEquals(0, auditCount(correlationId, "CREAR_PERFIL", "SUCCESS"));
        }
    }

    private org.springframework.test.web.servlet.ResultActions performPost(
            TestStudent student,
            Credentials credentials,
            boolean includeCsrf) throws Exception {

        var request = post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(student));

        List<Cookie> cookies = new ArrayList<>();
        if (credentials.authCookie() != null) {
            cookies.add(credentials.authCookie());
        }
        cookies.add(credentials.csrfCookie());
        request.cookie(cookies.toArray(Cookie[]::new));

        if (includeCsrf) {
            request.header(credentials.csrfHeader(), credentials.csrfToken());
        }

        return mockMvc.perform(request);
    }

    private Credentials credentials(String token) throws Exception {
        var csrfRequest = get("/api/auth/csrf");
        Cookie authCookie = token == null ? null : new Cookie("access_token", token);
        if (authCookie != null) {
            csrfRequest.cookie(authCookie);
        }

        MvcResult csrfResult = mockMvc.perform(csrfRequest)
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);

        var body = jsonMapper.readTree(csrfResult.getResponse().getContentAsString());
        String headerName = body.get("headerName").asText();
        String csrfToken = body.get("token").asText();

        assertFalse(headerName.isBlank());
        assertFalse(csrfToken.isBlank());

        return new Credentials(authCookie, csrfCookie, headerName, csrfToken);
    }

    private String payload(TestStudent student) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", "Estudiante sintetico " + student.caseId());
        body.put("tipoDocumentoId", documentTypeId);
        body.put("documento", student.document());
        body.put("email", student.email());
        body.put("telefono", student.phone());
        body.put("usuario", student.userCode());
        body.put("sedeId", campusId);
        body.put("codigo", student.academicCode());
        body.put("asesorId", advisorId);
        body.put("activo", true);
        body.put("conciliacion", false);
        return jsonMapper.writeValueAsString(body);
    }

    private void waitUntilExpired(String token) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (jwtService.esTokenValido(token) && System.nanoTime() < deadline) {
            LockSupport.parkNanos(java.time.Duration.ofMillis(25).toNanos());
        }
        assertFalse(jwtService.esTokenValido(token), "The short-lived JWT did not expire in the test window");
    }

    private String expiredSignedToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(administratorUsername)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private void assertCreated(TestStudent student) {
        assertEquals(1, studentCount(student));
        assertEquals(1, systemUserCount(student));
        assertEquals(1, studentUserRelationshipCount(student));
    }

    private void assertNotCreated(TestStudent student) {
        assertEquals(0, studentCount(student));
        assertEquals(0, systemUserCount(student));
        assertEquals(0, studentUserRelationshipCount(student));
    }

    private int studentCount(TestStudent student) {
        return count("SELECT COUNT(*) FROM " + SCHEMA + ".estudiante WHERE email = ?", student.email());
    }

    private int systemUserCount(TestStudent student) {
        return count("SELECT COUNT(*) FROM " + SCHEMA + ".usuario_sistema WHERE username = ?", student.email());
    }

    private int studentUserRelationshipCount(TestStudent student) {
        return count("""
                SELECT COUNT(*)
                FROM %s.estudiante e
                JOIN %s.usuario_sistema u ON u.id = e.usuario_sistema_id
                WHERE e.email = ? AND u.username = ?
                """.formatted(SCHEMA, SCHEMA), student.email(), student.email());
    }

    private int auditCount(String correlationId, String action, String outcome) {
        return count("""
                SELECT COUNT(*)
                FROM %s.audit_logs
                WHERE correlation_id = ? AND action = ? AND outcome = ?
                """.formatted(SCHEMA), correlationId, action, outcome);
    }

    private void assertDeniedAudit(String correlationId) {
        assertTrue(
                auditCount(correlationId, "AUTHENTICATION_REQUIRED", "DENIED")
                        + auditCount(correlationId, "ACCESS_DENIED", "DENIED") >= 1,
                "The rejected HTTP request must have denial audit evidence");
    }

    private void assertSuccessfulCreationAudit(String correlationId) {
        assertEquals(1, auditCount(correlationId, "CREAR_PERFIL", "SUCCESS"));
    }

    private int count(String sql, Object... parameters) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, parameters);
        return value == null ? 0 : value;
    }

    private String correlationId(MvcResult result) {
        String correlationId = result.getResponse().getHeader(CorrelationIdContext.HEADER_NAME);
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
        return correlationId;
    }

    private void recordRejected(String caseId, MvcResult result) {
        rejectedCorrelationIds.put(caseId, correlationId(result));
    }

    private TestStudent student(String caseId) {
        String suffix = caseId + "-" + executionId;
        return new TestStudent(
                caseId,
                "DOC-" + suffix,
                caseId.toLowerCase() + "." + executionId + "@integration.test",
                "TEL-" + suffix,
                "usr-" + suffix,
                "COD-" + suffix);
    }

    private void seedScenario() {
        Permiso createUsers = new Permiso();
        createUsers.setNombre(CREAR_USUARIOS);
        createUsers.setDescripcion("Permiso sintetico de integracion");
        entityManager.persist(createUsers);

        Rol administratorRole = new Rol();
        administratorRole.setNombre("Administrador de prueba " + executionId);
        administratorRole.setDescripcion("Rol sintetico de integracion");
        administratorRole.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        administratorRole.setCodigoBase(CodigoRolBase.ADMINISTRADOR);
        administratorRole.setPermisos(Set.of(createUsers));
        entityManager.persist(administratorRole);

        Rol studentRole = new Rol();
        studentRole.setNombre("Estudiante de prueba " + executionId);
        studentRole.setDescripcion("Rol base sintetico de integracion");
        studentRole.setTipoPerfil(TipoPerfilUsuario.ESTUDIANTE);
        studentRole.setCodigoBase(CodigoRolBase.ESTUDIANTE);
        entityManager.persist(studentRole);

        Sede campus = new Sede();
        campus.setNombre("Sede de prueba " + executionId);
        entityManager.persist(campus);

        TipoDocumento documentType = new TipoDocumento();
        documentType.setNombre("Tipo documental de prueba " + executionId);
        entityManager.persist(documentType);

        Area area = new Area();
        area.setNombre("Area de prueba " + executionId);
        entityManager.persist(area);

        Asesor advisor = new Asesor();
        advisor.setNombre("Asesor sintetico de prueba");
        advisor.setTipoDocumento(documentType);
        advisor.setDocumento("ASE-DOC-" + executionId);
        advisor.setEmail("advisor." + executionId + "@integration.test");
        advisor.setTelefono("ASE-TEL-" + executionId);
        advisor.setUsuario("advisor-" + executionId);
        advisor.setCodigo("ASE-COD-" + executionId);
        advisor.setSede(campus);
        advisor.setArea(area);
        entityManager.persist(advisor);

        administratorUsername = "administrator." + executionId + "@integration.test";
        UsuarioSistema administrator = new UsuarioSistema();
        administrator.setUsername(administratorUsername);
        administrator.setPasswordHash("synthetic-non-login-hash");
        administrator.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        administrator.setRol(administratorRole);
        entityManager.persist(administrator);

        Administrativo administratorProfile = new Administrativo();
        administratorProfile.setNombre("Administrador sintetico de prueba");
        administratorProfile.setTipoDocumento(documentType);
        administratorProfile.setDocumento("ADM-DOC-" + executionId);
        administratorProfile.setEmail(administratorUsername);
        administratorProfile.setTelefono("ADM-TEL-" + executionId);
        administratorProfile.setUsuario("admin-" + executionId);
        administratorProfile.setCodigo("ADM-COD-" + executionId);
        administratorProfile.setSede(campus);
        administratorProfile.setDirectora(true);
        administratorProfile.setUsuarioSistema(administrator);
        entityManager.persist(administratorProfile);

        entityManager.flush();
        documentTypeId = documentType.getId();
        campusId = campus.getId();
        advisorId = advisor.getId();
    }

    private record Credentials(
            Cookie authCookie,
            Cookie csrfCookie,
            String csrfHeader,
            String csrfToken) {
    }

    private record TestStudent(
            String caseId,
            String document,
            String email,
            String phone,
            String userCode,
            String academicCode) {
    }
}
