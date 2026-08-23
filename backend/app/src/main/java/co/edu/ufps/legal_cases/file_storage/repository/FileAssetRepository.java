package co.edu.ufps.legal_cases.file_storage.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    Optional<FileAsset> findByBucketAndObjectKey(String bucket, String objectKey);
}
