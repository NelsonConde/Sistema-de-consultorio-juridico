package co.edu.ufps.legal_cases.file_storage.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    Optional<FileAsset> findByBucketAndObjectKey(String bucket, String objectKey);

    Optional<FileAsset> findByUploadId(UUID uploadId);

    Optional<FileAsset> findByDocumentoLogicoAndStatus(UUID documentoLogico, FileAssetStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fa FROM FileAsset fa WHERE fa.documentoLogico = :documentoLogico AND fa.status = :status")
    Optional<FileAsset> findVigenteForUpdate(
            @Param("documentoLogico") UUID documentoLogico,
            @Param("status") FileAssetStatus status);

    @Query("SELECT COALESCE(MAX(fa.version), 0) FROM FileAsset fa WHERE fa.documentoLogico = :documentoLogico")
    Integer findMaxVersionByDocumentoLogico(@Param("documentoLogico") UUID documentoLogico);

    List<FileAsset> findByDocumentoLogicoOrderByVersionDesc(UUID documentoLogico);

    List<FileAsset> findByDocumentoLogico(UUID documentoLogico);

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
