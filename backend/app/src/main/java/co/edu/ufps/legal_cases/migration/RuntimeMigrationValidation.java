package co.edu.ufps.legal_cases.migration;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Boot runs this validation before creating EntityManagerFactory; DDL belongs to the separate job. */
@Configuration(proxyBeanMethods = false)
public class RuntimeMigrationValidation {
    @Bean
    FlywayMigrationStrategy validateMigrationHistoryOnly() {
        return flyway -> flyway.validate();
    }
}
