package co.edu.ufps.legal_cases.file_storage.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.security.service.context.UsuarioActualService;

class FileResourceServiceDb03Test {

    @Test
    void discardStoredAssetEliminaObjetoFisicoYMarcaMetadataFallida() {
        FileAssetService fileAssetService = mock(FileAssetService.class);
        StorageProvider storageProvider = mock(StorageProvider.class);

        FileResourceService service = new FileResourceService(
                fileAssetService,
                mock(FileValidationService.class),
                mock(FileResourceAuthorizationService.class),
                storageProvider,
                mock(UsuarioActualService.class),
                Duration.ofMinutes(10),
                Duration.ofMinutes(5));

        UUID uploadId = UUID.randomUUID();
        FileAsset asset = new FileAsset();
        asset.setObjectKey("conciliacion/1/acta.pdf");
        asset.setUploadId(uploadId);

        service.discardStoredAsset(asset);

        verify(storageProvider).delete("conciliacion/1/acta.pdf");
        verify(fileAssetService).markUploadFailed(uploadId);
    }
}
