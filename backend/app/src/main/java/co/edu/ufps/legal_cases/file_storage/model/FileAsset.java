package co.edu.ufps.legal_cases.file_storage.model;

import java.time.LocalDateTime;

import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "file_asset", uniqueConstraints = {
        @UniqueConstraint(name = "uk_file_asset_bucket_object_key", columnNames = {"bucket", "object_key"})
})
@Getter
@Setter
public class FileAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 64)
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private UsuarioSistema uploadedBy;

    @Column(nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileAssetStatus status = FileAssetStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        normalizeState();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeState();
    }

    private void normalizeState() {
        if (status == null) {
            status = Boolean.TRUE.equals(active) ? FileAssetStatus.ACTIVE : FileAssetStatus.FAILED;
        }
        active = status == FileAssetStatus.ACTIVE;
    }
}
