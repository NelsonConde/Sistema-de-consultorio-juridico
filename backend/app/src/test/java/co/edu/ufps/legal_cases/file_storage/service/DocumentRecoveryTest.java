package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class DocumentRecoveryTest {

    @Test
    @DisplayName("QA-DOC-16: los bytes sobreviven al reemplazo de la instancia backend")
    void bytesPersistenAlReemplazarInstanciaBackend() throws IOException {
        SyntheticObjectStore externalStore = new SyntheticObjectStore();
        InMemoryStorageProvider firstBackend = new InMemoryStorageProvider(externalStore);
        byte[] original = "documento sintetico persistente".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String key = "consulta/41/persistente.txt";
        firstBackend.store(new MockMultipartFile(
                "file", "persistente.txt", "text/plain", original), key);

        InMemoryStorageProvider replacementBackend = new InMemoryStorageProvider(externalStore);
        byte[] afterReplacement = replacementBackend.load(key).getInputStream().readAllBytes();

        assertArrayEquals(original, afterReplacement);
        assertEquals(sha256(original), sha256(afterReplacement));
        assertEquals("text/plain", replacementBackend.head(key).contentType());
    }

    @Test
    @DisplayName("QA-DOC-17: backup y restauracion conservan bytes, SHA-256 y metadata")
    void backupYRestauracionSinteticaConservanIntegridadYMetadata() throws IOException {
        SyntheticObjectStore source = new SyntheticObjectStore();
        InMemoryStorageProvider sourceProvider = new InMemoryStorageProvider(source);
        sourceProvider.store(file("uno.txt", "text/plain", "objeto uno"), "consulta/41/uno.txt");
        sourceProvider.store(file("dos.pdf", "application/pdf", "%PDF-sintetico"), "conciliacion/9/dos.pdf");

        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        Map<String, StoredObject> backup = source.snapshot();
        SyntheticObjectStore restored = new SyntheticObjectStore();
        restored.restore(backup);
        long durationNanos = System.nanoTime() - startedNanos;
        Instant finishedAt = Instant.now();

        assertEquals(backup.keySet(), restored.snapshot().keySet());
        for (Map.Entry<String, StoredObject> entry : backup.entrySet()) {
            StoredObject restoredObject = restored.snapshot().get(entry.getKey());
            String checksumBefore = sha256(entry.getValue().bytes());
            String checksumAfter = sha256(restoredObject.bytes());
            assertArrayEquals(entry.getValue().bytes(), restoredObject.bytes());
            assertEquals(checksumBefore, checksumAfter);
            assertEquals(entry.getValue().contentType(), restoredObject.contentType());
            assertEquals(entry.getValue().contentLength(), restoredObject.contentLength());
            System.out.printf(
                    "QA_DOC_17_RECOVERY_OBJECT key=%s sha256Before=%s sha256After=%s contentType=%s contentLength=%d%n",
                    entry.getKey(),
                    checksumBefore,
                    checksumAfter,
                    entry.getValue().contentType(),
                    entry.getValue().contentLength());
        }

        System.out.printf(
                "QA_DOC_17_RECOVERY start=%s end=%s durationNanos=%d objects=%d checksumMatch=true bytesMatch=true metadataMatch=true%n",
                startedAt,
                finishedAt,
                durationNanos,
                backup.size());
    }

    private static MockMultipartFile file(String name, String contentType, String contents) {
        return new MockMultipartFile(
                "file", name, contentType, contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record StoredObject(byte[] bytes, String contentType, long contentLength) {
        StoredObject copy() {
            return new StoredObject(bytes.clone(), contentType, contentLength);
        }
    }

    private static final class SyntheticObjectStore {
        private final Map<String, StoredObject> objects = new LinkedHashMap<>();

        void put(String key, byte[] bytes, String contentType) {
            objects.put(key, new StoredObject(bytes.clone(), contentType, bytes.length));
        }

        StoredObject get(String key) {
            StoredObject value = objects.get(key);
            if (value == null) {
                throw new IllegalArgumentException("Objeto sintetico inexistente");
            }
            return value.copy();
        }

        void delete(String key) {
            objects.remove(key);
        }

        Map<String, StoredObject> snapshot() {
            Map<String, StoredObject> snapshot = new LinkedHashMap<>();
            objects.forEach((key, value) -> snapshot.put(key, value.copy()));
            return snapshot;
        }

        void restore(Map<String, StoredObject> backup) {
            backup.forEach((key, value) -> objects.put(key, value.copy()));
        }
    }

    private static final class InMemoryStorageProvider implements StorageProvider {
        private final SyntheticObjectStore store;

        private InMemoryStorageProvider(SyntheticObjectStore store) {
            this.store = store;
        }

        @Override
        public String store(MultipartFile file, String objectKey) {
            try {
                store.put(objectKey, file.getBytes(), file.getContentType());
                return objectKey;
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public Resource load(String objectKey) {
            StoredObject object = store.get(objectKey);
            return new InputStreamResource(new ByteArrayInputStream(object.bytes())) {
                @Override
                public long contentLength() {
                    return object.contentLength();
                }
            };
        }

        @Override
        public void delete(String objectKey) {
            store.delete(objectKey);
        }

        @Override
        public List<String> list(String prefix) {
            return store.snapshot().keySet().stream().filter(key -> key.startsWith(prefix)).toList();
        }

        @Override
        public List<String> listDirectories(String prefix) {
            return List.of();
        }

        @Override
        public StorageObjectMetadata head(String objectKey) {
            StoredObject object = store.get(objectKey);
            return new StorageObjectMetadata(object.contentLength(), object.contentType());
        }

        @Override
        public PresignedUpload createUploadUrl(
                String objectKey, String contentType, long contentLength, Duration validity) {
            throw new UnsupportedOperationException("No se usa en recuperacion sintetica");
        }

        @Override
        public PresignedDownload createDownloadUrl(String objectKey, Duration validity) {
            throw new UnsupportedOperationException("No se usa en recuperacion sintetica");
        }
    }
}
