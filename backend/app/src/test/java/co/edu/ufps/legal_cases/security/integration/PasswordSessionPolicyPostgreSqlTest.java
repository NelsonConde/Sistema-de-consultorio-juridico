package co.edu.ufps.legal_cases.security.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.perfil.Administrativo;
import co.edu.ufps.legal_cases.security.model.access.CodigoRolBase;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.auth.password.PasswordResetTokenService;
import co.edu.ufps.legal_cases.security.service.jwt.JwtService;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordSessionPolicyPostgreSqlTest
        extends PostgreSqlIntegrationTest {

    private static final String INITIAL_PASSWORD = "clave-inicial";
    private static final String CHANGED_PASSWORD = "clave-cambiada";
    private static final String RESET_PASSWORD = "clave-recuperada";

    /*
     * Testcontainers 2.x requiere iniciar explícitamente el contenedor
     * heredado antes de que Spring resuelva las propiedades dinámicas.
     * Se reutiliza exactamente el mismo contenedor definido en la clase base.
     */
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
    private JsonMapper jsonMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private UsuarioSistemaRepository usuarioSistemaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final String executionId =
            UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10);

    private String username;
    private Long usuarioId;

    @BeforeAll
    void prepareScenario() throws Exception {
        assertTrue(
                POSTGRESQL.isRunning(),
                "PostgreSQL Testcontainer must be running");

        try (Connection connection = dataSource.getConnection()) {
            assertEquals(
                    POSTGRESQL.getJdbcUrl(),
                    connection.getMetaData().getURL());

            assertEquals(
                    "legal_cases_test",
                    connection.getCatalog());
        }

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(
                status -> seedAuthenticatedUser());
    }

    @Test
    void passwordChangeAndRecoveryPreserveSessionSecurityPolicy()
            throws Exception {

        /*
         * 1. El usuario puede autenticarse con la contraseña inicial.
         */
        MvcResult initialLogin =
                login(INITIAL_PASSWORD, 200);

        Cookie initialAuthCookie =
                authCookie(initialLogin);

        String initialJwt =
                initialAuthCookie.getValue();

        assertEquals(
                0L,
                jwtService.obtenerAuthVersion(initialJwt));

        /*
         * 2. Obtiene un CSRF válido asociado al estado actual
         * y ejecuta PATCH /cambiar-password.
         */
        CsrfCredentials initialCsrf =
                csrf(initialAuthCookie);

        MvcResult passwordChange =
                mockMvc.perform(
                                patch("/api/auth/cambiar-password")
                                        .cookie(
                                                initialAuthCookie,
                                                initialCsrf.cookie())
                                        .header(
                                                initialCsrf.headerName(),
                                                initialCsrf.token())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "passwordActual",
                                                                INITIAL_PASSWORD,
                                                                "passwordNueva",
                                                                CHANGED_PASSWORD))))
                        .andExpect(status().isNoContent())
                        .andReturn();

        /*
         * 3. La respuesta renueva la cookie JWT.
         */
        Cookie renewedAuthCookie =
                authCookie(passwordChange);

        String renewedJwt =
                renewedAuthCookie.getValue();

        assertNotEquals(
                initialJwt,
                renewedJwt);

        assertEquals(
                1L,
                jwtService.obtenerAuthVersion(renewedJwt));

        /*
         * 4. El cambio exitoso elimina el estado CSRF anterior.
         */
        assertTrue(
                passwordChange
                        .getResponse()
                        .getHeaders(HttpHeaders.SET_COOKIE)
                        .stream()
                        .anyMatch(this::isExpiredCsrfCookie),
                "Successful password change must expire the previous CSRF cookie");

        CsrfCredentials renewedCsrf =
                csrf(renewedAuthCookie);

        assertNotEquals(
                initialCsrf.cookie().getValue(),
                renewedCsrf.cookie().getValue(),
                "A new CSRF cookie must be issued after password change");

        /*
         * 5. El JWT anterior queda revocado por auth_version.
         */
        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(initialAuthCookie))
                .andExpect(status().isUnauthorized());

        /*
         * 6. El JWT renovado continúa autenticado.
         */
        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(renewedAuthCookie))
                .andExpect(status().isOk());

        UsuarioSistema changedUser =
                usuarioSistemaRepository
                        .findById(usuarioId)
                        .orElseThrow();

        String changedPasswordHash =
                changedUser.getPasswordHash();

        assertEquals(
                1L,
                changedUser.getAuthVersion());

        assertFalse(
                passwordEncoder.matches(
                        INITIAL_PASSWORD,
                        changedPasswordHash));

        assertTrue(
                passwordEncoder.matches(
                        CHANGED_PASSWORD,
                        changedPasswordHash));

        /*
         * 7. La contraseña anterior ya no autentica.
         */
        login(INITIAL_PASSWORD, 400);

        /*
         * 8. La nueva contraseña sí autentica.
         */
        MvcResult changedPasswordLogin =
                login(CHANGED_PASSWORD, 200);

        Cookie changedPasswordSession =
                authCookie(changedPasswordLogin);

        assertEquals(
                1L,
                jwtService.obtenerAuthVersion(
                        changedPasswordSession.getValue()));

        /*
         * 9. Se crea un token de recuperación real persistido.
         * El endpoint HTTP de restablecimiento consume ese token.
         */
        UsuarioSistema userForReset =
                usuarioSistemaRepository
                        .findById(usuarioId)
                        .orElseThrow();

        PasswordResetTokenService.TokenRecuperacion resetToken =
                passwordResetTokenService
                        .crearTokenParaUsuario(userForReset);

        CsrfCredentials resetCsrf =
                csrf(null);

        mockMvc.perform(
                        post("/api/auth/restablecer-password")
                                .cookie(resetCsrf.cookie())
                                .header(
                                        resetCsrf.headerName(),
                                        resetCsrf.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(
                                                Map.of(
                                                        "token",
                                                        resetToken.token(),
                                                        "passwordNueva",
                                                        RESET_PASSWORD,
                                                        "confirmarPassword",
                                                        RESET_PASSWORD))))
                .andExpect(status().isOk());

        UsuarioSistema resetUser =
                usuarioSistemaRepository
                        .findById(usuarioId)
                        .orElseThrow();

        String resetPasswordHash =
                resetUser.getPasswordHash();

        assertEquals(
                2L,
                resetUser.getAuthVersion());

        assertFalse(
                passwordEncoder.matches(
                        CHANGED_PASSWORD,
                        resetPasswordHash));

        assertTrue(
                passwordEncoder.matches(
                        RESET_PASSWORD,
                        resetPasswordHash));

        /*
         * 10. El JWT emitido antes de la recuperación también queda revocado.
         */
        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(changedPasswordSession))
                .andExpect(status().isUnauthorized());

        /*
         * 11. La contraseña anterior al reset falla y la recuperada funciona.
         */
        login(CHANGED_PASSWORD, 400);
        login(RESET_PASSWORD, 200);

        /*
         * 12. Existe auditoría del cambio autenticado.
         */
        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM "DB_consultorioJuridico".audit_logs
                        WHERE action = 'CAMBIAR_PASSWORD_PROPIA'
                          AND outcome = 'SUCCESS'
                        """));

        /*
         * 13. Ningún registro de auditoría contiene secretos.
         */
        String auditEvidence =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(
                            string_agg(row_to_json(a)::text, E'\\n'),
                            ''
                        )
                        FROM "DB_consultorioJuridico".audit_logs a
                        """,
                        String.class);

        assertNotNull(auditEvidence);

        List<String> secrets =
                List.of(
                        INITIAL_PASSWORD,
                        CHANGED_PASSWORD,
                        RESET_PASSWORD,
                        initialJwt,
                        renewedJwt,
                        resetToken.token(),
                        changedPasswordHash,
                        resetPasswordHash);

        for (String secret : secrets) {
            assertFalse(
                    auditEvidence.contains(secret),
                    "Audit logs must not contain sensitive authentication values");
        }
    }

    private MvcResult login(
            String password,
            int expectedStatus)
            throws Exception {

        CsrfCredentials csrf =
                csrf(null);

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .cookie(csrf.cookie())
                                        .header(
                                                csrf.headerName(),
                                                csrf.token())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "username",
                                                                username,
                                                                "password",
                                                                password))))
                        .andReturn();

        assertEquals(
                expectedStatus,
                result.getResponse().getStatus());

        return result;
    }

    private CsrfCredentials csrf(
            Cookie authenticationCookie)
            throws Exception {

        var request =
                get("/api/auth/csrf");

        if (authenticationCookie != null) {
            request.cookie(authenticationCookie);
        }

        MvcResult result =
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andReturn();

        Cookie csrfCookie =
                result.getResponse()
                        .getCookie("XSRF-TOKEN");

        assertNotNull(
                csrfCookie,
                "CSRF endpoint must issue XSRF-TOKEN cookie");

        var json =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString());

        String headerName =
                json.get("headerName")
                        .asText();

        String token =
                json.get("token")
                        .asText();

        assertFalse(headerName.isBlank());
        assertFalse(token.isBlank());

        return new CsrfCredentials(
                csrfCookie,
                headerName,
                token);
    }

    private Cookie authCookie(
            MvcResult result) {

        String header =
                result.getResponse()
                        .getHeaders(HttpHeaders.SET_COOKIE)
                        .stream()
                        .filter(value ->
                                value.startsWith("access_token="))
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Response did not renew access_token"));

        int separator =
                header.indexOf(';');

        String cookieValue =
                separator >= 0
                        ? header.substring(
                        "access_token=".length(),
                        separator)
                        : header.substring(
                        "access_token=".length());

        assertFalse(cookieValue.isBlank());

        return new Cookie(
                "access_token",
                cookieValue);
    }

    private boolean isExpiredCsrfCookie(
            String setCookieHeader) {

        return setCookieHeader.startsWith("XSRF-TOKEN=")
                && (
                setCookieHeader.contains("Max-Age=0")
                        || setCookieHeader.contains("Max-Age=0;")
        );
    }

    private int count(
            String sql) {

        Integer value =
                jdbcTemplate.queryForObject(
                        sql,
                        Integer.class);

        return value == null
                ? 0
                : value;
    }

    private void seedAuthenticatedUser() {
        TipoDocumento documentType =
                new TipoDocumento();

        documentType.setNombre(
                "Documento auth " + executionId);

        entityManager.persist(
                documentType);

        Sede campus =
                new Sede();

        campus.setNombre(
                "Sede auth " + executionId);

        entityManager.persist(
                campus);

        Rol role =
                new Rol();

        role.setNombre(
                "Rol auth " + executionId);

        role.setDescripcion(
                "Rol sintetico para politica de sesion");

        role.setTipoPerfil(
                TipoPerfilUsuario.ADMINISTRATIVO);

        role.setCodigoBase(
                CodigoRolBase.ADMINISTRADOR);

        entityManager.persist(
                role);

        username =
                "auth."
                        + executionId
                        + "@integration.test";

        UsuarioSistema user =
                new UsuarioSistema();

        user.setUsername(
                username);

        user.setPasswordHash(
                passwordEncoder.encode(
                        INITIAL_PASSWORD));

        user.setTipoPerfilActual(
                TipoPerfilUsuario.ADMINISTRATIVO);

        user.setRol(
                role);

        entityManager.persist(
                user);

        Administrativo profile =
                new Administrativo();

        profile.setNombre(
                "Administrativo auth");

        profile.setTipoDocumento(
                documentType);

        profile.setDocumento(
                "DOC-" + executionId);

        profile.setEmail(
                username);

        profile.setTelefono(
                "TEL-" + executionId);

        profile.setUsuario(
                "usr-" + executionId);

        profile.setCodigo(
                "COD-" + executionId);

        profile.setSede(
                campus);

        profile.setDirectora(
                true);

        profile.setUsuarioSistema(
                user);

        entityManager.persist(
                profile);

        entityManager.flush();

        usuarioId =
                user.getId();
    }

    private record CsrfCredentials(
            Cookie cookie,
            String headerName,
            String token) {
    }
}