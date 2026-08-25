package co.edu.ufps.legal_cases.security.repository.invariant;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
abstract class AdministracionPostgreSqlTestBase {

    @Container
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("legal_cases_test")
                    .withUsername("legal_cases")
                    .withPassword("legal_cases")
                    .withInitScript(
                            "db/administracion-invariant-init.sql");

    @DynamicPropertySource
    static void configurarPostgreSql(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                POSTGRESQL::getJdbcUrl);
        registry.add(
                "spring.datasource.username",
                POSTGRESQL::getUsername);
        registry.add(
                "spring.datasource.password",
                POSTGRESQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop");
        registry.add(
                "spring.jpa.properties.hibernate.default_schema",
                () -> "\"DB_consultorioJuridico\"");
        registry.add(
                "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces",
                () -> "true");
        registry.add(
                "spring.jpa.show-sql",
                () -> "false");
    }
}