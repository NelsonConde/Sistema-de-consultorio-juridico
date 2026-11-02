package co.edu.ufps.legal_cases.migration;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.DriverManager;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=false"})
class RuntimeMigrationValidationPostgreSqlTest {
    @Container static final PostgreSQLContainer PG = initializedDatabase();
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired DataSource dataSource;
    @Autowired Flyway runtimeFlyway;

    static PostgreSQLContainer initializedDatabase() {
        var pg = new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("runtime_validation")
                .withUsername("migration_owner").withPassword("synthetic-owner-only");
        pg.start();
        var output = new ByteArrayOutputStream();
        int result = DatabaseMigrationCommand.run("migrate", Map.of(
                "MIGRATION_DB_URL", pg.getJdbcUrl(), "MIGRATION_DB_USERNAME", pg.getUsername(),
                "MIGRATION_DB_PASSWORD", pg.getPassword(), "MIGRATION_ENVIRONMENT", "synthetic-runtime",
                "MIGRATION_RELEASE", "0".repeat(40)), new PrintStream(output));
        if (result != 0) { pg.stop(); throw new IllegalStateException(output.toString()); }
        try (var connection = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE runtime_dml LOGIN PASSWORD 'synthetic-runtime-only'");
            // Bind the fixed synthetic role as psql would; exercise the operator script's actual DDL.
            statement.execute(java.nio.file.Files.readString(java.nio.file.Path.of("db/operations/grant_runtime_permissions.sql"))
                    .replace(":'runtime_role'", "'runtime_dml'").replace(":\"runtime_role\"", "\"runtime_dml\""));
        } catch (Exception failure) { pg.stop(); throw new IllegalStateException(failure); }
        return pg;
    }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "runtime_dml");
        registry.add("spring.datasource.password", () -> "synthetic-runtime-only");
    }

    @Test void bootValidatesFlywayAndAllEntitiesWithoutDdlPermission() throws Exception {
        assertTrue(entityManagerFactory.isOpen());
        new RuntimeMigrationValidation().validateMigrationHistoryOnly().migrate(runtimeFlyway);
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertEquals("runtime_dml", connection.getMetaData().getUserName());
            try (var rows = statement.executeQuery("SELECT has_schema_privilege(current_user,'DB_consultorioJuridico','CREATE')")) {
                rows.next(); assertFalse(rows.getBoolean(1));
            }
            var denied = assertThrows(java.sql.SQLException.class,
                    () -> statement.execute("CREATE TABLE \"DB_consultorioJuridico\".forbidden_ddl(id integer)"));
            assertEquals("42501", denied.getSQLState());
            assertThrows(java.sql.SQLException.class,
                    () -> statement.execute("UPDATE \"DB_consultorioJuridico\".flyway_schema_history SET checksum=0"));
        }
        System.out.println("SCRUM-332 Spring Boot Flyway validate + Hibernate validate + DML-only runtime PASS");
    }

    @Test void startupStrategyRejectsAnUnmigratedDatabaseInsteadOfApplyingDdl() throws Exception {
        try (var connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var statement = connection.createStatement()) { statement.execute("CREATE DATABASE unmigrated_runtime"); }
        var flyway = DatabaseMigrationCommand.configuration(PG.getJdbcUrl().replace("/runtime_validation", "/unmigrated_runtime"),
                "runtime_dml", "synthetic-runtime-only").createSchemas(false).load();
        assertThrows(FlywayException.class,
                () -> new RuntimeMigrationValidation().validateMigrationHistoryOnly().migrate(flyway));
    }
}
