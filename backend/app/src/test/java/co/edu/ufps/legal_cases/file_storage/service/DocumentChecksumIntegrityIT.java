package co.edu.ufps.legal_cases.file_storage.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.model.FileResourceType;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

/** QA-DOC-10: explicit diagnostic reproduction, excluded from normal Surefire discovery. */
class DocumentChecksumIntegrityIT {

    @Test
    @DisplayName("QA-DOC-10: rechaza SHA-256 valido que no coincide con bytes almacenados")
    void checksumValidoPeroDistintoDebeSerRechazado() {
        FileAssetService assetService = mock(FileAssetService.class);
        FileResourceAuthorizationService authorizationService = mock(FileResourceAuthorizationService.class);
        StorageProvider storageProvider = mock(StorageProvider.class);
        UsuarioActualService currentUser = mock(UsuarioActualService.class);
        FileResourceService service = new FileResourceService(
                assetService, mock(FileValidationService.class), authorizationService,
                storageProvider, currentUser, Duration.ofMinutes(10), Duration.ofMinutes(5));

        UUID uploadId = UUID.randomUUID();
        byte[] storedBytes = "contenido-real".getBytes(StandardCharsets.UTF_8);
        UsuarioSistema user = new UsuarioSistema();
        user.setId(9L);
        FileAsset asset = new FileAsset();
        asset.setUploadId(uploadId);
        asset.setUploadedBy(user);
        asset.setResourceType("CONSULTA");
        asset.setResourceId(41L);
        asset.setObjectKey("consulta/41/evidencia.bin");
        asset.setOriginalFileName("evidencia.bin");
        asset.setContentType("application/octet-stream");
        asset.setSize((long) storedBytes.length);
        asset.setChecksum("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        asset.setStatus(FileAssetStatus.UPLOADING);

        when(assetService.findByUploadId(uploadId)).thenReturn(asset);
        when(currentUser.obtenerUsuarioActualId()).thenReturn(9L);
        when(authorizationService.parseType("CONSULTA")).thenReturn(FileResourceType.CONSULTA);
        when(storageProvider.head(asset.getObjectKey())).thenReturn(
                new StorageProvider.StorageObjectMetadata(storedBytes.length, "application/octet-stream"));
        when(storageProvider.load(asset.getObjectKey())).thenReturn(new ByteArrayResource(storedBytes));
        when(assetService.markReady(uploadId, storedBytes.length, "application/octet-stream"))
                .thenReturn(asset);

        assertThrows(BusinessException.class, () -> service.complete(uploadId, null),
                "complete() debe recalcular SHA-256 y rechazar bytes que no coinciden");
    }
}
