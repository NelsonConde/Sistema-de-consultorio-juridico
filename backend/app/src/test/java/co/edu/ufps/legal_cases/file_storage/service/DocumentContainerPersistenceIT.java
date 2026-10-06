package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;

class DocumentContainerPersistenceIT {

    private static final String CONTENT_TYPE = "text/plain";

    @Test
    @DisplayName("QA-DOC-16: persiste bytes entre contenedores backend independientes")
    void ejecutaFaseOperacionalConfigurada() throws IOException {
        Phase phase = Phase.valueOf(requiredEnvironment("DOCUMENT_PHASE").toUpperCase(Locale.ROOT));
        String sessionId = requiredEnvironment("DOCUMENT_SESSION_ID");
        if (!sessionId.matches("^[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("DOCUMENT_SESSION_ID contiene caracteres no permitidos");
        }

        byte[] expectedBytes = expectedBytes(sessionId);
        String expectedChecksum = sha256(expectedBytes);
        String objectKey = "document/qa-doc-16/" + sessionId + "/evidencia.txt";
        String containerId = requiredEnvironment("HOSTNAME");

        SupabaseStorageProvider provider = new SupabaseStorageProvider(
                requiredEnvironment("DOCUMENT_S3_ENDPOINT"),
                requiredEnvironment("DOCUMENT_S3_REGION"),
                requiredEnvironment("DOCUMENT_S3_ACCESS_KEY"),
                requiredEnvironment("DOCUMENT_S3_SECRET_KEY"),
                requiredEnvironment("DOCUMENT_S3_BUCKET"));
        try {
            switch (phase) {
                case WRITE -> write(provider, objectKey, expectedBytes, expectedChecksum, containerId);
                case VERIFY -> verify(provider, objectKey, expectedBytes, expectedChecksum, containerId);
                case CLEANUP -> cleanup(provider, objectKey, containerId);
            }
        } finally {
            provider.close();
        }
    }

    private static void write(
            StorageProvider provider,
            String objectKey,
            byte[] expectedBytes,
            String expectedChecksum,
            String containerId) throws IOException {
        provider.store(new MockMultipartFile(
                "file", "evidencia.txt", CONTENT_TYPE, expectedBytes), objectKey);
        StorageProvider.StorageObjectMetadata metadata = provider.head(objectKey);
        byte[] storedBytes = provider.load(objectKey).getInputStream().readAllBytes();

        assertEquals(expectedBytes.length, metadata.contentLength());
        assertEquals(CONTENT_TYPE, metadata.contentType());
        assertArrayEquals(expectedBytes, storedBytes);
        assertEquals(expectedChecksum, sha256(storedBytes));

        System.out.printf(
                "QA_DOC_16 phase=WRITE containerId=%s objectStored=true bytes=%d sha256=%s contentType=%s%n",
                containerId,
                storedBytes.length,
                expectedChecksum,
                metadata.contentType());
    }

    private static void verify(
            StorageProvider provider,
            String objectKey,
            byte[] expectedBytes,
            String expectedChecksum,
            String containerId) throws IOException {
        StorageProvider.StorageObjectMetadata metadata = provider.head(objectKey);
        byte[] downloadedBytes = provider.load(objectKey).getInputStream().readAllBytes();
        String downloadedChecksum = sha256(downloadedBytes);

        assertArrayEquals(expectedBytes, downloadedBytes);
        assertEquals(expectedChecksum, downloadedChecksum);
        assertEquals(expectedBytes.length, metadata.contentLength());
        assertEquals(CONTENT_TYPE, metadata.contentType());

        System.out.printf(
                "QA_DOC_16 phase=VERIFY containerId=%s storeCalled=false bytesMatch=true sha256Match=true metadataMatch=true bytes=%d sha256=%s contentType=%s%n",
                containerId,
                downloadedBytes.length,
                downloadedChecksum,
                metadata.contentType());
    }

    private static void cleanup(StorageProvider provider, String objectKey, String containerId) {
        provider.delete(objectKey);
        assertThrows(FileNotFoundException.class, () -> provider.head(objectKey));
        System.out.printf(
                "QA_DOC_16 phase=CLEANUP containerId=%s objectDeleted=true objectAbsent=true%n",
                containerId);
    }

    private static byte[] expectedBytes(String sessionId) {
        return ("DOCUMENT QA-DOC-16 synthetic document\n" + "session=" + sessionId + "\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta variable requerida: " + name);
        }
        return value;
    }

    private enum Phase {
        WRITE,
        VERIFY,
        CLEANUP
    }
}
