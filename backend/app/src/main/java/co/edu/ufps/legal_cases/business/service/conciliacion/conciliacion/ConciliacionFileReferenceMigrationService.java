package co.edu.ufps.legal_cases.business.service.conciliacion.conciliacion;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.repository.conciliacion.ConciliacionRepository;
import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.file_storage.repository.FileAssetRepository;

/**
 * Puente de una sola dirección para instalaciones que aún tienen objetos
 * documentales de conciliación registrados con el esquema de rutas anterior.
 */
@Service
public class ConciliacionFileReferenceMigrationService {

    private final ConciliacionRepository conciliacionRepository;
    private final FileAssetRepository fileAssetRepository;

    public ConciliacionFileReferenceMigrationService(
            ConciliacionRepository conciliacionRepository,
            FileAssetRepository fileAssetRepository) {
        this.conciliacionRepository = conciliacionRepository;
        this.fileAssetRepository = fileAssetRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillLegacyReferences() {
        for (Conciliacion conciliacion : conciliacionRepository.findAll()) {
            if (conciliacion.getId() == null) {
                continue;
            }

            List<FileAsset> assets = fileAssetRepository
                    .findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
                            "CONCILIACION",
                            conciliacion.getId(),
                            List.of(FileAssetStatus.ACTIVE, FileAssetStatus.READY));

            if (conciliacion.getDocumentoSolicitud() == null) {
                findBySuffix(assets, "solicitud.pdf").ifPresent(conciliacion::setDocumentoSolicitud);
            }
            if (conciliacion.getActa() == null) {
                findBySuffix(assets, "acta.pdf").ifPresent(conciliacion::setActa);
            }

            if (conciliacion.getDocumentoSolicitud() != null
                    || conciliacion.getActa() != null) {
                conciliacionRepository.save(conciliacion);
            }
        }
    }

    private static java.util.Optional<FileAsset> findBySuffix(
            List<FileAsset> assets,
            String suffix) {
        return assets.stream()
                .filter(asset -> asset.getObjectKey() != null
                        && (asset.getObjectKey().endsWith("/" + suffix)
                                || asset.getObjectKey().endsWith("-" + suffix)))
                .findFirst();
    }
}
