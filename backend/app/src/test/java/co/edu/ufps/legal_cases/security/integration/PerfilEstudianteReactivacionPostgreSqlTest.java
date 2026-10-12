package co.edu.ufps.legal_cases.security.integration;

import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ASIGNAR_ROL_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ACCEDER_ADMINISTRACION;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.ACCEDER_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ADMINISTRADORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_PERMISOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_ROLES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.GESTIONAR_USUARIOS;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ASESORES_MONITORES;
import static co.edu.ufps.legal_cases.security.constant.PermisoNombre.VER_ESTUDIANTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.security.model.access.Permiso;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetTokenService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PerfilEstudianteReactivacionPostgreSqlTest.LockCoordinationConfig.class)
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
    @Autowired private AccountLockCoordinator accountLockCoordinator;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetTokenService passwordResetTokenService;

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
    @DisplayName("P03: reactivacion directa de estudiante historico se rechaza sin cambios")
    void p03DirectReactivationOfHistoricalProfileIsRejected() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P04: la misma sesion conserva ASESOR tras rechazar la reactivacion")
    void p04SameSessionKeepsAdvisorProfileAndPermissions() throws Exception {
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isConflict());

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
                .andExpect(status().isConflict());

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
    @DisplayName("P07: desactivar estudiante historico activo se rechaza sin sanear datos")
    void p07DeactivatingActiveHistoricalStudentIsRejected() throws Exception {
        setProfileActive("estudiante", scenario.subjectStudentId(), true);

        patchState("estudiantes", scenario.subjectStudentId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, true, "ASESOR", scenario.advisorRoleId());
        getMe(scenario.subjectUsername()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("P08: desactivar asesor actual se rechaza sin afectar la cuenta")
    void p08DeactivatingCurrentAdvisorIsRejected() throws Exception {
        setProfileActive("estudiante", scenario.subjectStudentId(), true);

        patchState("asesores", scenario.subjectAdvisorId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, true, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()));
    }

    @Test
    @DisplayName("P09: reactivar estudiante historico no rehabilita la cuenta")
    void p09HistoricalReactivationDoesNotRestoreInactiveAccount() throws Exception {
        setAccountActive(false);

        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(false, true, false, "ASESOR", scenario.advisorRoleId());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
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
    @DisplayName("P12: rechazo de reactivacion no altera perfiles ni cuentas semejantes")
    void p12ReactivationIsIsolatedFromOtherAccounts() throws Exception {
        AccountState otherBefore = loadAccountState(scenario.independentStudentUserId());
        boolean otherAdvisorBefore = profileActive("asesor", scenario.independentAdvisorId());

        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isConflict());

        assertEquals(otherBefore, loadAccountState(scenario.independentStudentUserId()));
        assertEquals(otherAdvisorBefore, profileActive("asesor", scenario.independentAdvisorId()));
        assertFalse(profileActive("estudiante", scenario.subjectStudentId()));
    }

    @Test
    @DisplayName("P13: activar estudiante actual no habilita una cuenta suspendida")
    void p13CurrentStudentActivationDoesNotEnableAccount() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        setProfileActive("estudiante", scenario.subjectStudentId(), false);
        setAccountActive(false);

        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        assertState(false, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P14: DELETE de estudiante historico se rechaza sin cambios")
    void p14DeleteHistoricalStudentIsRejected() throws Exception {
        deleteProfile("estudiantes", scenario.subjectStudentId(), scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P15: DELETE del estudiante actual se rechaza sin cambios")
    void p15DeleteCurrentStudentIsRejected() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        deleteProfile("estudiantes", scenario.subjectStudentId(), scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ESTUDIANTE"));
    }

    @Test
    @DisplayName("P16: activar estudiante actual se rechaza si otro perfil sigue activo")
    void p16CurrentStudentActivationWithAnotherActiveProfileIsRejected() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        setProfileActive("estudiante", scenario.subjectStudentId(), false);
        setProfileActive("asesor", scenario.subjectAdvisorId(), true);

        patchState("estudiantes", scenario.subjectStudentId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P17: desactivar directamente el estudiante actual se rechaza")
    void p17CurrentStudentDirectDeactivationIsRejected() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        patchState("estudiantes", scenario.subjectStudentId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P18: DELETE del asesor actual se rechaza sin cambios")
    void p18DeleteCurrentAdvisorIsRejected() throws Exception {
        deleteProfile("asesores", scenario.subjectAdvisorId(), scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P19: activar asesor historico se rechaza sin cambios")
    void p19HistoricalAdvisorActivationIsRejected() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        patchState("asesores", scenario.subjectAdvisorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P20: desactivar asesor historico activo se rechaza sin sanear datos")
    void p20ActiveHistoricalAdvisorDeactivationIsRejected() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());
        setProfileActive("asesor", scenario.subjectAdvisorId(), true);

        patchState("asesores", scenario.subjectAdvisorId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P21: DELETE del asesor historico se rechaza sin cambios")
    void p21DeleteHistoricalAdvisorIsRejected() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        deleteProfile("asesores", scenario.subjectAdvisorId(), scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P22: asesor historico no rehabilita una cuenta suspendida")
    void p22HistoricalAdvisorDoesNotRestoreSuspendedAccount() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());
        setAccountActive(false);

        patchState("asesores", scenario.subjectAdvisorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(false, false, true, "ESTUDIANTE", scenario.studentRoleId());
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P23: activar asesor actual no habilita una cuenta suspendida")
    void p23CurrentAdvisorActivationDoesNotEnableAccount() throws Exception {
        setProfileActive("asesor", scenario.subjectAdvisorId(), false);
        setAccountActive(false);

        patchState("asesores", scenario.subjectAdvisorId(), true, scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        assertState(false, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P24: activar asesor actual se rechaza si otro perfil sigue activo")
    void p24CurrentAdvisorActivationWithAnotherActiveProfileIsRejected() throws Exception {
        setProfileActive("asesor", scenario.subjectAdvisorId(), false);
        setProfileActive("estudiante", scenario.subjectStudentId(), true);

        patchState("asesores", scenario.subjectAdvisorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, true, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P25: cambio explicito ASESOR-ESTUDIANTE-ASESOR conserva cuenta e historial")
    void p25ExplicitRoundTripToAdvisorRemainsAtomic() throws Exception {
        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isOk());

        patchProfileToAdvisor(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scenario.subjectUserId()))
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.perfilId").value(scenario.subjectAdvisorId()))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(2L, historyCount());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()))
                .andExpect(jsonPath("$.permisos[?(@ == '" + VER_ASESORES_MONITORES + "')]").exists())
                .andExpect(jsonPath("$.permisos[?(@ == 'Permiso exclusivo estudiante')]")
                        .doesNotExist());
    }

    @Test
    @DisplayName("P26: activar monitor historico se rechaza sin cambios")
    void p26HistoricalMonitorActivationIsRejected() throws Exception {
        patchState("monitores", scenario.subjectMonitorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertFalse(profileActive("monitor", scenario.subjectMonitorId()));
        assertFalse(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P27: desactivar monitor historico activo se rechaza sin sanear datos")
    void p27ActiveHistoricalMonitorDeactivationIsRejected() throws Exception {
        setProfileActive("monitor", scenario.subjectMonitorId(), true);

        patchState("monitores", scenario.subjectMonitorId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertTrue(profileActive("monitor", scenario.subjectMonitorId()));
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P28: PATCH y DELETE no desactivan el monitor actual")
    void p28CurrentMonitorDeactivationAndDeleteAreRejected() throws Exception {
        patchProfileToMonitor(scenario.adminUsername()).andExpect(status().isOk());

        patchState("monitores", scenario.subjectMonitorId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));
        deleteProfile("monitores", scenario.subjectMonitorId(), scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, false, "MONITOR", scenario.monitorRoleId());
        assertTrue(profileActive("monitor", scenario.subjectMonitorId()));
        assertFalse(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("MONITOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.monitorRoleId()))
                .andExpect(jsonPath("$.permisos[?(@ == 'Permiso exclusivo monitor')]").exists())
                .andExpect(jsonPath("$.permisos[?(@ == 'Permiso exclusivo conciliador')]")
                        .doesNotExist());
    }

    @Test
    @DisplayName("P29: activar monitor actual no habilita una cuenta suspendida")
    void p29CurrentMonitorActivationDoesNotEnableAccount() throws Exception {
        patchProfileToMonitor(scenario.adminUsername()).andExpect(status().isOk());
        setProfileActive("monitor", scenario.subjectMonitorId(), false);
        setAccountActive(false);

        patchState("monitores", scenario.subjectMonitorId(), true, scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        assertState(false, false, false, "MONITOR", scenario.monitorRoleId());
        assertTrue(profileActive("monitor", scenario.subjectMonitorId()));
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P30: activar monitor actual se rechaza si otro perfil sigue activo")
    void p30CurrentMonitorActivationWithAnotherActiveProfileIsRejected() throws Exception {
        patchProfileToMonitor(scenario.adminUsername()).andExpect(status().isOk());
        setProfileActive("monitor", scenario.subjectMonitorId(), false);
        setProfileActive("asesor", scenario.subjectAdvisorId(), true);

        patchState("monitores", scenario.subjectMonitorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "MONITOR", scenario.monitorRoleId());
        assertFalse(profileActive("monitor", scenario.subjectMonitorId()));
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P31: activar conciliador historico se rechaza sin cambios")
    void p31HistoricalConciliatorActivationIsRejected() throws Exception {
        patchState("conciliadores", scenario.subjectConciliatorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertFalse(profileActive("monitor", scenario.subjectMonitorId()));
        assertFalse(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P32: desactivar conciliador historico activo se rechaza sin sanear datos")
    void p32ActiveHistoricalConciliatorDeactivationIsRejected() throws Exception {
        setProfileActive("conciliador", scenario.subjectConciliatorId(), true);

        patchState("conciliadores", scenario.subjectConciliatorId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertTrue(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P33: PATCH y DELETE no desactivan el conciliador actual")
    void p33CurrentConciliatorDeactivationAndDeleteAreRejected() throws Exception {
        patchProfileToConciliator(scenario.adminUsername()).andExpect(status().isOk());

        patchState("conciliadores", scenario.subjectConciliatorId(), false, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));
        deleteProfile("conciliadores", scenario.subjectConciliatorId(), scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, false, false, "CONCILIADOR", scenario.conciliatorRoleId());
        assertTrue(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertFalse(profileActive("monitor", scenario.subjectMonitorId()));
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("CONCILIADOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.conciliatorRoleId()))
                .andExpect(jsonPath("$.permisos[?(@ == 'Permiso exclusivo conciliador')]").exists())
                .andExpect(jsonPath("$.permisos[?(@ == 'Permiso exclusivo monitor')]")
                        .doesNotExist());
    }

    @Test
    @DisplayName("P34: activar conciliador actual no habilita una cuenta suspendida")
    void p34CurrentConciliatorActivationDoesNotEnableAccount() throws Exception {
        patchProfileToConciliator(scenario.adminUsername()).andExpect(status().isOk());
        setProfileActive("conciliador", scenario.subjectConciliatorId(), false);
        setAccountActive(false);

        patchState("conciliadores", scenario.subjectConciliatorId(), true, scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        assertState(false, false, false, "CONCILIADOR", scenario.conciliatorRoleId());
        assertTrue(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(1L, historyCount());
        getMe(scenario.subjectUsername()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P35: activar conciliador actual se rechaza si otro perfil sigue activo")
    void p35CurrentConciliatorActivationWithAnotherActiveProfileIsRejected() throws Exception {
        patchProfileToConciliator(scenario.adminUsername()).andExpect(status().isOk());
        setProfileActive("conciliador", scenario.subjectConciliatorId(), false);
        setProfileActive("asesor", scenario.subjectAdvisorId(), true);

        patchState("conciliadores", scenario.subjectConciliatorId(), true, scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertState(true, true, false, "CONCILIADOR", scenario.conciliatorRoleId());
        assertFalse(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P36: transiciones explicitas con monitor y conciliador conservan historial")
    void p36ExplicitMonitorAndConciliatorTransitionsRemainAtomic() throws Exception {
        patchProfileToMonitor(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("MONITOR"))
                .andExpect(jsonPath("$.perfilId").value(scenario.subjectMonitorId()));
        patchProfileToConciliator(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("CONCILIADOR"))
                .andExpect(jsonPath("$.perfilId").value(scenario.subjectConciliatorId()));
        patchProfileToAdvisor(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertFalse(profileActive("monitor", scenario.subjectMonitorId()));
        assertFalse(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(3L, historyCount());
    }

    @Test
    @DisplayName("P37: consulta operativa de monitor bloquea cambio explicito y revierte todo")
    void p37OperationalMonitorConsultationBlocksExplicitChange() throws Exception {
        patchProfileToMonitor(scenario.adminUsername()).andExpect(status().isOk());
        transaction.executeWithoutResult(
                ignored -> createOperationalMonitorConsultation(scenario.subjectMonitorId()));

        patchProfileToConciliator(scenario.adminUsername())
                .andExpect(status().isBadRequest());

        assertState(true, false, false, "MONITOR", scenario.monitorRoleId());
        assertTrue(profileActive("monitor", scenario.subjectMonitorId()));
        assertFalse(profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P38: activar administrativo historico se rechaza sin cambios")
    void p38HistoricalAdministratorActivationIsRejected() throws Exception {
        patchState("administrativos", scenario.subjectAdministratorId(), true,
                scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertFalse(profileActive("administrativo", scenario.subjectAdministratorId()));
        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
        getMe(scenario.subjectUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"))
                .andExpect(jsonPath("$.rolId").value(scenario.advisorRoleId()))
                .andExpect(jsonPath("$.permisos[?(@ == '" + VER_ASESORES_MONITORES + "')]").exists());
    }

    @Test
    @DisplayName("P39: desactivar administrativo historico activo se rechaza sin sanear datos")
    void p39ActiveHistoricalAdministratorDeactivationIsRejected() throws Exception {
        setProfileActive("administrativo", scenario.subjectAdministratorId(), true);

        patchState("administrativos", scenario.subjectAdministratorId(), false,
                scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertTrue(profileActive("administrativo", scenario.subjectAdministratorId()));
        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P40: PATCH y DELETE no desactivan el administrativo actual")
    void p40CurrentAdministratorDeactivationAndDeleteAreRejected() throws Exception {
        patchProfileToAdministrator(scenario.adminUsername()).andExpect(status().isOk());

        patchState("administrativos", scenario.subjectAdministratorId(), false,
                scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));
        deleteProfile("administrativos", scenario.subjectAdministratorId(),
                scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        AccountState account = loadAccountState(scenario.subjectUserId());
        assertTrue(account.active());
        assertEquals("ADMINISTRATIVO", account.currentProfile());
        assertEquals(scenario.adminRoleId(), account.roleId());
        assertTrue(profileActive("administrativo", scenario.subjectAdministratorId()));
        assertFalse(profileActive("asesor", scenario.subjectAdvisorId()));
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P41: activar administrativo actual no habilita una cuenta suspendida")
    void p41CurrentAdministratorActivationDoesNotEnableAccount() throws Exception {
        patchProfileToAdministrator(scenario.adminUsername()).andExpect(status().isOk());
        setProfileActive("administrativo", scenario.subjectAdministratorId(), false);
        setAccountActive(false);

        patchState("administrativos", scenario.subjectAdministratorId(), true,
                scenario.adminUsername())
                .andExpect(status().isOk());

        AccountState account = loadAccountState(scenario.subjectUserId());
        assertFalse(account.active());
        assertEquals("ADMINISTRATIVO", account.currentProfile());
        assertEquals(scenario.adminRoleId(), account.roleId());
        assertTrue(profileActive("administrativo", scenario.subjectAdministratorId()));
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P42: activar administrativo actual se rechaza si otro perfil sigue activo")
    void p42CurrentAdministratorActivationWithAnotherActiveProfileIsRejected() throws Exception {
        patchProfileToAdministrator(scenario.adminUsername()).andExpect(status().isOk());
        setProfileActive("administrativo", scenario.subjectAdministratorId(), false);
        setProfileActive("asesor", scenario.subjectAdvisorId(), true);

        patchState("administrativos", scenario.subjectAdministratorId(), true,
                scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertFalse(profileActive("administrativo", scenario.subjectAdministratorId()));
        assertTrue(profileActive("asesor", scenario.subjectAdvisorId()));
        AccountState account = loadAccountState(scenario.subjectUserId());
        assertEquals("ADMINISTRATIVO", account.currentProfile());
        assertEquals(scenario.adminRoleId(), account.roleId());
        assertEquals(1L, historyCount());
    }

    @Test
    @DisplayName("P43: cambio explicito hacia y desde ADMINISTRATIVO conserva historial")
    void p43ExplicitAdministratorRoundTripRemainsAtomic() throws Exception {
        patchProfileToAdministrator(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ADMINISTRATIVO"))
                .andExpect(jsonPath("$.perfilId").value(scenario.subjectAdministratorId()));
        patchProfileToAdvisor(scenario.adminUsername())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoPerfil").value("ASESOR"));

        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertFalse(profileActive("administrativo", scenario.subjectAdministratorId()));
        assertEquals(2L, historyCount());
    }

    @Test
    @DisplayName("P44: continuidad rechaza que el ultimo administrador cambie de perfil")
    void p44LastAdministratorExplicitChangeRollsBackCompletely() throws Exception {
        patchActorProfileToAdvisor()
                .andExpect(status().isConflict());

        AccountState account = loadAccountState(scenario.adminUserId());
        assertTrue(account.active());
        assertEquals("ADMINISTRATIVO", account.currentProfile());
        assertEquals(scenario.adminRoleId(), account.roleId());
        assertTrue(profileActive("administrativo", scenario.adminProfileId()));
        assertEquals(0L, historyCount(scenario.adminUserId()));
    }

    @Test
    @DisplayName("P45: dos cambios explicitos concurrentes se serializan por cuenta")
    void p45ConcurrentExplicitChangesOnSameAccountAreSerialized() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("student");
        accountLockCoordinator.prepare("monitor");

        try {
            Future<MvcResult> student = submitWithAccountLockLabel(
                    executor,
                    "student",
                    () -> patchProfileToStudent(scenario.adminUsername()).andReturn());
            accountLockCoordinator.awaitLocked("student");

            Future<MvcResult> monitor = submitWithAccountLockLabel(
                    executor,
                    "monitor",
                    () -> patchProfileToMonitor(scenario.adminUsername()).andReturn());

            accountLockCoordinator.release("student");
            assertEquals(200, student.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            accountLockCoordinator.awaitLocked("monitor");
            accountLockCoordinator.release("monitor");
            assertEquals(200, monitor.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            assertOnlyProfileActive(TipoPerfilUsuario.MONITOR);
            AccountState account = loadAccountState(scenario.subjectUserId());
            assertEquals("MONITOR", account.currentProfile());
            assertEquals(scenario.monitorRoleId(), account.roleId());
            assertEquals(2L, historyCount());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P46: activacion historica concurrente espera el cambio explicito y se rechaza")
    void p46HistoricalActivationConcurrentWithExplicitChangeIsRejected() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("explicit");
        accountLockCoordinator.prepare("direct");

        try {
            Future<MvcResult> explicit = submitWithAccountLockLabel(
                    executor,
                    "explicit",
                    () -> patchProfileToStudent(scenario.adminUsername()).andReturn());
            accountLockCoordinator.awaitLocked("explicit");

            Future<MvcResult> direct = submitWithAccountLockLabel(
                    executor,
                    "direct",
                    () -> patchState("monitores", scenario.subjectMonitorId(), true,
                            scenario.adminUsername()).andReturn());

            accountLockCoordinator.release("explicit");
            assertEquals(200, explicit.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            accountLockCoordinator.awaitLocked("direct");
            accountLockCoordinator.release("direct");
            MvcResult rejected = direct.get(10, TimeUnit.SECONDS);
            assertEquals(409, rejected.getResponse().getStatus());
            assertConflictCode(rejected);

            assertOnlyProfileActive(TipoPerfilUsuario.ESTUDIANTE);
            assertEquals(1L, historyCount());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P47: activacion del perfil actual precede atomicamente al cambio concurrente")
    void p47CurrentActivationConcurrentWithExplicitChangeIsAtomic() throws Exception {
        setProfileActive("asesor", scenario.subjectAdvisorId(), false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("activation");
        accountLockCoordinator.prepare("explicit");

        try {
            Future<MvcResult> activation = submitWithAccountLockLabel(
                    executor,
                    "activation",
                    () -> patchState("asesores", scenario.subjectAdvisorId(), true,
                            scenario.adminUsername()).andReturn());
            accountLockCoordinator.awaitLocked("activation");

            Future<MvcResult> explicit = submitWithAccountLockLabel(
                    executor,
                    "explicit",
                    () -> patchProfileToStudent(scenario.adminUsername()).andReturn());

            accountLockCoordinator.release("activation");
            assertEquals(200, activation.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            accountLockCoordinator.awaitLocked("explicit");
            accountLockCoordinator.release("explicit");
            assertEquals(200, explicit.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            assertOnlyProfileActive(TipoPerfilUsuario.ESTUDIANTE);
            assertEquals(1L, historyCount());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P48: suspension concurrente prevalece sobre activacion directa de perfil")
    void p48ConcurrentAccountSuspensionIsNotUndoneByProfileActivation() throws Exception {
        setProfileActive("asesor", scenario.subjectAdvisorId(), false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("activation");

        try {
            Future<MvcResult> activation = submitWithAccountLockLabel(
                    executor,
                    "activation",
                    () -> patchState("asesores", scenario.subjectAdvisorId(), true,
                            scenario.adminUsername()).andReturn());
            accountLockCoordinator.awaitLocked("activation");

            Future<MvcResult> suspension = executor.submit(
                    () -> patchAccountState(false, scenario.adminUsername()).andReturn());

            assertThrows(TimeoutException.class,
                    () -> suspension.get(300, TimeUnit.MILLISECONDS));
            accountLockCoordinator.release("activation");

            assertEquals(200, activation.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(200, suspension.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            AccountState account = loadAccountState(scenario.subjectUserId());
            assertFalse(account.active());
            assertEquals("ASESOR", account.currentProfile());
            assertEquals(scenario.advisorRoleId(), account.roleId());
            assertTrue(profileActive("asesor", scenario.subjectAdvisorId()));
            assertEquals(0L, historyCount());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P49: transicion administrativa y retiro de directora no se interbloquean")
    void p49AdministrativeTransitionPreservesContinuityWithoutDeadlock() throws Exception {
        patchProfileToAdministrator(scenario.adminUsername()).andExpect(status().isOk());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("leave-admin");

        try {
            Future<MvcResult> transition = submitWithAccountLockLabel(
                    executor,
                    "leave-admin",
                    () -> patchProfileToStudent(scenario.adminUsername()).andReturn());
            accountLockCoordinator.awaitLocked("leave-admin");

            Future<MvcResult> removeDirector = executor.submit(
                    () -> patchDirector(false, scenario.adminUsername()).andReturn());

            assertThrows(TimeoutException.class,
                    () -> removeDirector.get(300, TimeUnit.MILLISECONDS));
            accountLockCoordinator.release("leave-admin");

            assertEquals(200, transition.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(409, removeDirector.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            assertOnlyProfileActive(TipoPerfilUsuario.ESTUDIANTE);
            assertTrue(administratorDirector(scenario.adminProfileId()));
            assertEquals(2L, historyCount());
            assertEquals(0L, historyCount(scenario.adminUserId()));
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P50: cambio explicito rechaza cuentas con multiples perfiles activos")
    void p50ExplicitChangeRejectsPreexistingMultipleActiveProfiles() throws Exception {
        setProfileActive("monitor", scenario.subjectMonitorId(), true);

        patchProfileToStudent(scenario.adminUsername())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles.codigo")
                        .value("CONFLICTO_OPERACION_PERFIL"));

        assertTrue(profileActive("asesor", scenario.subjectAdvisorId()));
        assertTrue(profileActive("monitor", scenario.subjectMonitorId()));
        assertFalse(profileActive("estudiante", scenario.subjectStudentId()));
        assertState(true, true, false, "ASESOR", scenario.advisorRoleId());
        assertEquals(0L, historyCount());
    }

    @Test
    @DisplayName("P51: cambio a administrativo y suspension se serializan sin perder la transicion")
    void p51AdministrativeTransitionConcurrentWithSuspensionIsSerialized() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("explicit-admin");
        CountDownLatch suspensionStarted = new CountDownLatch(1);

        try {
            Future<MvcResult> transition = submitWithAccountLockLabel(
                    executor,
                    "explicit-admin",
                    () -> patchProfileToAdministrator(scenario.adminUsername()).andReturn());
            accountLockCoordinator.awaitLocked("explicit-admin");

            Future<MvcResult> suspension = executor.submit(() -> {
                suspensionStarted.countDown();
                return patchAccountState(false, scenario.adminUsername()).andReturn();
            });

            assertTrue(suspensionStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> suspension.get(300, TimeUnit.MILLISECONDS));

            accountLockCoordinator.release("explicit-admin");

            assertEquals(200, transition.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(200, suspension.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            AccountState account = loadAccountState(scenario.subjectUserId());
            assertFalse(account.active());
            assertEquals("ADMINISTRATIVO", account.currentProfile());
            assertEquals(scenario.adminRoleId(), account.roleId());
            assertOnlyProfileActive(TipoPerfilUsuario.ADMINISTRATIVO);
            assertEquals(1L, historyCount());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P52: cambio de password y transicion de perfil preservan todas las actualizaciones")
    void p52PasswordChangeConcurrentWithProfileTransitionIsSerialized() throws Exception {
        String currentPassword = "Current-Password-52";
        String newPassword = "New-Password-52";
        setPassword(currentPassword);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("password-change");
        CountDownLatch transitionStarted = new CountDownLatch(1);

        try {
            Future<MvcResult> passwordChange = submitWithAccountLockLabel(
                    executor,
                    "password-change",
                    () -> patchPassword(currentPassword, newPassword).andReturn());
            accountLockCoordinator.awaitLocked("password-change");

            Future<MvcResult> transition = executor.submit(() -> {
                transitionStarted.countDown();
                return patchProfileToAdministrator(scenario.adminUsername()).andReturn();
            });

            assertTrue(transitionStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> transition.get(300, TimeUnit.MILLISECONDS));

            accountLockCoordinator.release("password-change");

            assertEquals(204, passwordChange.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(200, transition.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            assertTrue(passwordEncoder.matches(newPassword, loadPasswordHash()));
            AccountState account = loadAccountState(scenario.subjectUserId());
            assertTrue(account.active());
            assertEquals("ADMINISTRATIVO", account.currentProfile());
            assertEquals(scenario.adminRoleId(), account.roleId());
            assertOnlyProfileActive(TipoPerfilUsuario.ADMINISTRATIVO);
            assertEquals(1L, historyCount());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("P53: restablecimiento de password y suspension preservan todas las actualizaciones")
    void p53PasswordResetConcurrentWithSuspensionIsSerialized() throws Exception {
        String currentPassword = "Current-Password-53";
        String newPassword = "New-Password-53";
        setPassword(currentPassword);
        String resetToken = createPasswordResetToken();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        accountLockCoordinator.prepare("password-reset");
        CountDownLatch suspensionStarted = new CountDownLatch(1);

        try {
            Future<MvcResult> passwordReset = submitWithAccountLockLabel(
                    executor,
                    "password-reset",
                    () -> postPasswordReset(resetToken, newPassword).andReturn());
            accountLockCoordinator.awaitLocked("password-reset");

            Future<MvcResult> suspension = executor.submit(() -> {
                suspensionStarted.countDown();
                return patchAccountState(false, scenario.adminUsername()).andReturn();
            });

            assertTrue(suspensionStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> suspension.get(300, TimeUnit.MILLISECONDS));

            accountLockCoordinator.release("password-reset");

            assertEquals(200, passwordReset.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(200, suspension.get(10, TimeUnit.SECONDS).getResponse().getStatus());

            assertTrue(passwordEncoder.matches(newPassword, loadPasswordHash()));
            AccountState account = loadAccountState(scenario.subjectUserId());
            assertFalse(account.active());
            assertEquals("ASESOR", account.currentProfile());
            assertEquals(scenario.advisorRoleId(), account.roleId());
            assertOnlyProfileActive(TipoPerfilUsuario.ASESOR);
            assertEquals(0L, historyCount());
            postPasswordReset(resetToken, "Another-Password-53")
                    .andExpect(status().isBadRequest());
        } finally {
            accountLockCoordinator.releaseAll();
            shutdown(executor);
        }
    }

    private org.springframework.test.web.servlet.ResultActions getMe(String username) throws Exception {
        return authenticatedGet("/api/auth/me", username);
    }

    private org.springframework.test.web.servlet.ResultActions deleteProfile(
            String resource,
            Long id,
            String username) throws Exception {
        Credentials credentials = credentials(username);
        return mockMvc.perform(delete("/api/" + resource + "/" + id)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private void setProfileActive(String table, Long id, boolean active) {
        transaction.executeWithoutResult(ignored -> jdbcTemplate.update(
                "UPDATE " + SCHEMA + "." + table + " SET activo = ? WHERE id = ?",
                active,
                id));
    }

    private void setAccountActive(boolean active) {
        transaction.executeWithoutResult(ignored -> jdbcTemplate.update(
                "UPDATE " + SCHEMA + ".usuario_sistema SET activo = ? WHERE id = ?",
                active,
                scenario.subjectUserId()));
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

    private org.springframework.test.web.servlet.ResultActions patchAccountState(
            boolean active,
            String username) throws Exception {
        Credentials credentials = credentials(username);
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.subjectUserId()
                        + "/activo")
                .param("activo", Boolean.toString(active))
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions patchPassword(
            String currentPassword,
            String newPassword) throws Exception {
        Credentials credentials = credentials(scenario.subjectUsername());
        String json = jsonMapper.writeValueAsString(java.util.Map.of(
                "passwordActual", currentPassword,
                "passwordNueva", newPassword));
        return mockMvc.perform(patch("/api/auth/cambiar-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions postPasswordReset(
            String token,
            String newPassword) throws Exception {
        Credentials credentials = credentials(scenario.adminUsername());
        String json = jsonMapper.writeValueAsString(java.util.Map.of(
                "token", token,
                "passwordNueva", newPassword,
                "confirmarPassword", newPassword));
        return mockMvc.perform(post("/api/auth/restablecer-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private void setPassword(String rawPassword) {
        transaction.executeWithoutResult(ignored -> jdbcTemplate.update(
                "UPDATE " + SCHEMA + ".usuario_sistema SET password_hash = ? WHERE id = ?",
                passwordEncoder.encode(rawPassword),
                scenario.subjectUserId()));
    }

    private String loadPasswordHash() {
        return jdbcTemplate.queryForObject(
                "SELECT password_hash FROM " + SCHEMA + ".usuario_sistema WHERE id = ?",
                String.class,
                scenario.subjectUserId());
    }

    private String createPasswordResetToken() {
        return transaction.execute(status -> {
            UsuarioSistema usuario = entityManager.find(UsuarioSistema.class, scenario.subjectUserId());
            return passwordResetTokenService.crearTokenParaUsuario(usuario).token();
        });
    }

    private org.springframework.test.web.servlet.ResultActions patchDirector(
            boolean director,
            String username) throws Exception {
        Credentials credentials = credentials(username);
        return mockMvc.perform(patch("/api/administrativos/" + scenario.adminProfileId()
                        + "/directora")
                .param("directora", Boolean.toString(director))
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private Future<MvcResult> submitWithAccountLockLabel(
            ExecutorService executor,
            String label,
            Callable<MvcResult> operation) {
        return executor.submit(() -> accountLockCoordinator.execute(label, operation));
    }

    private void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    private void assertConflictCode(MvcResult result) throws Exception {
        JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("CONFLICTO_OPERACION_PERFIL",
                body.get("detalles").get("codigo").asText());
    }

    private void assertOnlyProfileActive(TipoPerfilUsuario activeProfile) {
        assertEquals(activeProfile == TipoPerfilUsuario.ADMINISTRATIVO,
                profileActive("administrativo", scenario.subjectAdministratorId()));
        assertEquals(activeProfile == TipoPerfilUsuario.ASESOR,
                profileActive("asesor", scenario.subjectAdvisorId()));
        assertEquals(activeProfile == TipoPerfilUsuario.CONCILIADOR,
                profileActive("conciliador", scenario.subjectConciliatorId()));
        assertEquals(activeProfile == TipoPerfilUsuario.ESTUDIANTE,
                profileActive("estudiante", scenario.subjectStudentId()));
        assertEquals(activeProfile == TipoPerfilUsuario.MONITOR,
                profileActive("monitor", scenario.subjectMonitorId()));
    }

    private boolean administratorDirector(Long administratorId) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT directora FROM " + SCHEMA + ".administrativo WHERE id = ?",
                Boolean.class,
                administratorId);
        return Boolean.TRUE.equals(value);
    }

    private org.springframework.test.web.servlet.ResultActions patchProfileToStudent(String username)
            throws Exception {
        Credentials credentials = credentials(username);
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.studentRoleId()),
                java.util.Map.entry("motivo", "Validacion sintetica de perfil operativo"),
                java.util.Map.entry("nombre", "Identidad compartida de perfil operativo"),
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

    private org.springframework.test.web.servlet.ResultActions patchProfileToAdvisor(String username)
            throws Exception {
        Credentials credentials = credentials(username);
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.advisorRoleId()),
                java.util.Map.entry("motivo", "Retorno sintetico al perfil asesor"),
                java.util.Map.entry("nombre", "Identidad compartida asesor"),
                java.util.Map.entry("tipoDocumentoId", scenario.documentTypeId()),
                java.util.Map.entry("documento", scenario.subjectDocument()),
                java.util.Map.entry("telefono", scenario.subjectPhone()),
                java.util.Map.entry("usuario", scenario.subjectCode()),
                java.util.Map.entry("codigo", scenario.subjectCode()),
                java.util.Map.entry("sedeId", scenario.campusId()),
                java.util.Map.entry("areaId", scenario.areaId())));
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.subjectUserId()
                        + "/perfil/asesor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions patchProfileToMonitor(String username)
            throws Exception {
        Credentials credentials = credentials(username);
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.monitorRoleId()),
                java.util.Map.entry("motivo", "Cambio sintetico al perfil monitor"),
                java.util.Map.entry("nombre", "Identidad compartida monitor"),
                java.util.Map.entry("tipoDocumentoId", scenario.documentTypeId()),
                java.util.Map.entry("documento", scenario.subjectDocument()),
                java.util.Map.entry("telefono", scenario.subjectPhone()),
                java.util.Map.entry("usuario", scenario.subjectCode()),
                java.util.Map.entry("codigo", scenario.subjectCode()),
                java.util.Map.entry("sedeId", scenario.campusId())));
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.subjectUserId()
                        + "/perfil/monitor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions patchProfileToConciliator(String username)
            throws Exception {
        Credentials credentials = credentials(username);
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.conciliatorRoleId()),
                java.util.Map.entry("motivo", "Cambio sintetico al perfil conciliador"),
                java.util.Map.entry("nombre", "Identidad compartida conciliador"),
                java.util.Map.entry("tipoDocumentoId", scenario.documentTypeId()),
                java.util.Map.entry("documento", scenario.subjectDocument()),
                java.util.Map.entry("telefono", scenario.subjectPhone()),
                java.util.Map.entry("usuario", scenario.subjectCode()),
                java.util.Map.entry("codigo", scenario.subjectCode()),
                java.util.Map.entry("sedeId", scenario.campusId()),
                java.util.Map.entry("tipoConciliador", "INTERNO")));
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.subjectUserId()
                        + "/perfil/conciliador")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions patchProfileToAdministrator(String username)
            throws Exception {
        Credentials credentials = credentials(username);
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.adminRoleId()),
                java.util.Map.entry("motivo", "Cambio sintetico al perfil administrativo"),
                java.util.Map.entry("nombre", "Identidad compartida administrativa"),
                java.util.Map.entry("tipoDocumentoId", scenario.documentTypeId()),
                java.util.Map.entry("documento", scenario.subjectDocument()),
                java.util.Map.entry("telefono", scenario.subjectPhone()),
                java.util.Map.entry("usuario", scenario.subjectCode()),
                java.util.Map.entry("codigo", scenario.subjectCode()),
                java.util.Map.entry("sedeId", scenario.campusId()),
                java.util.Map.entry("directora", false)));
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.subjectUserId()
                        + "/perfil/administrativo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .cookie(credentials.authCookie(), credentials.csrfCookie())
                .header(credentials.csrfHeader(), credentials.csrfToken()));
    }

    private org.springframework.test.web.servlet.ResultActions patchActorProfileToAdvisor()
            throws Exception {
        Credentials credentials = credentials(scenario.adminUsername());
        String json = jsonMapper.writeValueAsString(java.util.Map.ofEntries(
                java.util.Map.entry("rolId", scenario.advisorRoleId()),
                java.util.Map.entry("motivo", "Prueba de continuidad administrativa"),
                java.util.Map.entry("nombre", "Administrador actor"),
                java.util.Map.entry("tipoDocumentoId", scenario.documentTypeId()),
                java.util.Map.entry("documento", "ACTOR-DOC-" + scenario.adminUserId()),
                java.util.Map.entry("telefono", "ACTOR-TEL-" + scenario.adminUserId()),
                java.util.Map.entry("usuario", "actor-" + scenario.adminUserId()),
                java.util.Map.entry("codigo", "actor-" + scenario.adminUserId()),
                java.util.Map.entry("sedeId", scenario.campusId()),
                java.util.Map.entry("areaId", scenario.areaId())));
        return mockMvc.perform(patch("/api/usuarios-sistema/" + scenario.adminUserId()
                        + "/perfil/asesor")
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
        return historyCount(scenario.subjectUserId());
    }

    private long historyCount(Long userId) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + SCHEMA
                        + ".usuario_cambio_perfil_historial WHERE usuario_sistema_id = ?",
                Long.class, userId);
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
        Permiso accessAdministration = permission(ACCEDER_ADMINISTRACION);
        Permiso accessRoles = permission(ACCEDER_ROLES);
        Permiso manageRoles = permission(GESTIONAR_ROLES);
        Permiso managePermissions = permission(GESTIONAR_PERMISOS);
        Permiso manageAdministrators = permission(GESTIONAR_ADMINISTRADORES);
        Permiso seeAdvisors = permission(VER_ASESORES_MONITORES);
        Permiso seeStudents = permission(VER_ESTUDIANTES);
        Permiso studentOnly = permission("Permiso exclusivo estudiante");
        Permiso monitorOnly = permission("Permiso exclusivo monitor");
        Permiso conciliatorOnly = permission("Permiso exclusivo conciliador");

        Rol adminRole = role("Admin de prueba " + suffix, TipoPerfilUsuario.ADMINISTRATIVO,
                Set.of(manageUsers, assignRoles, manageAdvisors,
                        accessAdministration, accessRoles, manageRoles,
                        managePermissions, manageAdministrators));
        Rol advisorRole = role("Asesor de prueba " + suffix, TipoPerfilUsuario.ASESOR,
                Set.of(seeAdvisors, seeStudents));
        Rol studentRole = role("Estudiante de prueba " + suffix, TipoPerfilUsuario.ESTUDIANTE,
                Set.of(studentOnly));
        Rol monitorRole = role("Monitor de prueba " + suffix, TipoPerfilUsuario.MONITOR,
                Set.of(monitorOnly));
        Rol conciliatorRole = role("Conciliador de prueba " + suffix, TipoPerfilUsuario.CONCILIADOR,
                Set.of(conciliatorOnly));

        UsuarioSistema admin = user("admin." + suffix + "@integration.test",
                TipoPerfilUsuario.ADMINISTRATIVO, adminRole, true);
        Administrativo adminProfile = createAdministrator(
                "admin-" + suffix, campus, documentType, admin, true, true);

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
        Monitor subjectMonitor = createMonitor("mon-" + suffix, campus,
                documentType, subject, false);
        Conciliador subjectConciliator = createConciliator("con-" + suffix,
                campus, documentType, subject, false);
        Administrativo subjectAdministrator = createAdministrator(
                "history-" + suffix, campus, documentType, subject, false, false);

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
        return new Scenario(admin.getId(), admin.getUsername(), adminProfile.getId(),
                subject.getId(), subject.getUsername(),
                subjectAdvisor.getId(), subjectStudent.getId(), subjectMonitor.getId(),
                subjectConciliator.getId(), subjectAdministrator.getId(), academicAdvisor.getId(),
                independentAdvisor.getId(), independentStudent.getId(), independentStudentUser.getId(),
                adminRole.getId(), advisorRole.getId(), studentRole.getId(),
                monitorRole.getId(), conciliatorRole.getId(),
                campus.getId(), area.getId(), documentType.getId(),
                subjectDocument, subjectPhone, subjectCode);
    }

    private Permiso permission(String name) {
        return entityManager.createQuery("SELECT p FROM Permiso p WHERE p.nombre = :name", Permiso.class)
                .setParameter("name", name).getResultStream().findFirst().orElseGet(() -> {
                    Permiso permission = new Permiso();
                    permission.setNombre(name);
                    permission.setDescripcion("Permiso sintetico de prueba");
                    entityManager.persist(permission);
                    return permission;
                });
    }

    private Rol role(String name, TipoPerfilUsuario profile, Set<Permiso> permissions) {
        Rol role = new Rol();
        role.setNombre(name);
        role.setDescripcion("Rol sintetico de prueba");
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

    private Monitor createMonitor(String suffix, Sede campus, TipoDocumento documentType,
            UsuarioSistema user, boolean active) {
        Monitor monitor = new Monitor();
        monitor.setNombre("Monitor " + suffix);
        monitor.setTipoDocumento(documentType);
        monitor.setDocumento("MON-DOC-" + suffix);
        monitor.setEmail(user.getUsername());
        monitor.setTelefono("MON-TEL-" + suffix);
        monitor.setUsuario("mon-" + suffix);
        monitor.setCodigo("MON-COD-" + suffix);
        monitor.setSede(campus);
        monitor.setUsuarioSistema(user);
        monitor.setActivo(active);
        entityManager.persist(monitor);
        return monitor;
    }

    private Conciliador createConciliator(String suffix, Sede campus,
            TipoDocumento documentType, UsuarioSistema user, boolean active) {
        Conciliador conciliador = new Conciliador();
        conciliador.setNombre("Conciliador " + suffix);
        conciliador.setTipoDocumento(documentType);
        conciliador.setDocumento("CON-DOC-" + suffix);
        conciliador.setEmail(user.getUsername());
        conciliador.setTelefono("CON-TEL-" + suffix);
        conciliador.setUsuario("con-" + suffix);
        conciliador.setCodigo("CON-COD-" + suffix);
        conciliador.setSede(campus);
        conciliador.setTipoConciliador(TipoConciliador.INTERNO);
        conciliador.setUsuarioSistema(user);
        conciliador.setActivo(active);
        entityManager.persist(conciliador);
        return conciliador;
    }

    private Administrativo createAdministrator(
            String suffix,
            Sede campus,
            TipoDocumento documentType,
            UsuarioSistema user,
            boolean active,
            boolean director) {
        Administrativo admin = new Administrativo();
        admin.setNombre("Administrador " + suffix);
        admin.setTipoDocumento(documentType);
        admin.setDocumento("ADM-DOC-" + suffix);
        admin.setEmail(user.getUsername());
        admin.setTelefono("ADM-TEL-" + suffix);
        admin.setUsuario("adm-" + suffix);
        admin.setCodigo("ADM-COD-" + suffix);
        admin.setSede(campus);
        admin.setDirectora(director);
        admin.setUsuarioSistema(user);
        admin.setActivo(active);
        entityManager.persist(admin);
        return admin;
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
        consultation.setDescripcion("Consulta operativa de prueba");
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

    private void createOperationalMonitorConsultation(Long monitorId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Area area = entityManager.find(Area.class, scenario.areaId());
        Sede campus = entityManager.find(Sede.class, scenario.campusId());
        Tema topic = new Tema();
        topic.setNombre("Tema monitor " + suffix);
        topic.setArea(area);
        entityManager.persist(topic);
        Persona person = createPerson("monitor-" + suffix);
        Consulta consultation = new Consulta();
        consultation.setFecha(LocalDate.of(2026, 10, 11));
        consultation.setDescripcion("Consulta operativa de monitor");
        consultation.setHechos("Hechos sinteticos");
        consultation.setPretensiones("Pretensiones sinteticas");
        consultation.setConceptoJuridico("Concepto sintetico");
        consultation.setTramite("Asesoria");
        consultation.setEstado(EstadoConsulta.ACTIVO);
        consultation.setPersona(person);
        consultation.setSede(campus);
        consultation.setArea(area);
        consultation.setTema(topic);
        consultation.setMonitor(entityManager.find(Monitor.class, monitorId));
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

    @TestConfiguration(proxyBeanMethods = false)
    static class LockCoordinationConfig {

        @Bean
        AccountLockCoordinator accountLockCoordinator() {
            return new AccountLockCoordinator();
        }

        @Bean
        AccountLockAspect accountLockAspect(AccountLockCoordinator coordinator) {
            return new AccountLockAspect(coordinator);
        }
    }

    static class AccountLockCoordinator {

        private final ConcurrentHashMap<String, Gate> gates = new ConcurrentHashMap<>();
        private final ThreadLocal<String> currentLabel = new ThreadLocal<>();

        void prepare(String label) {
            gates.put(label, new Gate(new CountDownLatch(1), new CountDownLatch(1)));
        }

        <T> T execute(String label, Callable<T> operation) throws Exception {
            currentLabel.set(label);
            try {
                return operation.call();
            } finally {
                currentLabel.remove();
            }
        }

        void afterAccountLock() {
            String label = currentLabel.get();
            if (label == null) {
                return;
            }

            Gate gate = gates.get(label);
            if (gate == null) {
                return;
            }

            gate.locked().countDown();
            await(gate.release(), "liberacion del bloqueo de cuenta " + label);
        }

        void awaitLocked(String label) {
            Gate gate = gates.get(label);
            if (gate == null) {
                throw new IllegalStateException("No existe coordinacion para " + label);
            }
            await(gate.locked(), "adquisicion del bloqueo de cuenta " + label);
        }

        void release(String label) {
            Gate gate = gates.get(label);
            if (gate != null) {
                gate.release().countDown();
            }
        }

        void releaseAll() {
            gates.values().forEach(gate -> gate.release().countDown());
            gates.clear();
        }

        private void await(CountDownLatch latch, String operation) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout esperando " + operation);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupcion esperando " + operation, ex);
            }
        }

        private record Gate(CountDownLatch locked, CountDownLatch release) {}
    }

    @Aspect
    static class AccountLockAspect {

        private final AccountLockCoordinator coordinator;

        AccountLockAspect(AccountLockCoordinator coordinator) {
            this.coordinator = coordinator;
        }

        @Around("execution(* co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository.findByIdForUpdate(..))")
        Object coordinateAccountLock(ProceedingJoinPoint joinPoint) throws Throwable {
            Object result = joinPoint.proceed();
            coordinator.afterAccountLock();
            return result;
        }
    }

    private record Credentials(Cookie authCookie, Cookie csrfCookie, String csrfHeader, String csrfToken) {}
    private record AccountState(boolean active, String currentProfile, Long roleId) {}
    private record Scenario(Long adminUserId, String adminUsername, Long adminProfileId,
            Long subjectUserId, String subjectUsername,
            Long subjectAdvisorId, Long subjectStudentId, Long subjectMonitorId,
            Long subjectConciliatorId, Long subjectAdministratorId, Long academicAdvisorId,
            Long independentAdvisorId, Long independentStudentId, Long independentStudentUserId,
            Long adminRoleId, Long advisorRoleId, Long studentRoleId,
            Long monitorRoleId, Long conciliatorRoleId,
            Long campusId, Long areaId, Long documentTypeId,
            String subjectDocument, String subjectPhone, String subjectCode) {}
}
