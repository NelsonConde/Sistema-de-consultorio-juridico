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
import java.util.UUID;
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

    /** Identificador público de la sesión de carga; nunca es una clave del bucket. */
    @Column(name = "upload_id", unique = true, length = 36)
    private UUID uploadId;

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

    @Column(name = "documento_logico", nullable = false)
    private UUID documentoLogico;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "tipo_documental", nullable = false, length = 60)
    private String tipoDocumental = "GENERAL";

    @Column(name = "origen", nullable = false, length = 40)
    private String origen = "SISTEMA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referencia_anterior_id")
    private FileAsset referenciaAnterior;

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
        if (documentoLogico == null) {
            documentoLogico = UUID.randomUUID();
        }
        if (version == null) {
            version = 1;
        }
        if (tipoDocumental == null || tipoDocumental.isBlank()) {
            tipoDocumental = "GENERAL";
        }
        if (origen == null || origen.isBlank()) {
            origen = "SISTEMA";
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
            status = Boolean.TRUE.equals(active) ? FileAssetStatus.VIGENTE : FileAssetStatus.FAILED;
        }
        active = status == FileAssetStatus.VIGENTE
                || status == FileAssetStatus.ACTIVE
                || status == FileAssetStatus.READY;
    }
}
