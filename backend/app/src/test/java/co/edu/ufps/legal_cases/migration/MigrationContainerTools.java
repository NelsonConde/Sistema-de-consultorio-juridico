package co.edu.ufps.legal_cases.migration;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Docker CLI also honors DOCKER_HOST for Podman; avoids docker-java exec streaming incompatibilities. */
final class MigrationContainerTools {
    private MigrationContainerTools() { }
    static String exec(PostgreSQLContainer container, String... arguments) throws Exception {
        var command = new ArrayList<>(List.of("docker", "exec", container.getContainerId()));
        command.addAll(List.of(arguments));
        var stdout = Files.createTempFile("migration-command-", ".out");
        var stderr = Files.createTempFile("migration-command-", ".err");
        try {
            var process = new ProcessBuilder(command).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Synthetic database tool timed out");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Synthetic database tool failed: " + Files.readString(stderr));
            }
            return Files.readString(stdout);
        } finally {
            Files.deleteIfExists(stdout); Files.deleteIfExists(stderr);
        }
    }
}
