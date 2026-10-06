package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;

/** Operational test: explicitly run with -Dtest and LocalStack prepared. */
class DocumentBackupRestoreIT {

    @Test
    void restoresDeletedS3ObjectExclusivelyFromHostBackupFile() throws Exception {
        // This test is restricted to the agreed local emulator and synthetic account.
        assertEquals("http://localhost:4566", environment("DOCUMENT_S3_ENDPOINT"));
        assertEquals("us-east-1", environment("DOCUMENT_S3_REGION"));
        assertTrue("test".equals(environment("DOCUMENT_S3_ACCESS_KEY")), "Synthetic account required");
        assertTrue("test".equals(environment("DOCUMENT_S3_SECRET_KEY")), "Synthetic account required");
        assertEquals("legal-documents", environment("DOCUMENT_S3_BUCKET"));

        String session = UUID.randomUUID().toString();
        String objectKey = "document/qa-doc-17/" + session + "/evidencia.txt";
        byte[] originalBytes = ("DOCUMENT QA-DOC-17 synthetic backup evidence\nsession=" + session + "\n")
                .getBytes(StandardCharsets.UTF_8);
        Path backupDirectory = Files.createTempDirectory("document-qa-doc-17-").toRealPath();
        Path workspace = Path.of("").toAbsolutePath().normalize();
        assertFalse(backupDirectory.startsWith(workspace), "Backup must be outside the workspace");
        Path backupFile = backupDirectory.resolve("document.backup");
        Path metadataFile = backupDirectory.resolve("metadata.properties");
        SupabaseStorageProvider provider = new SupabaseStorageProvider(
                environment("DOCUMENT_S3_ENDPOINT"), environment("DOCUMENT_S3_REGION"),
                environment("DOCUMENT_S3_ACCESS_KEY"), environment("DOCUMENT_S3_SECRET_KEY"),
                environment("DOCUMENT_S3_BUCKET"));
        try {
            provider.store(new MockMultipartFile("file", "evidencia.txt", "text/plain", originalBytes), objectKey);
            StorageProvider.StorageObjectMetadata originalMetadata = provider.head(objectKey);
            byte[] originalStoredBytes = read(provider, objectKey);
            assertArrayEquals(originalBytes, originalStoredBytes);
            assertEquals(originalBytes.length, originalMetadata.contentLength());
            assertEquals("text/plain", originalMetadata.contentType());
            String originalSha256 = sha256(originalStoredBytes);
            System.out.printf("QA_DOC_17 phase=CREATE session=%s bytes=%d sha256=%s contentType=%s%n",
                    session, originalStoredBytes.length, originalSha256, originalMetadata.contentType());

            // Download a separate material copy directly from S3, closing both streams.
            try (InputStream source = provider.load(objectKey).getInputStream();
                    OutputStream destination = Files.newOutputStream(backupFile)) {
                source.transferTo(destination);
            }
            Properties manifest = new Properties();
            manifest.setProperty("objectKey", objectKey);
            manifest.setProperty("size", Long.toString(originalMetadata.contentLength()));
            manifest.setProperty("contentType", originalMetadata.contentType());
            manifest.setProperty("sha256", originalSha256);
            try (OutputStream output = Files.newOutputStream(metadataFile)) {
                manifest.store(output, "Synthetic DOCUMENT backup metadata; no credentials");
            }
            assertTrue(Files.isRegularFile(backupFile));
            String backupSha256 = sha256(Files.readAllBytes(backupFile));
            assertEquals(originalSha256, backupSha256);
            System.out.printf("QA_DOC_17 phase=BACKUP backupExists=true externalToRepository=true sha256=%s%n",
                    backupSha256);

            provider.delete(objectKey);
            assertThrows(FileNotFoundException.class, () -> provider.head(objectKey));
            assertTrue(Files.isRegularFile(backupFile));
            System.out.println("QA_DOC_17 phase=LOSS activeObjectDeleted=true activeObjectAbsent=true backupExists=true");

            // Reload the manifest; neither original array is used as the restoration source.
            Properties restorationMetadata = new Properties();
            try (InputStream input = Files.newInputStream(metadataFile)) {
                restorationMetadata.load(input);
            }
            String restoredKey = restorationMetadata.getProperty("objectKey");
            assertEquals(objectKey, restoredKey);
            assertEquals(restorationMetadata.getProperty("sha256"), sha256(Files.readAllBytes(backupFile)));
            assertEquals(Long.parseLong(restorationMetadata.getProperty("size")), Files.size(backupFile));

            Instant recoveryStart = Instant.now();
            long startNanos = System.nanoTime();
            try (InputStream backupInput = Files.newInputStream(backupFile)) {
                provider.store(new MockMultipartFile("file", "evidencia.txt",
                        restorationMetadata.getProperty("contentType"), backupInput), restoredKey);
            }
            StorageProvider.StorageObjectMetadata restoredMetadata = provider.head(restoredKey);
            byte[] restoredBytes = read(provider, restoredKey);
            String restoredSha256 = sha256(restoredBytes);
            assertArrayEquals(originalStoredBytes, restoredBytes);
            assertEquals(originalSha256, restoredSha256);
            assertEquals(originalMetadata, restoredMetadata);
            assertEquals(objectKey, restoredKey);
            long recoveryNanos = System.nanoTime() - startNanos;
            Instant recoveryEnd = Instant.now();
            System.out.printf("QA_DOC_17 phase=RESTORE source=backup-file bytesMatch=true sha256Match=true metadataMatch=true objectKeyMatch=true bytes=%d sha256=%s start=%s end=%s recoveryTimeMs=%.3f%n",
                    restoredBytes.length, restoredSha256, recoveryStart, recoveryEnd, recoveryNanos / 1_000_000.0);
        } finally {
            try {
                provider.delete(objectKey);
                assertThrows(FileNotFoundException.class, () -> provider.head(objectKey));
                System.out.println("QA_DOC_17 phase=CLEANUP restoredObjectAbsent=true");
            } finally {
                try {
                    provider.close();
                } finally {
                    Files.deleteIfExists(backupFile);
                    Files.deleteIfExists(metadataFile);
                    Files.delete(backupDirectory);
                    assertFalse(Files.exists(backupFile));
                    assertFalse(Files.exists(metadataFile));
                    assertFalse(Files.exists(backupDirectory));
                    System.out.println("QA_DOC_17 phase=CLEANUP backupFileAbsent=true metadataFileAbsent=true temporaryDirectoryAbsent=true");
                }
            }
        }
    }

    private static byte[] read(StorageProvider provider, String key) throws Exception {
        try (InputStream input = provider.load(key).getInputStream()) {
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
