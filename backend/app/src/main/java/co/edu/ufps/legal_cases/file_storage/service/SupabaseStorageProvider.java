package co.edu.ufps.legal_cases.file_storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Proveedor S3 para Supabase Storage. Las credenciales se leen únicamente de
 * variables de entorno inyectadas por la configuración de Spring.
 */
@Service
public class SupabaseStorageProvider implements StorageProvider {

    private final String bucket;
    private final S3Client client;
    private final S3Presigner presigner;

    public SupabaseStorageProvider(
            @Value("${supabase.storage.endpoint}") String endpoint,
            @Value("${supabase.storage.region}") String region,
            @Value("${supabase.storage.access-key}") String accessKey,
            @Value("${supabase.storage.secret-key}") String secretKey,
            @Value("${supabase.storage.bucket:legal-documents}") String bucket) {
        this.bucket = requireValue(bucket, "supabase.storage.bucket");
        String resolvedEndpoint = requireValue(endpoint, "supabase.storage.endpoint");
        String resolvedRegion = requireValue(region, "supabase.storage.region");
        String resolvedAccessKey = requireValue(accessKey, "supabase.storage.access-key");
        String resolvedSecretKey = requireValue(secretKey, "supabase.storage.secret-key");

        this.client = S3Client.builder()
                .endpointOverride(URI.create(resolvedEndpoint))
                .region(Region.of(resolvedRegion))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(resolvedAccessKey, resolvedSecretKey)))
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(resolvedEndpoint))
                .region(Region.of(resolvedRegion))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(resolvedAccessKey, resolvedSecretKey)))
                .build();
    }

    @Override
    public String store(MultipartFile file, String objectKey) {
        String key = requireValue(objectKey, "objectKey");
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType() == null
                            ? "application/octet-stream"
                            : file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException | S3Exception ex) {
            throw new FileStorageException("No se pudo guardar el objeto documental", ex);
        }
    }

    @Override
    public Resource load(String objectKey) {
        String key = requireValue(objectKey, "objectKey");
        try {
            HeadObjectResponse metadata = client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            return new AbstractResource() {
                @Override
                public InputStream getInputStream() throws IOException {
                    try {
                        ResponseInputStream<?> input = client.getObject(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build());
                        return input;
                    } catch (S3Exception ex) {
                        throw new IOException("No se pudo abrir el objeto documental", ex);
                    }
                }

                @Override
                public long contentLength() {
                    return metadata.contentLength();
                }

                @Override
                public String getFilename() {
                    int separator = key.lastIndexOf('/');
                    return separator >= 0 ? key.substring(separator + 1) : key;
                }

                @Override
                public String getDescription() {
                    return "Objeto documental " + key;
                }
            };
        } catch (S3Exception ex) {
            if (ex instanceof NoSuchKeyException || ex.statusCode() == 404) {
                throw new FileNotFoundException("Archivo no encontrado " + key, ex);
            }
            throw new FileStorageException("No se pudo cargar el objeto documental", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(requireValue(objectKey, "objectKey"))
                    .build());
        } catch (S3Exception ex) {
            throw new FileStorageException("No se pudo eliminar el objeto documental", ex);
        }
    }

    @Override
    public List<String> list(String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);
        try {
            List<String> keys = new ArrayList<>();
            String continuationToken = null;
            do {
                var builder = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(normalizedPrefix)
                        .continuationToken(continuationToken);
                var page = client.listObjectsV2(builder.build());
                page.contents().forEach(object -> keys.add(relativeKey(object.key(), normalizedPrefix)));
                continuationToken = page.nextContinuationToken();
            } while (continuationToken != null);
            return keys;
        } catch (S3Exception ex) {
            throw new FileStorageException("No se pudieron listar los objetos documentales", ex);
        }
    }

    @Override
    public List<String> listDirectories(String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);
        Set<String> directories = new LinkedHashSet<>();
        listAllKeys().stream()
                .filter(key -> normalizedPrefix.isEmpty() || key.startsWith(normalizedPrefix))
                .filter(key -> key.lastIndexOf('/') >= 0)
                .map(key -> key.substring(0, key.lastIndexOf('/')))
                .filter(directory -> !directory.isBlank())
                .forEach(directories::add);
        return directories.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public PresignedUpload createUploadUrl(
            String objectKey,
            String contentType,
            long contentLength,
            Duration validity) {
        try {
            var putRequest = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(requireValue(objectKey, "objectKey"))
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            var presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(validity)
                    .putObjectRequest(putRequest)
                    .build());

            return new PresignedUpload(presigned.url().toString(),
                    Instant.now().plus(validity));
        } catch (RuntimeException ex) {
            throw new FileStorageException("No se pudo preparar la carga documental", ex);
        }
    }

    @Override
    public StorageObjectMetadata head(String objectKey) {
        try {
            HeadObjectResponse response = client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(requireValue(objectKey, "objectKey"))
                    .build());
            return new StorageObjectMetadata(response.contentLength(), response.contentType());
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new FileNotFoundException("Objeto documental no encontrado", ex);
            }
            throw new FileStorageException("No se pudo verificar el objeto documental", ex);
        }
    }

    @Override
    public PresignedDownload createDownloadUrl(String objectKey, Duration validity) {
        try {
            var getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(requireValue(objectKey, "objectKey"))
                    .build();

            var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(validity)
                    .getObjectRequest(getRequest)
                    .build());

            return new PresignedDownload(presigned.url().toString(),
                    Instant.now().plus(validity));
        } catch (RuntimeException ex) {
            throw new FileStorageException("No se pudo preparar la descarga documental", ex);
        }
    }

    private List<String> listAllKeys() {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;
        do {
            var page = client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .continuationToken(continuationToken)
                    .build());
            page.contents().forEach(object -> keys.add(object.key()));
            continuationToken = page.nextContinuationToken();
        } while (continuationToken != null);
        return keys;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.replace('\\', '/');
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static String relativeKey(String key, String prefix) {
        return prefix.isEmpty() ? key : key.substring(prefix.length());
    }

    private static String requireValue(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new FileStorageException("Falta la configuración requerida: " + property);
        }
        return value;
    }

    @PreDestroy
    void close() {
        client.close();
        presigner.close();
    }
}
