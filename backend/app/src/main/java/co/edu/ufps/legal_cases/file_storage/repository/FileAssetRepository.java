package co.edu.ufps.legal_cases.file_storage.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    Optional<FileAsset> findByBucketAndObjectKey(String bucket, String objectKey);

    Optional<FileAsset> findByUploadId(UUID uploadId);

    List<FileAsset> findByResourceTypeAndResourceIdAndStatusOrderByCreatedAtDesc(
            String resourceType,
            Long resourceId,
            FileAssetStatus status);

    List<FileAsset> findByResourceTypeAndResourceIdAndStatusInOrderByCreatedAtDesc(
            String resourceType,
            Long resourceId,
            List<FileAssetStatus> statuses);

    List<FileAsset> findByStatusAndUpdatedAtBefore(FileAssetStatus status, java.time.LocalDateTime cutoff);

    List<FileAsset> findByResourceTypeAndResourceIdAndStatusAndObjectKeyNot(
            String resourceType,
            Long resourceId,
            FileAssetStatus status,
            String objectKey);
}
