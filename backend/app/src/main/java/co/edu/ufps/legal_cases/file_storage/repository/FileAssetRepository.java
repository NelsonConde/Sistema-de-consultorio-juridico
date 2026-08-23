package co.edu.ufps.legal_cases.file_storage.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    Optional<FileAsset> findByBucketAndObjectKey(String bucket, String objectKey);

    List<FileAsset> findByStatusAndUpdatedAtBefore(FileAssetStatus status, java.time.LocalDateTime cutoff);

    List<FileAsset> findByResourceTypeAndResourceIdAndStatusAndObjectKeyNot(
            String resourceType,
            Long resourceId,
            FileAssetStatus status,
            String objectKey);
}
