package co.edu.ufps.legal_cases.migration;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/** Separate deployment job: deliberately starts neither Spring nor HTTP nor business initializers. */
public final class DatabaseMigrationCommand {
    public static final String SCHEMA = "DB_consultorioJuridico";
    public static final String BASELINE_VERSION = "20261015.01";
    public static final String BASELINE_SQL = "/db/migration/V20261015_01__baseline_current_schema.sql";
    private static final long DEPLOYMENT_LOCK = 33220261015L;
    private DatabaseMigrationCommand() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar backend-migrations.jar migrate|validate|baseline|inspect");
            System.exit(2);
        }
        System.exit(run(args[0], System.getenv(), System.out));
    }

    public static FluentConfiguration configuration(String url, String user, String password) {
        return Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").schemas(SCHEMA).defaultSchema(SCHEMA)
                .table("flyway_schema_history").baselineVersion(BASELINE_VERSION)
                .baselineDescription("Reviewed equivalent legacy schema")
                .baselineOnMigrate(false).validateOnMigrate(true).validateMigrationNaming(true)
                .outOfOrder(false).cleanDisabled(true).ignoreMigrationPatterns(new String[0])
                .loggers(SanitizedMigrationLogCreator.class.getName());
    }

    public static int run(String operation, Map<String, String> environment, PrintStream output) {
        try {
            if (!Set.of("migrate", "validate", "baseline", "inspect").contains(operation)) {
                throw new IllegalArgumentException("Unsupported migration operation");
            }
            String url = required(environment, "MIGRATION_DB_URL");
            String user = required(environment, "MIGRATION_DB_USERNAME");
            String password = required(environment, "MIGRATION_DB_PASSWORD");
            String target = required(environment, "MIGRATION_ENVIRONMENT");
            String release = required(environment, "MIGRATION_RELEASE");
            if (!target.matches("[a-zA-Z0-9_.-]{1,64}") || !release.matches("[a-fA-F0-9]{40}")) {
                throw new IllegalArgumentException("Invalid deployment identity");
            }
            if (!url.startsWith("jdbc:postgresql://") || url.contains("@")
                    || url.matches("(?is).*[?&](user|password|sslpassword)=.*")) {
                throw new IllegalArgumentException("Supply credentials separately from the PostgreSQL URL");
            }
            if (operation.equals("baseline")
                    && (!BASELINE_VERSION.equals(environment.get("MIGRATION_BASELINE_ACK"))
                    || !environment.getOrDefault("MIGRATION_BACKUP_SHA256", "").matches("[a-fA-F0-9]{64}"))) {
                throw new IllegalArgumentException("Explicit baseline and verified backup acknowledgement required");
            }
            try (var connection = DriverManager.getConnection(url, user, password);
                 var statement = connection.createStatement()) {
                if (connection.getMetaData().getDatabaseMajorVersion() != 16) {
                    throw new IllegalStateException("Only PostgreSQL 16 has been qualified for this baseline");
                }
                statement.execute("SET lock_timeout = '30s'");
                // Fail immediately instead of silently racing another deployment job.
                try (var lock = statement.executeQuery("SELECT pg_try_advisory_lock(" + DEPLOYMENT_LOCK + ")")) {
                    lock.next();
                    if (!lock.getBoolean(1)) throw new IllegalStateException("Another migration job is active");
                }
                Flyway flyway = configuration(url, user, password).load();
                verifyPackagedMigrations(flyway);
                switch (operation) {
                    case "baseline" -> {
                        try (var history = statement.executeQuery(
                                "SELECT to_regclass('\"DB_consultorioJuridico\".flyway_schema_history') IS NULL")) {
                            history.next();
                            if (!history.getBoolean(1)) throw new IllegalStateException("History already exists");
                        }
                        SchemaContract.requireBaseline(connection);
                        flyway.baseline();
                    }
                    case "migrate" -> flyway.migrate();
                    case "validate" -> flyway.validate();
                    case "inspect" -> { /* Read-only structural inventory. */ }
                    default -> throw new IllegalArgumentException("Unsupported operation");
                }
                var current = flyway.info().current();
                output.printf("MIGRATION_OK operation=%s environment=%s release=%s version=%s schema_sha256=%s baseline_sql_sha256=%s%n",
                        operation, target, release, current == null ? "none" : current.getVersion(),
                        SchemaContract.fingerprint(connection),
                        SchemaContract.sha256(SchemaContract.resource(BASELINE_SQL).getBytes(StandardCharsets.UTF_8)));
                return 0;
            }
        } catch (Exception failure) {
            // Driver/Flyway messages can include connection details or SQL. Never print them here.
            output.println("MIGRATION_FAILED category=" + failure.getClass().getSimpleName());
            return 1;
        }
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static void verifyPackagedMigrations(Flyway flyway) throws Exception {
        var expected = new java.util.HashMap<String,String>();
        for (String line : SchemaContract.resource("/db/migration-support/migrations.sha256").lines().toList()) {
            if (line.isBlank()) continue;
            String[] parts = line.split("  ", 2);
            if (parts.length != 2 || !parts[0].matches("[a-f0-9]{64}")
                    || !parts[1].matches("V[0-9_]+__[a-z0-9_]+\\.sql")
                    || expected.put(parts[1], parts[0]) != null) {
                throw new IllegalStateException("Invalid packaged migration manifest");
            }
            String actual = SchemaContract.sha256(SchemaContract.resource("/db/migration/" + parts[1]).getBytes(StandardCharsets.UTF_8));
            if (!actual.equals(parts[0])) throw new IllegalStateException("Packaged migration hash differs");
        }
        if (expected.isEmpty()) throw new IllegalStateException("Empty migration manifest");
        for (var migration : flyway.info().all()) {
            if (migration.getScript().endsWith(".sql") && !expected.containsKey(migration.getScript())) {
                throw new IllegalStateException("Migration missing from reviewed manifest");
            }
        }
    }
}
