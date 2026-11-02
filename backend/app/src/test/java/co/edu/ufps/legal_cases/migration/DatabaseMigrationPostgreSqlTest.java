package co.edu.ufps.legal_cases.migration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class DatabaseMigrationPostgreSqlTest {

    private static final String S = "\"DB_consultorioJuridico\".";
    private static final int EXPECTED_SQL_MIGRATIONS = 2;

    @Container
    static final PostgreSQLContainer PG =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("migration_control")
                    .withUsername("migration_owner")
                    .withPassword("synthetic-owner-only");

    @TempDir
    Path temporary;

    String database;
    Map<String, String> environment;
    ByteArrayOutputStream output;

    @BeforeEach
    void isolatedDatabase() throws Exception {
        database =
                "migration_"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        sql(
                "migration_control",
                "CREATE DATABASE " + database);

        environment = environment(database);
        output = new ByteArrayOutputStream();
    }

    @Test
    void emptyDatabaseMigratesOnceAndAppliesReviewedMigrations()
            throws Exception {

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        assertEquals(
                0,
                run("validate"),
                output.toString());

        assertEquals(
                EXPECTED_SQL_MIGRATIONS,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='SQL' AND success"));

        assertAuthVersionColumn();

        assertTrue(
                output.toString()
                        .contains("baseline_sql_sha256="));

        assertFalse(
                output.toString()
                        .contains(PG.getPassword()));

        System.out.println(
                "SCRUM-332 empty migrate/repeat/validate PASS version="
                        + DatabaseMigrationCommand.BASELINE_VERSION);
    }

    @Test
    void unmanagedNonemptySchemaIsNotAutomaticallyBaselined()
            throws Exception {

        sql(
                database,
                "CREATE SCHEMA \"DB_consultorioJuridico\";"
                        + "CREATE TABLE "
                        + S
                        + "legacy_marker(id integer);"
                        + "INSERT INTO "
                        + S
                        + "legacy_marker VALUES(7)");

        assertEquals(
                1,
                run("migrate"));

        assertEquals(
                7,
                number(
                        "SELECT id FROM "
                                + S
                                + "legacy_marker"));

        assertEquals(
                0,
                number(
                        "SELECT count(*) "
                                + "FROM information_schema.tables "
                                + "WHERE table_schema='DB_consultorioJuridico' "
                                + "AND table_name='flyway_schema_history'"));
    }

    @Test
    void equivalentLegacyBaselineRequiresBackupAndPreservesRowsThroughRestore()
            throws Exception {

        // Construye inicialmente el esquema administrado completo.
        assertEquals(
                0,
                run("migrate"),
                output.toString());

        /*
         * Reconstruye el estado exacto del baseline histórico.
         * auth_version pertenece a una migración posterior al baseline.
         */
        sql(
                database,
                "ALTER TABLE "
                        + S
                        + "usuario_sistema DROP COLUMN auth_version;"
                        + "DROP TABLE "
                        + S
                        + "flyway_schema_history");

        seedRelatedRows();

        String before = sample();
        byte[] backup = dump();
        String backupHash =
                SchemaContract.sha256(backup);

        Path destination =
                Path.of(
                        "target",
                        "migration-evidence");

        Files.createDirectories(destination);

        Files.setPosixFilePermissions(
                destination,
                java.nio.file.attribute.PosixFilePermissions
                        .fromString("rwx------"));

        Path file =
                destination.resolve(
                        database + ".dump");

        Files.write(file, backup);

        Files.setPosixFilePermissions(
                file,
                java.nio.file.attribute.PosixFilePermissions
                        .fromString("rw-------"));

        // Sin confirmación explícita no se permite adoptar el baseline.
        assertEquals(
                1,
                run("baseline"));

        assertEquals(
                0,
                number(
                        "SELECT count(*) "
                                + "FROM information_schema.tables "
                                + "WHERE table_schema='DB_consultorioJuridico' "
                                + "AND table_name='flyway_schema_history'"));

        environment.put(
                "MIGRATION_BASELINE_ACK",
                DatabaseMigrationCommand.BASELINE_VERSION);

        environment.put(
                "MIGRATION_BACKUP_SHA256",
                backupHash);

        assertEquals(
                0,
                run("baseline"),
                output.toString());

        /*
         * Después de adoptar el baseline histórico,
         * debe aplicarse la migración posterior de auth_version.
         */
        assertEquals(
                0,
                run("migrate"),
                output.toString());

        assertEquals(
                0,
                run("validate"),
                output.toString());

        assertEquals(
                1,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='BASELINE' AND success"));

        assertEquals(
                1,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='SQL' AND success"));

        assertAuthVersionColumn();

        assertEquals(
                before,
                sample());

        // El historial existente nunca debe reescribirse.
        assertEquals(
                1,
                run("baseline"));

        sql(
                "migration_control",
                "DROP DATABASE " + database);

        sql(
                "migration_control",
                "CREATE DATABASE " + database);

        restore(backup);

        assertEquals(
                before,
                sample());

        /*
         * El backup fue tomado sobre el estado histórico del baseline,
         * por lo que todavía debe coincidir con dicho contrato.
         */
        try (var connection =
                     connection(database)) {

            SchemaContract.requireBaseline(
                    connection);
        }

        assertEquals(
                0,
                run("baseline"),
                output.toString());

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        assertEquals(
                0,
                run("validate"),
                output.toString());

        assertAuthVersionColumn();

        System.out.println(
                "SCRUM-332 legacy backup/baseline/restore PASS "
                        + "dump_bytes="
                        + backup.length
                        + " dump_sha256="
                        + backupHash
                        + " sample_sha256="
                        + SchemaContract.sha256(
                        before.getBytes(
                                StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ALTER TABLE \"DB_consultorioJuridico\".sede DROP COLUMN activo",
            "DROP TRIGGER trigger_prevent_audit_mod ON \"DB_consultorioJuridico\".audit_logs",
            "DROP INDEX \"DB_consultorioJuridico\".uk_file_asset_doc_vigente",
            "CREATE TABLE \"DB_consultorioJuridico\".unexpected_table(id integer)"
    })
    void divergentLegacyIsRejectedBeforeHistoryIsWritten(
            String divergence)
            throws Exception {

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        /*
         * Devuelve el esquema al baseline histórico antes de introducir
         * deliberadamente una divergencia.
         */
        sql(
                database,
                "ALTER TABLE "
                        + S
                        + "usuario_sistema DROP COLUMN auth_version;"
                        + "DROP TABLE "
                        + S
                        + "flyway_schema_history");

        sql(
                database,
                divergence);

        environment.put(
                "MIGRATION_BASELINE_ACK",
                DatabaseMigrationCommand.BASELINE_VERSION);

        environment.put(
                "MIGRATION_BACKUP_SHA256",
                "a".repeat(64));

        assertEquals(
                1,
                run("baseline"));

        assertEquals(
                0,
                number(
                        "SELECT count(*) "
                                + "FROM information_schema.tables "
                                + "WHERE table_schema='DB_consultorioJuridico' "
                                + "AND table_name='flyway_schema_history'"));
    }

    @Test
    void modifiedAppliedMigrationFailsChecksumValidation()
            throws Exception {

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        Files.writeString(
                temporary.resolve(
                        "V20261015_01__baseline_current_schema.sql"),
                SchemaContract.resource(
                        DatabaseMigrationCommand.BASELINE_SQL)
                        + "\n-- checksum tamper fixture\n");

        /*
         * La ubicación temporal debe contener también la segunda
         * migración sin modificar para que la prueba falle únicamente
         * por el checksum adulterado del baseline.
         */
        Files.writeString(
                temporary.resolve(
                        "V20261102_01__add_auth_version_to_usuario_sistema.sql"),
                SchemaContract.resource(
                        "/db/migration/"
                                + "V20261102_01__add_auth_version_to_usuario_sistema.sql"));

        var tampered =
                DatabaseMigrationCommand
                        .configuration(
                                url(database),
                                PG.getUsername(),
                                PG.getPassword())
                        .locations(
                                "filesystem:"
                                        + temporary)
                        .load();

        assertThrows(
                FlywayException.class,
                tampered::validate);

        assertThrows(
                FlywayException.class,
                tampered::migrate);

        assertEquals(
                EXPECTED_SQL_MIGRATIONS,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='SQL' AND success"));
    }

    @Test
    void failedMigrationRollsBackDdlDataAndSuccessHistory()
            throws Exception {

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        seedRelatedRows();

        String before = sample();

        /*
         * Debe ser posterior a V20261102_01 para que Flyway realmente
         * intente ejecutarla y podamos verificar el rollback.
         */
        Files.writeString(
                temporary.resolve(
                        "V20261102_02__synthetic_failure.sql"),
                "CREATE TABLE "
                        + "\"DB_consultorioJuridico\"."
                        + "must_rollback(id integer);"
                        + "INSERT INTO "
                        + "\"DB_consultorioJuridico\"."
                        + "sede(nombre,activo) "
                        + "VALUES('must rollback',true);"
                        + "DO $$ BEGIN "
                        + "RAISE EXCEPTION 'synthetic rollback test'; "
                        + "END $$;");

        var failed =
                DatabaseMigrationCommand
                        .configuration(
                                url(database),
                                PG.getUsername(),
                                PG.getPassword())
                        .locations(
                                "classpath:db/migration",
                                "filesystem:"
                                        + temporary)
                        .load();

        assertThrows(
                FlywayException.class,
                failed::migrate);

        assertEquals(
                0,
                number(
                        "SELECT count(*) "
                                + "FROM information_schema.tables "
                                + "WHERE table_schema='DB_consultorioJuridico' "
                                + "AND table_name='must_rollback'"));

        assertEquals(
                EXPECTED_SQL_MIGRATIONS,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='SQL' AND success"));

        assertEquals(
                before,
                sample());

        assertEquals(
                0,
                run("validate"),
                output.toString());
    }

    @Test
    void baselineContainsImmutableAuditAndReassignmentGuards()
            throws Exception {

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        sql(
                database,
                "INSERT INTO "
                        + S
                        + "audit_logs("
                        + "actor_username,"
                        + "action,"
                        + "entity_name,"
                        + "outcome,"
                        + "occurred_at,"
                        + "source,"
                        + "correlation_id"
                        + ") VALUES("
                        + "'synthetic',"
                        + "'QA',"
                        + "'Synthetic',"
                        + "'SUCCESS',"
                        + "now(),"
                        + "'SYSTEM',"
                        + "'synthetic-migration'"
                        + ")");

        assertThrows(
                java.sql.SQLException.class,
                () -> sql(
                        database,
                        "UPDATE "
                                + S
                                + "audit_logs "
                                + "SET action='altered'"));

        assertThrows(
                java.sql.SQLException.class,
                () -> sql(
                        database,
                        "DELETE FROM "
                                + S
                                + "audit_logs"));

        assertEquals(
                2,
                number(
                        "SELECT count(*) "
                                + "FROM pg_trigger "
                                + "WHERE tgrelid="
                                + "'\"DB_consultorioJuridico\"."
                                + "consulta_reasignacion_historial'"
                                + "::regclass "
                                + "AND NOT tgisinternal"));

        for (String script :
                new String[] {
                        "db/migration/"
                                + "V20260905_01__version_document_model_verify.sql",
                        "db/migration/"
                                + "V20261011_01__create_consulta_reasignacion_historial_verify.sql",
                        "../db/optimistic_locking_verify.sql"
                }) {

            sql(
                    database,
                    Files.readString(
                            Path.of(script)));
        }
    }

    @Test
    void duplicateVersionsAndOutOfOrderChangesAreRejected()
            throws Exception {

        assertEquals(
                0,
                run("migrate"),
                output.toString());

        /*
         * Crea una versión posterior a la migración real
         * V20261102_01.
         */
        Path later =
                temporary.resolve(
                        "V20261102_03__synthetic_later.sql");

        Files.writeString(
                later,
                "CREATE TABLE "
                        + "\"DB_consultorioJuridico\"."
                        + "later_version(id integer);");

        var additional =
                DatabaseMigrationCommand
                        .configuration(
                                url(database),
                                PG.getUsername(),
                                PG.getPassword())
                        .locations(
                                "classpath:db/migration",
                                "filesystem:"
                                        + temporary)
                        .load();

        additional.migrate();

        /*
         * Esta migración queda por debajo de la versión ya aplicada
         * 20261102.03 y debe ser rechazada con outOfOrder=false.
         */
        Path outOfOrder =
                temporary.resolve(
                        "V20261102_02__synthetic_out_of_order.sql");

        Files.writeString(
                outOfOrder,
                "SELECT 1;");

        assertThrows(
                FlywayException.class,
                additional::migrate);

        assertEquals(
                EXPECTED_SQL_MIGRATIONS + 1,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='SQL' AND success"));

        Files.delete(outOfOrder);

        /*
         * Después crea otra migración con la misma versión 20261102.03
         * para verificar detección de duplicados.
         */
        Files.writeString(
                temporary.resolve(
                        "V20261102_03__synthetic_duplicate.sql"),
                "SELECT 1;");

        assertThrows(
                FlywayException.class,
                additional::validate);
    }

    @Test
    void concurrentDeploymentIsRejectedAndDoesNotCreateSchema()
            throws Exception {

        try (var holder =
                     connection(database);
             var statement =
                     holder.createStatement()) {

            statement.execute(
                    "SELECT pg_advisory_lock("
                            + "33220261015)");

            assertEquals(
                    1,
                    run("migrate"));

            assertEquals(
                    0,
                    number(
                            "SELECT count(*) "
                                    + "FROM information_schema.schemata "
                                    + "WHERE schema_name="
                                    + "'DB_consultorioJuridico'"));
        }

        assertEquals(
                0,
                run("migrate"),
                output.toString());
    }

    @Test
    void realCommandProcessUsesOnlyMigrationCredentialsAndSanitizedOutput()
            throws Exception {

        /*
         * Surefire agrega una entrada vacía.
         * Java la interpreta como el working directory, que contiene
         * SQL histórico en db/migration.
         * El JAR ejecutable no contiene esa entrada.
         */
        String classpath =
                java.util.Arrays.stream(
                                System.getProperty(
                                                "surefire.test.class.path",
                                                System.getProperty(
                                                        "java.class.path"))
                                        .split(
                                                java.util.regex.Pattern.quote(
                                                        java.io.File.pathSeparator)))
                        .filter(
                                path ->
                                        !path.isBlank())
                        .collect(
                                java.util.stream.Collectors
                                        .joining(
                                                java.io.File.pathSeparator));

        var process =
                new ProcessBuilder(
                        Path.of(
                                        System.getProperty(
                                                "java.home"),
                                        "bin",
                                        "java")
                                .toString(),
                        "-cp",
                        classpath,
                        DatabaseMigrationCommand.class
                                .getName(),
                        "migrate");

        process.environment()
                .putAll(environment);

        process.environment()
                .put(
                        "DB_URL",
                        "jdbc:postgresql://"
                                + "unreachable.invalid/"
                                + "never_use_runtime_credentials");

        process.redirectErrorStream(true);

        var child =
                process.start();

        if (!child.waitFor(
                60,
                java.util.concurrent.TimeUnit.SECONDS)) {

            child.destroyForcibly();

            fail(
                    "Standalone migration command timed out");
        }

        String result =
                new String(
                        child.getInputStream()
                                .readAllBytes(),
                        StandardCharsets.UTF_8);

        assertEquals(
                0,
                child.exitValue(),
                result);

        assertTrue(
                result.contains(
                        "MIGRATION_OK"));

        assertFalse(
                result.contains(
                        PG.getPassword()));

        assertFalse(
                result.contains(
                        url(database)));

        assertEquals(
                EXPECTED_SQL_MIGRATIONS,
                number(
                        "SELECT count(*) FROM "
                                + S
                                + "flyway_schema_history "
                                + "WHERE type='SQL' AND success"));
    }

    private void assertAuthVersionColumn()
            throws Exception {

        assertEquals(
                1,
                number(
                        """
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'DB_consultorioJuridico'
                          AND table_name = 'usuario_sistema'
                          AND column_name = 'auth_version'
                          AND data_type = 'bigint'
                          AND is_nullable = 'NO'
                          AND column_default IS NOT NULL
                        """));
    }

    private int run(String operation) {
        return DatabaseMigrationCommand.run(
                operation,
                environment,
                new PrintStream(output));
    }

    private static Map<String, String> environment(
            String database) {

        var env =
                new HashMap<String, String>();

        env.put(
                "MIGRATION_DB_URL",
                url(database));

        env.put(
                "MIGRATION_DB_USERNAME",
                PG.getUsername());

        env.put(
                "MIGRATION_DB_PASSWORD",
                PG.getPassword());

        env.put(
                "MIGRATION_ENVIRONMENT",
                "synthetic-test");

        env.put(
                "MIGRATION_RELEASE",
                "0".repeat(40));

        return env;
    }

    private static String url(String database) {
        return PG.getJdbcUrl()
                .replace(
                        "/migration_control",
                        "/" + database);
    }

    private static Connection connection(
            String database)
            throws Exception {

        return DriverManager.getConnection(
                url(database),
                PG.getUsername(),
                PG.getPassword());
    }

    private static void sql(
            String database,
            String sql)
            throws Exception {

        try (var connection =
                     connection(database);
             var statement =
                     connection.createStatement()) {

            statement.execute(sql);
        }
    }

    private int number(String sql)
            throws Exception {

        try (var connection =
                     connection(database);
             var statement =
                     connection.createStatement();
             var rows =
                     statement.executeQuery(sql)) {

            rows.next();

            return rows.getInt(1);
        }
    }

    private void seedRelatedRows()
            throws Exception {

        sql(
                database,
                "INSERT INTO "
                        + S
                        + "sede(nombre,activo) "
                        + "VALUES('synthetic backup campus',true);"
                        + "INSERT INTO "
                        + S
                        + "rol(nombre,tipo_perfil,activo) "
                        + "VALUES('synthetic backup role','ESTUDIANTE',true);"
                        + "INSERT INTO "
                        + S
                        + "permiso(nombre,descripcion,activo) "
                        + "VALUES("
                        + "'synthetic backup permission',"
                        + "'synthetic',true);"
                        + "INSERT INTO "
                        + S
                        + "rol_permiso(rol_id,permiso_id) "
                        + "SELECT r.id,p.id "
                        + "FROM "
                        + S
                        + "rol r "
                        + "CROSS JOIN "
                        + S
                        + "permiso p");
    }

    private String sample()
            throws Exception {

        StringBuilder value =
                new StringBuilder();

        for (String table :
                new String[] {
                        "sede",
                        "rol",
                        "permiso",
                        "rol_permiso"
                }) {

            try (var connection =
                         connection(database);
                 var statement =
                         connection.createStatement();
                 var rows =
                         statement.executeQuery(
                                 "SELECT row_to_json(t)::text "
                                         + "FROM "
                                         + S
                                         + table
                                         + " t "
                                         + "ORDER BY row_to_json(t)::text "
                                         + "COLLATE \"C\"")) {

                value.append(table)
                        .append('\n');

                while (rows.next()) {
                    value.append(
                                    rows.getString(1))
                            .append('\n');
                }
            }
        }

        return value.toString();
    }

    private static String exec(
            String... command)
            throws Exception {

        return MigrationContainerTools.exec(
                PG,
                command);
    }

    private byte[] dump()
            throws Exception {

        return Base64.getMimeDecoder()
                .decode(
                        exec(
                                "sh",
                                "-c",
                                "set -eu; "
                                        + "pg_dump "
                                        + "-U migration_owner "
                                        + "-d "
                                        + database
                                        + " --format=custom "
                                        + "--no-owner "
                                        + "--no-privileges "
                                        + "-f /tmp/fixture.dump; "
                                        + "pg_restore "
                                        + "--list "
                                        + "/tmp/fixture.dump "
                                        + ">/dev/null; "
                                        + "base64 "
                                        + "/tmp/fixture.dump"));
    }

    private void restore(byte[] backup)
            throws Exception {

        String encoded =
                Base64.getEncoder()
                        .encodeToString(backup);

        exec(
                "sh",
                "-c",
                ": > /tmp/restore.b64");

        for (int offset = 0;
             offset < encoded.length();
             offset += 16000) {

            /*
             * Generated Base64 contains no shell metacharacters;
             * never interpolate arbitrary SQL/data paths.
             */
            exec(
                    "sh",
                    "-c",
                    "printf %s '"
                            + encoded.substring(
                            offset,
                            Math.min(
                                    offset + 16000,
                                    encoded.length()))
                            + "' >> /tmp/restore.b64");
        }

        exec(
                "sh",
                "-c",
                "set -eu; "
                        + "base64 -d "
                        + "/tmp/restore.b64 "
                        + ">/tmp/restore.dump; "
                        + "pg_restore "
                        + "-U migration_owner "
                        + "-d "
                        + database
                        + " --exit-on-error "
                        + "--no-owner "
                        + "/tmp/restore.dump");
    }
}