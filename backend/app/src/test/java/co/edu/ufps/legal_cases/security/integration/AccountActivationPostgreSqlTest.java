package co.edu.ufps.legal_cases.security.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import co.edu.ufps.legal_cases.common.exception.AccountActivationException;
import co.edu.ufps.legal_cases.security.dto.auth.activation.ActivarCuentaRequestDTO;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.EstadoActivacionCuenta;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.repository.account.UsuarioSistemaRepository;
import co.edu.ufps.legal_cases.security.service.auth.activation.AccountActivationService;
import co.edu.ufps.legal_cases.security.service.auth.activation.AccountActivationTokenService;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountActivationPostgreSqlTest
        extends PostgreSqlIntegrationTest {

    private static final String ACTIVATED_PASSWORD =
            "clave-activada";

    /*
     * Mantiene el mismo patrón de las demás pruebas PostgreSQL:
     * Spring debe recibir las propiedades del contenedor ya iniciado.
     */
    @Container
    static final PostgreSQLContainer TEST_POSTGRESQL =
            startPostgreSql();

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
    private AccountActivationTokenService tokenService;

    @Autowired
    private AccountActivationService activationService;

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

    @BeforeAll
    void verifySyntheticDatabase() throws Exception {
        assertTrue(
                POSTGRESQL.isRunning(),
                "PostgreSQL Testcontainer must be running");

        try (Connection connection =
                     dataSource.getConnection()) {

            assertEquals(
                    POSTGRESQL.getJdbcUrl(),
                    connection.getMetaData().getURL());

            assertEquals(
                    "legal_cases_test",
                    connection.getCatalog());
        }
    }

    @Test
    void latestInvitationActivatesAccountOnceWithoutPersistingRawToken()
            throws Exception {

        PendingAccount account =
                createPendingAccount(
                        "principal");

        /*
         * Una cuenta pendiente no tiene secreto inicial.
         */
        UsuarioSistema pending =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        assertEquals(
                EstadoActivacionCuenta.PENDIENTE,
                pending.getEstadoActivacion());

        assertNull(
                pending.getPasswordHash());

        /*
         * El documento histórico no puede actuar como contraseña inicial.
         */
        login(
                account.username(),
                account.document(),
                400);

        /*
         * La segunda invitación revoca automáticamente la anterior.
         */
        var first =
                tokenService.crearTokenParaUsuario(
                        pending);

        UsuarioSistema refreshed =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        var latest =
                tokenService.crearTokenParaUsuario(
                        refreshed);

        assertNotEquals(
                first.token(),
                latest.token());

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM "DB_consultorioJuridico".account_activation_token
                        WHERE usuario_sistema_id = ?
                          AND usado = FALSE
                          AND revocado = FALSE
                        """,
                        account.usuarioId()));

        /*
         * Nunca debe persistirse el token real.
         */
        String storedHash =
                jdbcTemplate.queryForObject(
                        """
                        SELECT token_hash
                        FROM "DB_consultorioJuridico".account_activation_token
                        WHERE usuario_sistema_id = ?
                          AND revocado = FALSE
                          AND usado = FALSE
                        """,
                        String.class,
                        account.usuarioId());

        assertNotNull(storedHash);
        assertEquals(64, storedHash.length());
        assertNotEquals(
                latest.token(),
                storedHash);

        /*
         * La invitación anterior queda revocada.
         */
        activate(
                first.token(),
                ACTIVATED_PASSWORD,
                403);

        UsuarioSistema stillPending =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        assertEquals(
                EstadoActivacionCuenta.PENDIENTE,
                stillPending.getEstadoActivacion());

        assertNull(
                stillPending.getPasswordHash());

        /*
         * La invitación más reciente activa la cuenta.
         */
        activate(
                latest.token(),
                ACTIVATED_PASSWORD,
                204);

        UsuarioSistema activated =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        assertEquals(
                EstadoActivacionCuenta.ACTIVA,
                activated.getEstadoActivacion());

        assertNotNull(
                activated.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        ACTIVATED_PASSWORD,
                        activated.getPasswordHash()));

        assertFalse(
                passwordEncoder.matches(
                        account.document(),
                        activated.getPasswordHash()));

        /*
         * La activación incrementa la versión defensiva de credenciales.
         */
        assertEquals(
                1L,
                activated.getAuthVersion());

        /*
         * El token queda consumido exactamente una vez.
         */
        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM "DB_consultorioJuridico".account_activation_token
                        WHERE usuario_sistema_id = ?
                          AND usado = TRUE
                          AND fecha_uso IS NOT NULL
                        """,
                        account.usuarioId()));

        /*
         * Reutilizar exactamente el mismo enlace es conflicto.
         */
        activate(
                latest.token(),
                ACTIVATED_PASSWORD,
                409);

        /*
         * Después de activar, la contraseña elegida sí autentica.
         */
        login(
                account.username(),
                ACTIVATED_PASSWORD,
                200);

        /*
         * Debe existir auditoría de la activación exitosa.
         */
        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM "DB_consultorioJuridico".audit_logs
                        WHERE action = 'ACTIVAR_CUENTA'
                          AND outcome = 'SUCCESS'
                          AND entity_id = ?
                        """,
                        account.usuarioId().toString()));

        /*
         * La auditoría nunca puede contener token, contraseña ni hash BCrypt.
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
                        first.token(),
                        latest.token(),
                        ACTIVATED_PASSWORD,
                        activated.getPasswordHash());

        for (String secret : secrets) {
            assertFalse(
                    auditEvidence.contains(secret),
                    "Audit logs must not contain activation secrets");
        }
    }

    @Test
    void expiredInvitationCannotActivateAccount()
            throws Exception {

        PendingAccount account =
                createPendingAccount(
                        "expired");

        UsuarioSistema user =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        var token =
                tokenService.crearTokenParaUsuario(
                        user);

        jdbcTemplate.update(
                """
                UPDATE "DB_consultorioJuridico".account_activation_token
                SET fecha_expiracion = NOW() - INTERVAL '1 minute'
                WHERE usuario_sistema_id = ?
                  AND usado = FALSE
                  AND revocado = FALSE
                """,
                account.usuarioId());

        activate(
                token.token(),
                ACTIVATED_PASSWORD,
                410);

        UsuarioSistema unchanged =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        assertEquals(
                EstadoActivacionCuenta.PENDIENTE,
                unchanged.getEstadoActivacion());

        assertNull(
                unchanged.getPasswordHash());

        assertEquals(
                0,
                count(
                        """
                        SELECT COUNT(*)
                        FROM "DB_consultorioJuridico".account_activation_token
                        WHERE usuario_sistema_id = ?
                          AND usado = TRUE
                        """,
                        account.usuarioId()));
    }

    @Test
    void concurrentConsumptionAllowsExactlyOneActivation()
            throws Exception {

        PendingAccount account =
                createPendingAccount(
                        "concurrent");

        UsuarioSistema user =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        String rawToken =
                tokenService
                        .crearTokenParaUsuario(user)
                        .token();

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<String> first =
                    executor.submit(
                            () -> concurrentActivation(
                                    rawToken,
                                    ready,
                                    start));

            Future<String> second =
                    executor.submit(
                            () -> concurrentActivation(
                                    rawToken,
                                    ready,
                                    start));

            assertTrue(
                    ready.await(
                            10,
                            TimeUnit.SECONDS),
                    "Concurrent workers did not become ready");

            start.countDown();

            List<String> results =
                    List.of(
                            first.get(
                                    20,
                                    TimeUnit.SECONDS),
                            second.get(
                                    20,
                                    TimeUnit.SECONDS));

            assertEquals(
                    1,
                    results.stream()
                            .filter("SUCCESS"::equals)
                            .count());

            assertEquals(
                    1,
                    results.stream()
                            .filter(
                                    "TOKEN_ACTIVACION_USADO"::equals)
                            .count());

        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        UsuarioSistema activated =
                usuarioSistemaRepository
                        .findById(account.usuarioId())
                        .orElseThrow();

        assertEquals(
                EstadoActivacionCuenta.ACTIVA,
                activated.getEstadoActivacion());

        assertTrue(
                passwordEncoder.matches(
                        ACTIVATED_PASSWORD,
                        activated.getPasswordHash()));

        assertEquals(
                1,
                count(
                        """
                        SELECT COUNT(*)
                        FROM "DB_consultorioJuridico".account_activation_token
                        WHERE usuario_sistema_id = ?
                          AND usado = TRUE
                          AND fecha_uso IS NOT NULL
                        """,
                        account.usuarioId()));
    }

    private String concurrentActivation(
            String token,
            CountDownLatch ready,
            CountDownLatch start)
            throws InterruptedException {

        ready.countDown();

        if (!start.await(
                10,
                TimeUnit.SECONDS)) {

            return "START_TIMEOUT";
        }

        ActivarCuentaRequestDTO dto =
                activationRequest(
                        token,
                        ACTIVATED_PASSWORD);

        try {
            activationService.activarCuenta(dto);
            return "SUCCESS";

        } catch (AccountActivationException exception) {
            return exception.getCodigo();
        }
    }

    private void activate(
            String token,
            String password,
            int expectedStatus)
            throws Exception {

        CsrfCredentials csrf =
                csrf();

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/activar-cuenta")
                                        .cookie(csrf.cookie())
                                        .header(
                                                csrf.headerName(),
                                                csrf.token())
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
                                        .content(
                                                jsonMapper.writeValueAsString(
                                                        Map.of(
                                                                "token",
                                                                token,
                                                                "passwordNueva",
                                                                password))))
                        .andReturn();

        assertEquals(
                expectedStatus,
                result.getResponse().getStatus(),
                result.getResponse().getContentAsString());
    }

    private void login(
            String username,
            String password,
            int expectedStatus)
            throws Exception {

        CsrfCredentials csrf =
                csrf();

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .cookie(csrf.cookie())
                                        .header(
                                                csrf.headerName(),
                                                csrf.token())
                                        .contentType(
                                                MediaType.APPLICATION_JSON)
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
                result.getResponse().getStatus(),
                result.getResponse().getContentAsString());
    }

    private CsrfCredentials csrf()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get("/api/auth/csrf"))
                        .andExpect(
                                status().isOk())
                        .andReturn();

        Cookie cookie =
                result.getResponse()
                        .getCookie("XSRF-TOKEN");

        assertNotNull(
                cookie,
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

        assertFalse(
                headerName.isBlank());

        assertFalse(
                token.isBlank());

        return new CsrfCredentials(
                cookie,
                headerName,
                token);
    }

    private PendingAccount createPendingAccount(
            String suffix) {

        TransactionTemplate transaction =
                new TransactionTemplate(
                        transactionManager);

        return transaction.execute(
                status -> {

                    String identifier =
                            executionId
                                    + "-"
                                    + suffix;

                    TipoDocumento documentType =
                            new TipoDocumento();

                    documentType.setNombre(
                            "Documento activacion "
                                    + identifier);

                    entityManager.persist(
                            documentType);

                    Sede campus =
                            new Sede();

                    campus.setNombre(
                            "Sede activacion "
                                    + identifier);

                    entityManager.persist(
                            campus);

                    Rol role =
                            new Rol();

                    role.setNombre(
                            "Rol activacion "
                                    + identifier);

                    role.setDescripcion(
                            "Rol sintetico para activacion");

                    role.setTipoPerfil(
                            TipoPerfilUsuario.ADMINISTRATIVO);

                    entityManager.persist(
                            role);

                    String username =
                            "activation."
                                    + identifier
                                    + "@integration.test";

                    String document =
                            "DOC-"
                                    + identifier;

                    UsuarioSistema user =
                            new UsuarioSistema();

                    user.setUsername(
                            username);

                    user.setPasswordHash(
                            null);

                    user.setEstadoActivacion(
                            EstadoActivacionCuenta.PENDIENTE);

                    user.setTipoPerfilActual(
                            TipoPerfilUsuario.ADMINISTRATIVO);

                    user.setRol(
                            role);

                    user.setActivo(
                            true);

                    entityManager.persist(
                            user);

                    Administrativo profile =
                            new Administrativo();

                    profile.setNombre(
                            "Administrativo activacion "
                                    + identifier);

                    profile.setTipoDocumento(
                            documentType);

                    profile.setDocumento(
                            document);

                    profile.setEmail(
                            username);

                    profile.setTelefono(
                            "TEL-" + identifier);

                    profile.setUsuario(
                            "usr-" + identifier);

                    profile.setCodigo(
                            "COD-" + identifier);

                    profile.setSede(
                            campus);

                    profile.setDirectora(
                            false);

                    profile.setUsuarioSistema(
                            user);

                    entityManager.persist(
                            profile);

                    entityManager.flush();

                    return new PendingAccount(
                            user.getId(),
                            username,
                            document);
                });
    }

    private ActivarCuentaRequestDTO activationRequest(
            String token,
            String password) {

        ActivarCuentaRequestDTO dto =
                new ActivarCuentaRequestDTO();

        dto.setToken(
                token);

        dto.setPasswordNueva(
                password);

        return dto;
    }

    private int count(
            String sql,
            Object... arguments) {

        Integer value =
                jdbcTemplate.queryForObject(
                        sql,
                        Integer.class,
                        arguments);

        return value == null
                ? 0
                : value;
    }

    private record PendingAccount(
            Long usuarioId,
            String username,
            String document) {
    }

    private record CsrfCredentials(
            Cookie cookie,
            String headerName,
            String token) {
    }
}