package co.edu.ufps.legal_cases.file_storage.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

@Service
public class FileAssetService {

    private static final Pattern CONSULTA = Pattern.compile("^(\\d+)(?:/.*)?$");
    private static final Pattern TAREA = Pattern.compile("^tareas-(\\d+)-documentos(?:/.*)?$");
    private static final Pattern RESPUESTA = Pattern.compile("^tareas-(\\d+)-respuestas-(\\d+)(?:/.*)?$");
    private static final Pattern CONCILIACION = Pattern.compile("^conciliacion/(\\d+)(?:/.*)?$");

    private final FileAssetRepository repository;
    private final UsuarioActualService usuarioActualService;
    private final String bucket;

    public FileAssetService(
            FileAssetRepository repository,
            UsuarioActualService usuarioActualService,
            @Value("${supabase.storage.bucket:legal-documents}") String bucket) {
        this.repository = repository;
        this.usuarioActualService = usuarioActualService;
        this.bucket = bucket;
    }

    @Transactional
    public void register(String objectKey, MultipartFile file) {
        ResourceReference reference = resolve(objectKey);
        FileAsset asset = repository.findByBucketAndObjectKey(bucket, objectKey)
                .orElseGet(FileAsset::new);

        asset.setBucket(bucket);
        asset.setObjectKey(objectKey);
        asset.setResourceType(reference.type());
        asset.setResourceId(reference.resourceId());
        asset.setOriginalFileName(file.getOriginalFilename() == null
                ? "unnamed"
                : file.getOriginalFilename());
        asset.setContentType(file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType());
        asset.setSize(file.getSize());
        asset.setChecksum(checksum(file));
        asset.setUploadedBy(usuarioActualService.obtenerUsuarioActual());
        asset.setActive(true);

        repository.save(asset);
    }

    private static String checksum(MultipartFile file) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new BusinessException("No se pudo calcular la huella del archivo");
        }
    }

    private static ResourceReference resolve(String objectKey) {
        Matcher matcher = RESPUESTA.matcher(objectKey);
        if (matcher.matches()) {
            return new ResourceReference("SEGUIMIENTO_RESPUESTA", Long.valueOf(matcher.group(2)));
        }
        matcher = TAREA.matcher(objectKey);
        if (matcher.matches()) {
            return new ResourceReference("SEGUIMIENTO", Long.valueOf(matcher.group(1)));
        }
        matcher = CONCILIACION.matcher(objectKey);
        if (matcher.matches()) {
            return new ResourceReference("CONCILIACION", Long.valueOf(matcher.group(1)));
        }
        matcher = CONSULTA.matcher(objectKey);
        if (matcher.matches()) {
            return new ResourceReference("CONSULTA", Long.valueOf(matcher.group(1)));
        }
        throw new BusinessException("El archivo no está asociado a un recurso válido");
    }

    private record ResourceReference(String type, Long resourceId) {
    }
}
