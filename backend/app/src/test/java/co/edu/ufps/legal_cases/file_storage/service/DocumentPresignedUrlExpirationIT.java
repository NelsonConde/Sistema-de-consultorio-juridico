package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;

class DocumentPresignedUrlExpirationIT {

    private static final Duration URL_TTL = Duration.ofSeconds(2);
    private static final long EXPIRATION_WAIT_MILLIS = 3_000L;

    @Test
    @DisplayName("QA-DOC-13: LocalStack rechaza una URL firmada vencida y acepta una nueva")
    void urlFirmadaVencidaEsRechazadaPorLocalStack() throws Exception {
        SupabaseStorageProvider provider = new SupabaseStorageProvider(
                "http://localhost:4566",
                "us-east-1",
                "test",
                "test",
                "legal-documents");
        String objectKey = "document/qa-doc-13/" + UUID.randomUUID() + "/evidencia.txt";
        byte[] originalBytes = "DOCUMENT QA-DOC-13 synthetic evidence"
                .getBytes(StandardCharsets.UTF_8);
        boolean objectStored = false;

        try {
            provider.store(new MockMultipartFile(
                    "file",
                    "evidencia.txt",
                    "text/plain",
                    originalBytes), objectKey);
            objectStored = true;

            StorageProvider.StorageObjectMetadata metadata = provider.head(objectKey);
            assertEquals(originalBytes.length, metadata.contentLength());

            StorageProvider.PresignedDownload firstSignedUrl =
                    provider.createDownloadUrl(objectKey, URL_TTL);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest sameSignedRequest = HttpRequest.newBuilder()
                    .uri(URI.create(firstSignedUrl.url()))
                    .GET()
                    .build();

            HttpResponse<byte[]> beforeExpiration = httpClient.send(
                    sameSignedRequest, HttpResponse.BodyHandlers.ofByteArray());
            System.out.printf(
                    "QA_DOC_13 phase=before-expiration status=%d bytes=%d%n",
                    beforeExpiration.statusCode(),
                    beforeExpiration.body().length);
            Thread.sleep(EXPIRATION_WAIT_MILLIS);

            HttpResponse<byte[]> afterExpiration = httpClient.send(
                    sameSignedRequest, HttpResponse.BodyHandlers.ofByteArray());
            System.out.printf(
                    "QA_DOC_13 phase=after-expiration status=%d bytes=%d%n",
                    afterExpiration.statusCode(),
                    afterExpiration.body().length);
            StorageProvider.PresignedDownload renewedSignedUrl =
                    provider.createDownloadUrl(objectKey, URL_TTL);
            HttpRequest renewedRequest = HttpRequest.newBuilder()
                    .uri(URI.create(renewedSignedUrl.url()))
                    .GET()
                    .build();
            HttpResponse<byte[]> renewedResponse = httpClient.send(
                    renewedRequest, HttpResponse.BodyHandlers.ofByteArray());
            System.out.printf(
                    "QA_DOC_13 phase=renewed-url status=%d bytes=%d%n",
                    renewedResponse.statusCode(),
                    renewedResponse.body().length);

            System.out.printf(
                    "QA_DOC_13 integrity=%s comparedBytes=%d%n",
                    java.util.Arrays.equals(originalBytes, renewedResponse.body())
                            ? "bytes-match"
                            : "bytes-mismatch",
                    originalBytes.length);

            assertAll(
                    () -> assertEquals(200, beforeExpiration.statusCode()),
                    () -> assertNotEquals(200, afterExpiration.statusCode(),
                            "LocalStack debe rechazar la misma URL despues de su expiracion"),
                    () -> assertEquals(200, renewedResponse.statusCode()),
                    () -> assertArrayEquals(originalBytes, renewedResponse.body()));
        } finally {
            try {
                if (objectStored) {
                    provider.delete(objectKey);
                    assertThrows(FileNotFoundException.class, () -> provider.head(objectKey));
                    System.out.println("QA_DOC_13 cleanup=object-deleted");
                }
            } finally {
                provider.close();
            }
        }
    }
}
