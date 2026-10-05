package co.edu.ufps.legal_cases.file_storage.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.ufps.legal_cases.file_storage.exception.FileNotFoundException;
import co.edu.ufps.legal_cases.file_storage.exception.FileStorageException;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;

class DocumentReconciliationTest {

    @Test
    @DisplayName("QA-DOC-14: reconcilia PENDING y UPLOADING viejos")
    void reconciliaCargasIncompletasViejas() {
        FileAssetService assetService = mock(FileAssetService.class);
        FileAssetRepository repository = mock(FileAssetRepository.class);
        StorageProvider storage = mock(StorageProvider.class);
        FileAsset pending = asset(1L, "consulta/1/pending", FileAssetStatus.PENDING);
        FileAsset uploading = asset(2L, "consulta/1/uploading", FileAssetStatus.UPLOADING);
        when(repository.findByStatusAndUpdatedAtBefore(eq(FileAssetStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(pending));
        when(repository.findByStatusAndUpdatedAtBefore(eq(FileAssetStatus.UPLOADING), any(LocalDateTime.class)))
                .thenReturn(List.of(uploading));

        new FileAssetReconciliationService(assetService, repository, storage).reconcileStaleAssets();

        verify(storage).delete(pending.getObjectKey());
        verify(storage).delete(uploading.getObjectKey());
        verify(assetService).markFailedByObjectKey(pending.getObjectKey());
        verify(assetService).markFailedByObjectKey(uploading.getObjectKey());
    }

    @Test
    @DisplayName("QA-DOC-15: DELETE_PENDING cubre exito, objeto inexistente y error temporal")
    void reconciliaDeletePendingSegunResultadoDelProveedor() {
        FileAssetService assetService = mock(FileAssetService.class);
        FileAssetRepository repository = mock(FileAssetRepository.class);
        StorageProvider storage = mock(StorageProvider.class);
        FileAsset success = asset(1L, "consulta/1/success", FileAssetStatus.DELETE_PENDING);
        FileAsset missing = asset(2L, "consulta/1/missing", FileAssetStatus.DELETE_PENDING);
        FileAsset temporary = asset(3L, "consulta/1/temporary", FileAssetStatus.DELETE_PENDING);
        when(repository.findByStatusAndUpdatedAtBefore(eq(FileAssetStatus.DELETE_PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(success, missing, temporary));
        doThrow(new FileNotFoundException("objeto sintetico inexistente"))
                .when(storage).delete(missing.getObjectKey());
        doThrow(new FileStorageException("fallo temporal sintetico"))
                .when(storage).delete(temporary.getObjectKey());

        new FileAssetReconciliationService(assetService, repository, storage).reconcileStaleAssets();

        verify(assetService).markDeleted(success.getId());
        verify(assetService).markDeleted(missing.getId());
        verify(assetService, never()).markDeleted(temporary.getId());
    }

    private static FileAsset asset(Long id, String objectKey, FileAssetStatus status) {
        FileAsset asset = new FileAsset();
        asset.setId(id);
        asset.setObjectKey(objectKey);
        asset.setStatus(status);
        return asset;
    }
}
