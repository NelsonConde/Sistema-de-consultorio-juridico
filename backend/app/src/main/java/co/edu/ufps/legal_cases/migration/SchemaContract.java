package co.edu.ufps.legal_cases.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HexFormat;

/** Structural comparison excludes row data, owners, ACLs and Flyway's own history. */
public final class SchemaContract {
    private SchemaContract() { }

    public static String resource(String path) throws IOException {
        try (var input = SchemaContract.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing migration resource");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static String fingerprint(Connection connection) throws SQLException, IOException {
        // Deparsers must qualify names consistently, independently of the login's search_path.
        try (var statement = connection.createStatement()) {
            statement.execute("SET search_path TO pg_catalog");
            StringBuilder canonical = new StringBuilder();
            try (var rows = statement.executeQuery(resource("/db/migration-support/schema-contract.sql"))) {
                while (rows.next()) canonical.append(rows.getString(1)).append('\n');
            }
            return sha256(canonical.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void requireBaseline(Connection connection) throws SQLException, IOException {
        String expected = resource("/db/migration-support/baseline-contract.sha256").trim();
        if (!fingerprint(connection).equals(expected)) {
            throw new IllegalStateException("Legacy schema differs from the reviewed baseline");
        }
    }
}
