package co.edu.ufps.legal_cases.common.observability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LoggingSafetyConfigurationTest {

    private static final Path APPLICATION_PROPERTIES =
            Path.of("src/main/resources/application.properties");

    private static final Path BREVO_EMAIL_SERVICE =
            Path.of("src/main/java/co/edu/ufps/legal_cases/common/email/BrevoEmailService.java");

    private static final Path FILE_ASSET_RECONCILIATION_SERVICE =
            Path.of("src/main/java/co/edu/ufps/legal_cases/file_storage/service/FileAssetReconciliationService.java");

    @Test
    void applicationPropertiesShouldUseSafeLoggingDefaults() throws IOException {
        String properties = Files.readString(APPLICATION_PROPERTIES);

        assertTrue(properties.contains("spring.jpa.show-sql=${DB_SHOW_SQL:false}"));
        assertTrue(properties.contains("logging.level.root=${LOG_LEVEL_ROOT:INFO}"));
        assertTrue(properties.contains("logging.pattern.console="));
        assertTrue(properties.contains("%X{correlationId"));
        assertFalse(properties.contains("spring.jpa.show-sql=${DB_SHOW_SQL:true}"));
    }

    @Test
    void brevoEmailServiceShouldNotLogExternalBodiesOrMessages() throws IOException {
        String source = Files.readString(BREVO_EMAIL_SERVICE);

        assertFalse(source.contains("response.body()"));
        assertFalse(source.contains("ex.getMessage()"));
        assertFalse(source.contains("Body:"));
        assertFalse(source.contains(", ex);"));
    }

    @Test
    void fileAssetReconciliationShouldNotLogSensitiveExceptionData() throws IOException {
        String source = Files.readString(FILE_ASSET_RECONCILIATION_SERVICE);

        assertFalse(source.contains("asset.getObjectKey(), ex"));
        assertFalse(source.contains(", ex);"));
        assertFalse(source.contains("ex.getMessage()"));
        assertTrue(source.contains("assetId={}"));
        assertTrue(source.contains("tipoExcepcion={}"));
    }
}
