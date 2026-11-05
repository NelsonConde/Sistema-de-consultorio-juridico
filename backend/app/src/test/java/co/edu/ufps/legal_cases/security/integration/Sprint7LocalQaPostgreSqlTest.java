package co.edu.ufps.legal_cases.security.integration;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.CREAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.EDITAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_CONSULTAS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ESTUDIANTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Opt-in QA against a disposable, loopback-only PostgreSQL database; never a deployment. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfEnvironmentVariable(named = "LEGAL_CASES_QA_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Sprint7LocalQaPostgreSqlTest {
    private static final String SCHEMA = "\"DB_consultorioJuridico\"";
    private final String executionId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private String administratorUsername;
    private String advisorUsername;
    private Long documentTypeId;
    private Long campusId;
    private Long advisorId;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("LEGAL_CASES_QA_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/legal_cases_qa_[a-z0-9_]+")) {
            throw new IllegalArgumentException("QA requires a disposable loopback legal_cases_qa_ database");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "legal_cases_qa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @BeforeAll
    void prepare() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.getCatalog().startsWith("legal_cases_qa_"));
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedScenario());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "null", "absent"})
    void studentFlagSurvivesHttpPersistenceDetailListAndSelector(String flag) throws Exception {
        Map<String, Object> body = payload();
        if (!flag.equals("absent")) body.put("conciliacion", flag.equals("null") ? null : Boolean.valueOf(flag));
        Credentials auth = credentials(true);
        var result = mockMvc.perform(post("/api/estudiantes").cookie(auth.cookies())
                .header(auth.header(), auth.token()).contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        var created = jsonMapper.readTree(result.getResponse().getContentAsString());
        long id = created.get("id").asLong();
        boolean expected = flag.equals("true");
        assertEquals(expected, created.get("conciliacion").asBoolean());
        assertEquals(expected, jdbcTemplate.queryForObject("SELECT conciliacion FROM " + SCHEMA + ".estudiante WHERE id = ?", Boolean.class, id));
        var detail = mockMvc.perform(get("/api/estudiantes/" + id).cookie(auth.cookies()))
                .andExpect(status().isOk()).andReturn();
        assertEquals(expected, jsonMapper.readTree(detail.getResponse().getContentAsString()).get("conciliacion").asBoolean());
        var listing = mockMvc.perform(get("/api/estudiantes").param("search", body.get("email").toString()).cookie(auth.cookies()))
                .andExpect(status().isOk()).andReturn();
        var listed = jsonMapper.readTree(listing.getResponse().getContentAsString()).get("content");
        assertEquals(1, listed.size());
        assertEquals(id, listed.get(0).get("id").asLong());
        assertEquals(expected, listed.get(0).get("conciliacion").asBoolean());
        var selector = mockMvc.perform(get("/api/estudiantes/conciliacion/paginados")
                .param("search", body.get("nombre").toString()).cookie(auth.cookies()))
                .andExpect(status().isOk()).andReturn();
        assertEquals(expected ? 1 : 0, jsonMapper.readTree(selector.getResponse().getContentAsString()).get("totalElements").asInt());
        body.put("nombre", body.get("nombre") + " actualizado");
        body.put("conciliacion", !expected);
        mockMvc.perform(put("/api/estudiantes/" + id).cookie(auth.cookies()).header(auth.header(), auth.token())
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        assertEquals(expected, jdbcTemplate.queryForObject("SELECT conciliacion FROM " + SCHEMA + ".estudiante WHERE id = ?", Boolean.class, id));
        mockMvc.perform(patch("/api/estudiantes/" + id + "/conciliacion").param("conciliacion", Boolean.toString(!expected))
                .cookie(auth.cookies()).header(auth.header(), auth.token())).andExpect(status().isOk());
        assertEquals(!expected, jdbcTemplate.queryForObject("SELECT conciliacion FROM " + SCHEMA + ".estudiante WHERE id = ?", Boolean.class, id));
    }

    @Test
    void unauthenticatedStudentCreationHasNoPersistentEffects() throws Exception {
        var body = payload(); body.put("conciliacion", true);
        var auth = credentials(false);
        mockMvc.perform(post("/api/estudiantes").cookie(auth.cookies()).header(auth.header(), auth.token())
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM " + SCHEMA + ".estudiante WHERE email = ?", Integer.class, body.get("email")));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM " + SCHEMA + ".usuario_sistema WHERE username = ?", Integer.class, body.get("email")));
    }

    @Test
    void advisorCannotCreateStudentsEvenWithLegacyCreatePermission() throws Exception {
        var body = payload(); body.put("conciliacion", true);
        var auth = credentials(advisorUsername);
        mockMvc.perform(post("/api/estudiantes").cookie(auth.cookies()).header(auth.header(), auth.token())
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM " + SCHEMA + ".estudiante WHERE email = ?", Integer.class, body.get("email")));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM " + SCHEMA + ".usuario_sistema WHERE username = ?", Integer.class, body.get("email")));
    }

    @Test
    void consultationEndpointAcceptsFrontendSortAndRejectsInvalidSort() throws Exception {
        var auth = credentials(true);
        mockMvc.perform(get("/api/consultas").param("sortBy", "fecha").param("direction", "desc").cookie(auth.cookies()))
                .andExpect(status().isOk());
        for (String invalid : List.of("id", "not_a_field")) {
            mockMvc.perform(get("/api/consultas").param("sortBy", invalid).cookie(auth.cookies()))
                    .andExpect(status().isBadRequest());
        }
    }

    private Map<String, Object> payload() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", "QA " + suffix); body.put("tipoDocumentoId", documentTypeId);
        body.put("documento", "DOC" + suffix); body.put("email", suffix + "@qa.example.test");
        body.put("telefono", "TEL" + suffix); body.put("usuario", "qa" + suffix);
        body.put("sedeId", campusId); body.put("codigo", "COD" + suffix);
        body.put("asesorId", advisorId); body.put("activo", true);
        return body;
    }

    private Credentials credentials(boolean authenticated) throws Exception {
        return credentials(authenticated ? administratorUsername : null);
    }

    private Credentials credentials(String username) throws Exception {
        List<Cookie> cookies = new ArrayList<>();
        var request = get("/api/auth/csrf");
        if (username != null) {
            Cookie cookie = new Cookie("access_token", jwtService.generarToken(username));
            cookies.add(cookie); request.cookie(cookie);
        }
        var response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
        cookies.add(response.getCookie("XSRF-TOKEN"));
        var json = jsonMapper.readTree(response.getContentAsString());
        return new Credentials(cookies.toArray(Cookie[]::new), json.get("headerName").asText(), json.get("token").asText());
    }

    private record Credentials(Cookie[] cookies, String header, String token) {}
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
        Set<Permiso> permissions = new java.util.HashSet<>();
        permissions.add(createUsers);
        for (String name : List.of(VER_ESTUDIANTES, EDITAR_USUARIOS, VER_CONSULTAS)) {
            Permiso permission = new Permiso();
            permission.setNombre(name);
            permission.setDescripcion("Permiso sintetico QA");
            entityManager.persist(permission);
            permissions.add(permission);
        }
        administratorRole.setPermisos(permissions);
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

        Rol advisorRole = new Rol();
        advisorRole.setNombre("Asesor QA " + executionId);
        advisorRole.setDescripcion("Rol sintetico QA");
        advisorRole.setTipoPerfil(TipoPerfilUsuario.ASESOR);
        advisorRole.setCodigoBase(CodigoRolBase.ASESOR);
        advisorRole.setPermisos(permissions);
        entityManager.persist(advisorRole);
        advisorUsername = advisor.getEmail();
        UsuarioSistema advisorUser = new UsuarioSistema();
        advisorUser.setUsername(advisorUsername);
        advisorUser.setPasswordHash("synthetic-non-login-hash");
        advisorUser.setTipoPerfilActual(TipoPerfilUsuario.ASESOR);
        advisorUser.setRol(advisorRole);
        entityManager.persist(advisorUser);
        advisor.setUsuarioSistema(advisorUser);

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

}
