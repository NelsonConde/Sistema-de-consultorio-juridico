package co.edu.ufps.legal_cases.file_storage.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;

/** Reintenta cerrar estados documentales que quedaron incompletos. */
@Service
public class FileAssetReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(FileAssetReconciliationService.class);
    private static final int STALE_HOURS = 1;

    private final FileAssetService fileAssetService;
    private final FileAssetRepository repository;
    private final StorageProvider storageProvider;

    public FileAssetReconciliationService(
            FileAssetService fileAssetService,
            FileAssetRepository repository,
            StorageProvider storageProvider) {
        this.fileAssetService = fileAssetService;
        this.repository = repository;
        this.storageProvider = storageProvider;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void reconcileStaleAssets() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STALE_HOURS);
        reconcile(repository.findByStatusAndUpdatedAtBefore(FileAssetStatus.PENDING, cutoff));
        reconcile(repository.findByStatusAndUpdatedAtBefore(FileAssetStatus.DELETE_PENDING, cutoff));
    }

    private void reconcile(List<FileAsset> assets) {
        for (FileAsset asset : assets) {
            try {
                storageProvider.delete(asset.getObjectKey());
                fileAssetService.markFailed(asset.getObjectKey());
            } catch (RuntimeException ex) {
                log.error("No se pudo reconciliar el objeto documental {}", asset.getObjectKey(), ex);
            }
        }
    }

}
