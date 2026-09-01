package co.edu.ufps.legal_cases.audit.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AuditMigrationTest {

    @Test
    void versionsStructuredFieldsAndDatabaseImmutability() throws IOException {
        String sql = Files.readString(Path.of(
                "db/migration/V20260830_01__restructure_audit_log.sql"));

        assertTrue(sql.contains("before_state_json"));
        assertTrue(sql.contains("after_state_json"));
        assertTrue(sql.contains("DROP COLUMN IF EXISTS details"));
        assertTrue(sql.contains("BEFORE UPDATE OR DELETE"));
        assertTrue(sql.contains("CREATE TRIGGER trigger_prevent_audit_mod"));
        assertTrue(sql.contains("\"DB_consultorioJuridico\".audit_logs"));
    }
}
