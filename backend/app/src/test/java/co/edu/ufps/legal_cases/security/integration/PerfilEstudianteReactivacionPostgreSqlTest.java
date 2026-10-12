package co.edu.ufps.legal_cases.security.integration;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_ROL_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ESTUDIANTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerfilEstudianteReactivacionPostgreSqlTest extends PostgreSqlIntegrationTest {

    private static final String SCHEMA = "\"DB_consultorioJuridico\"";

    @Container
    static final PostgreSQLContainer TEST_POSTGRESQL = startPostgreSql();

    private static PostgreSQLContainer startPostgreSql() {
        if (!POSTGRESQL.isRunning()) {
            POSTGRESQL.start();
        }
        return POSTGRESQL;
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private Scenario scenario;

    @BeforeAll
    void verifyExclusivePostgreSqlContainer() throws Exception {
        assertTrue(POSTGRESQL.isRunning());
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(POSTGRESQL.getJdbcUrl(), connection.getMetaData().getURL());
            assertEquals("legal_cases_test", connection.getCatalog());
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertTrue(connection.getMetaData().getDatabaseProductVersion().startsWith("16."));
        }
    }

    @BeforeEach
    void createIndependentSyntheticScenario() {
        transaction = new TransactionTemplate(transactionManager);
        scenario = transaction.execute(ignored -> seedScenario());
        assertNotNull(scenario);
    }

    @Test
    @DisplayName("P01: distingue perfiles de una cuenta compartida y perfiles de cuentas distintas")
    void p01AccountIdentityUsesForeignKeys() {
        assertEquals(scenario.subjectUserId(), profileUserId("asesor", scenario.subjectAdvisorId()));
        assertEquals(scenario.subjectUserId(), profileUserId("estudiante", scenario.subjectStudentId()));
        assertNotEquals(profileUserId("asesor", scenario.independentAdvisorId()),
                profileUserId("estudiante", scenario.independentStudentId()));
    }

    @Test
    @DisplayName("P02: estado inicial expone ASESOR y permisos del rol asesor")
    void p02InitialStateReturnsAdvisor() throws Exception {
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.perfilId").value(scenario.subjectAdvisorId()))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()))
                .andExpect(jsonPath("$.permisos").isArray())
                .andExpect(jsonPath("$.permisos[?(@ == '" + VER_ASESORES_MONITORES + "')]").exists())
                .andExpect(jsonPath("$.permisos[?(@ == '" + VER_ESTUDIANTES + "')]").exists());
        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
    }

    @Test
    @DisplayName("P03: reactivacion directa deja ambos perfiles activos sin cambiar rol ni tipo")
    void p03DirectReactivationPersistsBothProfiles() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        assertState(true, true, true, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P04: la misma sesion sigue resolviendo ASESOR tras reactivar estudiante")
    void p04SameSessionKeepsAdvisorProfileAndPermissions() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk());

        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()))
                .andExpect(jsonPath("$.permisos[?(@ == '" + VER_ASESORES_MONITORES + "')]").exists());
    }

    @Test
    @DisplayName("P05: autorizacion conserva permisos de asesor y aplica alcance de asesor")
    void p05EffectiveAuthorizationUsesAdvisorRoleAndScope() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk());

        authenticatedGet("/api/asesores/" + scenario.subjectAdvisorId(), scenario.subjectUsername())
                .andExpect(status().isOk());

        // Tiene VER_ESTUDIANTES, por lo que el 403 procede de alcance: el estudiante
        // esta asignado al asesor academico, no al perfil asesor de esta cuenta.
        authenticatedGet("/api/estudiantes/" + scenario.subjectStudentId(), scenario.subjectUsername())
                .andExpect(status().isForbidden());

        // No tiene VER_USUARIOS ni GESTIONAR_USUARIOS: rechazo por permiso en @PreAuthorize.
        authenticatedGet("/api/usuarios-sistema", scenario.subjectUsername())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P06: cambio explicito conserva cuenta y cambia perfil, rol, permisos e historial")
    void p06ExplicitProfileChangeTransitionsAtomically() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scenario.subjectUserId()))
                .andExpect(jsonPath("$.tipoPerfil").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.perfilId").value(scenario.subjectStudentId()))
                .andExpect(jsonPath("$.rolId").value(scenario.studentRoleId()));

        assertState(true, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());

        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.rolId").value(scenario.studentRoleId()))
                .andExpect(jsonPath("$.permisos[?(@ == 'Permiso exclusivo estudiante')]").exists())
                .andExpect(jsonPath("$.permisos[?(@ == '" + VER_ASESORES_MONITORES + "')]").doesNotExist());
    }

    @Test
    @DisplayName("P07: desactivar estudiante no actual desactiva la cuenta completa")
    void p07DeactivatingNonCurrentStudentDisablesAccount() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk());
        patchState("estudiantes", scenario.subjectStudentId(), false, scenario.adminUsername())
                .andExpect(status().isOk());

        assertState(false, true, false, "ASESOR", scenario.advisorRoleId());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P08: desactivar asesor actual desactiva cuenta aunque estudiante siga activo")
    void p08DeactivatingCurrentAdvisorDisablesAccount() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk());
        patchState("asesores", scenario.subjectAdvisorId(), false, scenario.adminUsername())
                .andExpect(status().isOk());

        assertState(false, false, true, "ASESOR", scenario.advisorRoleId());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P09: reactivar estudiante historico reactiva acceso como ASESOR")
    void p09ReactivationRestoresInactiveAccountAsAdvisor() throws Exception {
        transaction.executeWithoutResult(ignored -> jdbcTemplate.update(
                "UPDATE " + SCHEMA + ".usuario_sistema SET activo = false WHERE id = ?",
                scenario.subjectUserId()));

        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk());

        assertState(true, true, true, "ASESOR", scenario.advisorRoleId());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()));
    }

    @Test
    @DisplayName("P10: perfil actual inactivo impide autenticacion aunque estudiante este activo")
    void p10InactiveCurrentProfileRejectsAuthentication() throws Exception {
        transaction.executeWithoutResult(ignored -> {
            jdbcTemplate.update("UPDATE " + SCHEMA + ".estudiante SET activo = true WHERE id = ?",
                    scenario.subjectStudentId());
            jdbcTemplate.update("UPDATE " + SCHEMA + ".asesor SET activo = false WHERE id = ?",
                    scenario.subjectAdvisorId());
        });

        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P11: consulta operativa bloquea cambio explicito y revierte todo")
    void p11OperationalConsultationBlocksExplicitChangeAndRollsBack() throws Exception {
        transaction.executeWithoutResult(ignored -> createOperationalConsultation(scenario.subjectAdvisorId()));

        patchProfileToStudent(scenario.adminUsername()).andExpect(status().isBadRequest());

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P12: reactivacion no altera perfiles ni cuentas semejantes")
    void p12ReactivationIsIsolatedFromOtherAccounts() throws Exception {
        AccountState otherBefore = loadAccountState(scenario.independentStudentUserId());
        boolean otherAdvisorBefore = profileActive("asesor", scenario.independentAdvisorId());

        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk());

        assertEquals(otherBefore, loadAccountState(scenario.independentStudentUserId()));
        assertEquals(otherAdvisorBefore, profileActive("asesor", scenario.independentAdvisorId()));
        assertTrue(profileActive("estudiante", scenario.subjectStudentId()));
    }

    private org.springframework.test.web.servlet.ResultActions getMe(String username) throws Exception {
        return authenticatedGet("/api/auth/me", username);
    }

    private org.springframework.test.web.servlet.ResultActions authenticatedGet(String path, String username)
            throws Exception {
        return mockMvc.perform(get(path).cookie(authCookie(username)));
    }

    private org.springframework.test.web.servlet.ResultActions patchState(
            String resource, Long id, boolean active, String username) throws Exception {
        Credentials credentials = credentials(username);
        return mockMvc.perform(patch("/api/" + resource + "/" + id + "/activo")
                .param("activo", Boolean.toString(active))
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions patchProfileToStudent(String username)
            throws Exception {
        Credentials credentials = credentials(username);
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.studentRoleId()),
                java.util.Map.entry("motivo", "Validacion sintetica SCRUM-374"),
                java.util.Map.entry("nombre", "Identidad compartida SCRUM-374"),
                java.util.Map.entry("tipoDocumentoId", scenario.documentTypeId()),
                java.util.Map.entry("documento", scenario.subjectDocument()),
                java.util.Map.entry("telefono", scenario.subjectPhone()),
                java.util.Map.entry("usuario", scenario.subjectCode()),
                java.util.Map.entry("codigo", scenario.subjectCode()),
                java.util.Map.entry("sedeId", scenario.campusId()),
                java.util.Map.entry("asesorId", scenario.academicAdvisorId()),
                java.util.Map.entry("conciliacion", false)));
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.subjectUserId()
                        + "/perfil/estudiante")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private Credentials credentials(String username) throws Exception {
        Cookie authCookie = authCookie(username);
        MvcResult result = mockMvc.perform(get("/api/auth/csrf").cookie(authCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
        return new Credentials(authCookie, csrfCookie,
                body.get("headerName").asText(), body.get("token").asText());
    }

    private Cookie authCookie(String username) {
        return new Cookie("access_token", jwtService.generarToken(username));
    }

    private void assertState(boolean accountActive, boolean advisorActive, boolean studentActive,
            String currentProfile, Long roleId) {
        AccountState account = loadAccountState(scenario.subjectUserId());
        assertEquals(accountActive, account.active());
        assertEquals(currentProfile, account.currentProfile());
        assertEquals(roleId, account.roleId());
        assertEquals(advisorActive, profileActive("asesor", scenario.subjectAdvisorId()));
        assertEquals(studentActive, profileActive("estudiante", scenario.subjectStudentId()));
    }

    private AccountState loadAccountState(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT activo, tipo_perfil_actual, rol_id FROM " + SCHEMA
                        + ".usuario_sistema WHERE id = ?",
                (rs, row) -> new AccountState(rs.getBoolean(1), rs.getString(2), rs.getLong(3)),
                userId);
    }

    private Long profileUserId(String table, Long profileId) {
        return jdbcTemplate.queryForObject(
                "SELECT usuario_sistema_id FROM " + SCHEMA + "." + table + " WHERE id = ?",
                Long.class, profileId);
    }

    private boolean profileActive(String table, Long profileId) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT activo FROM " + SCHEMA + "." + table + " WHERE id = ?",
                Boolean.class, profileId);
        return Boolean.TRUE.equals(value);
    }

    private long historyCount() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + SCHEMA
                        + ".usuario_cambio_perfil_historial WHERE usuario_sistema_id = ?",
                Long.class, scenario.subjectUserId());
        return value == null ? 0L : value;
    }

    private Scenario seedScenario() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Sede campus = persistCampus("Sede " + suffix);
        Area area = persistArea("Area " + suffix);
        TipoDocumento documentType = persistDocumentType("Documento " + suffix);

        Permiso manageUsers = permission(GESTIONAR_USUARIOS);
        Permiso assignRoles = permission(ASIGNAR_ROL_USUARIOS);
        Permiso manageAdvisors = permission(GESTIONAR_ASESORES_MONITORES);
        Permiso seeAdvisors = permission(VER_ASESORES_MONITORES);
        Permiso seeStudents = permission(VER_ESTUDIANTES);
        Permiso studentOnly = permission("Permiso exclusivo estudiante");

        Rol adminRole = role("Admin SCRUM-374 " + suffix, TipoPerfilUsuario.ADMINISTRATIVO,
                Set.of(manageUsers, assignRoles, manageAdvisors));
        Rol advisorRole = role("Asesor SCRUM-374 " + suffix, TipoPerfilUsuario.ASESOR,
                Set.of(seeAdvisors, seeStudents));
        Rol studentRole = role("Estudiante SCRUM-374 " + suffix, TipoPerfilUsuario.ESTUDIANTE,
                Set.of(studentOnly));

        UsuarioSistema admin = user("admin." + suffix + "@integration.test",
                TipoPerfilUsuario.ADMINISTRATIVO, adminRole, true);
        createAdministrator("admin-" + suffix, campus, documentType, admin);

        UsuarioSistema subject = user("identity." + suffix + "@integration.test",
                TipoPerfilUsuario.ASESOR, advisorRole, true);
        Asesor subjectAdvisor = createAdvisor("identity-" + suffix, campus, area,
                documentType, subject, true);

        UsuarioSistema academicUser = user("academic." + suffix + "@integration.test",
                TipoPerfilUsuario.ASESOR, advisorRole, true);
        Asesor academicAdvisor = createAdvisor("academic-" + suffix, campus, area,
                documentType, academicUser, true);

        String subjectDocument = "DOC-ID-" + suffix;
        String subjectPhone = "TEL-ID-" + suffix;
        String subjectCode = "usr-id-" + suffix;
        Estudiante subjectStudent = createStudent("student-history-" + suffix, campus,
                documentType, academicAdvisor, subject, false,
                subjectDocument, subjectPhone, subjectCode);

        UsuarioSistema independentAdvisorUser = user("similar.advisor." + suffix + "@integration.test",
                TipoPerfilUsuario.ASESOR, advisorRole, true);
        Asesor independentAdvisor = createAdvisor("similar-" + suffix, campus, area,
                documentType, independentAdvisorUser, true);

        UsuarioSistema independentStudentUser = user("similar.student." + suffix + "@integration.test",
                TipoPerfilUsuario.ESTUDIANTE, studentRole, true);
        Estudiante independentStudent = createStudent("similar-" + suffix, campus,
                documentType, academicAdvisor, independentStudentUser, true,
                "DOC-SIM-" + suffix, "TEL-SIM-" + suffix, "usr-sim-" + suffix);

        entityManager.flush();
        return new Scenario(admin.getUsername(), subject.getId(), subject.getUsername(),
                subjectAdvisor.getId(), subjectStudent.getId(), academicAdvisor.getId(),
                independentAdvisor.getId(), independentStudent.getId(), independentStudentUser.getId(),
                advisorRole.getId(), studentRole.getId(), campus.getId(), documentType.getId(),
                subjectDocument, subjectPhone, subjectCode);
    }

    private Permiso permission(String name) {
        return entityManager.createQuery("SELECT p FROM Permiso p WHERE p.nombre = :name", Permiso.class)
                .setParameter("name", name).getResultStream().findFirst().orElseGet(() -> {
                    Permiso permission = new Permiso();
                    permission.setNombre(name);
                    permission.setDescripcion("Permiso sintetico SCRUM-374");
                    entityManager.persist(permission);
                    return permission;
                });
    }

    private Rol role(String name, TipoPerfilUsuario profile, Set<Permiso> permissions) {
        Rol role = new Rol();
        role.setNombre(name);
        role.setDescripcion("Rol sintetico SCRUM-374");
        role.setTipoPerfil(profile);
        role.setPermisos(permissions);
        entityManager.persist(role);
        return role;
    }

    private UsuarioSistema user(String username, TipoPerfilUsuario profile, Rol role, boolean active) {
        UsuarioSistema user = new UsuarioSistema();
        user.setUsername(username);
        user.setPasswordHash("synthetic-non-login-hash");
        user.setTipoPerfilActual(profile);
        user.setRol(role);
        user.setActivo(active);
        entityManager.persist(user);
        return user;
    }

    private Sede persistCampus(String name) {
        Sede value = new Sede(); value.setNombre(name); entityManager.persist(value); return value;
    }

    private Area persistArea(String name) {
        Area value = new Area(); value.setNombre(name); entityManager.persist(value); return value;
    }

    private TipoDocumento persistDocumentType(String name) {
        TipoDocumento value = new TipoDocumento(); value.setNombre(name); entityManager.persist(value); return value;
    }

    private Asesor createAdvisor(String suffix, Sede campus, Area area, TipoDocumento documentType,
            UsuarioSistema user, boolean active) {
        Asesor advisor = new Asesor();
        advisor.setNombre("Asesor " + suffix);
        advisor.setTipoDocumento(documentType);
        advisor.setDocumento("ASE-DOC-" + suffix);
        advisor.setEmail(user.getUsername());
        advisor.setTelefono("ASE-TEL-" + suffix);
        advisor.setUsuario("ase-" + suffix);
        advisor.setCodigo("ASE-COD-" + suffix);
        advisor.setSede(campus);
        advisor.setArea(area);
        advisor.setUsuarioSistema(user);
        advisor.setActivo(active);
        entityManager.persist(advisor);
        return advisor;
    }

    private Estudiante createStudent(String suffix, Sede campus, TipoDocumento documentType,
            Asesor advisor, UsuarioSistema user, boolean active, String document, String phone, String code) {
        Estudiante student = new Estudiante();
        student.setNombre("Estudiante " + suffix);
        student.setTipoDocumento(documentType);
        student.setDocumento(document);
        student.setEmail(user.getUsername());
        student.setTelefono(phone);
        student.setUsuario(code);
        student.setCodigo("COD-" + code);
        student.setSede(campus);
        student.setAsesor(advisor);
        student.setUsuarioSistema(user);
        student.setActivo(active);
        student.setConciliacion(false);
        entityManager.persist(student);
        return student;
    }

    private void createAdministrator(String suffix, Sede campus, TipoDocumento documentType,
            UsuarioSistema user) {
        Administrativo admin = new Administrativo();
        admin.setNombre("Administrador " + suffix);
        admin.setTipoDocumento(documentType);
        admin.setDocumento("ADM-DOC-" + suffix);
        admin.setEmail(user.getUsername());
        admin.setTelefono("ADM-TEL-" + suffix);
        admin.setUsuario("adm-" + suffix);
        admin.setCodigo("ADM-COD-" + suffix);
        admin.setSede(campus);
        admin.setDirectora(true);
        admin.setUsuarioSistema(user);
        admin.setActivo(true);
        entityManager.persist(admin);
    }

    private void createOperationalConsultation(Long advisorId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Area area = entityManager.find(Area.class, jdbcTemplate.queryForObject(
                "SELECT area_id FROM " + SCHEMA + ".asesor WHERE id = ?", Long.class, advisorId));
        Sede campus = entityManager.find(Sede.class, scenario.campusId());
        Tema topic = new Tema(); topic.setNombre("Tema " + suffix); topic.setArea(area); entityManager.persist(topic);
        Persona person = createPerson(suffix);
        Consulta consultation = new Consulta();
        consultation.setFecha(LocalDate.of(2026, 10, 11));
        consultation.setDescripcion("Consulta operativa SCRUM-374");
        consultation.setHechos("Hechos sinteticos");
        consultation.setPretensiones("Pretensiones sinteticas");
        consultation.setConceptoJuridico("Concepto sintetico");
        consultation.setTramite("Asesoria");
        consultation.setEstado(EstadoConsulta.ACTIVO);
        consultation.setPersona(person);
        consultation.setSede(campus);
        consultation.setArea(area);
        consultation.setTema(topic);
        consultation.setAsesor(entityManager.find(Asesor.class, advisorId));
        entityManager.persist(consultation);
        entityManager.flush();
    }

    private Persona createPerson(String suffix) {
        Departamento department = new Departamento(); department.setNombre("Depto " + suffix); entityManager.persist(department);
        Municipio city = new Municipio(); city.setNombre("Municipio " + suffix); city.setDepartamento(department); entityManager.persist(city);
        Barrio neighborhood = new Barrio(); neighborhood.setNombre("Barrio " + suffix); neighborhood.setMunicipio(city); entityManager.persist(neighborhood);
        Nacionalidad nationality = new Nacionalidad(); nationality.setNombre("Nacionalidad " + suffix); entityManager.persist(nationality);
        TipoPersona personType = new TipoPersona(); personType.setNombre("Tipo " + suffix); entityManager.persist(personType);
        Condicion condition = new Condicion(); condition.setNombre("Condicion " + suffix); entityManager.persist(condition);
        Ocupacion occupation = new Ocupacion(); occupation.setNombre("Ocupacion " + suffix); entityManager.persist(occupation);
        Empresa company = new Empresa(); company.setNombre("Empresa " + suffix); entityManager.persist(company);
        Persona person = new Persona();
        person.setTipoPersona(personType); person.setTipoDocumento("CC"); person.setNumeroDocumento("PER-" + suffix);
        person.setFechaExpedicion(LocalDate.of(2010, 1, 1)); person.setCiudadExpedicion("Cucuta");
        person.setNombres("Persona"); person.setApellidos(suffix); person.setNombreIdentitario("Persona");
        person.setPronombre("ella"); person.setSexo("mujer"); person.setGenero("femenino");
        person.setOrientacionSexual("no informa"); person.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        person.setNacionalidad(nationality); person.setEstadoCivil("soltero"); person.setEscolaridad("universitaria");
        person.setGrupoEtnico("no informa"); person.setCondicionActual(condition); person.setSabeLeerEscribir(true);
        person.setDiscapacidad("ninguna"); person.setCaracterizacionPcd("no aplica"); person.setNecesitaAjustePcd(false);
        person.setMunicipio(city); person.setBarrio(neighborhood); person.setDireccion("Direccion sintetica");
        person.setComuna("Uno"); person.setLocalidad("Centro"); person.setEstrato(2); person.setTipoVivienda("arrendada");
        person.setZona("urbana"); person.setTenencia("arrendatario"); person.setNumeroPersonasACargo(0);
        person.setIngresosAdicionales(false); person.setEnergiaElectrica(true); person.setAcueducto(true);
        person.setAlcantarillado(true); person.setOcupacion(occupation); person.setEmpresa(company); person.setSalario(0);
        person.setCargo("No aplica"); person.setDireccionEmpresa("No aplica"); person.setTelefonoEmpresa("0000000000");
        person.setComoSeEntero("Universidad"); person.setRelacionConUniversidad("Ninguna"); person.setActivo(true);
        entityManager.persist(person);
        return person;
    }

    private record Credentials(Cookie authCookie, Cookie csrfCookie, String csrfHeader, String csrfToken) {}
    private record AccountState(boolean active, String currentProfile, Long roleId) {}
    private record Scenario(String adminUsername, Long subjectUserId, String subjectUsername,
            Long subjectAdvisorId, Long subjectStudentId, Long academicAdvisorId,
            Long independentAdvisorId, Long independentStudentId, Long independentStudentUserId,
            Long advisorRoleId, Long studentRoleId, Long campusId, Long documentTypeId,
            String subjectDocument, String subjectPhone, String subjectCode) {}
}
