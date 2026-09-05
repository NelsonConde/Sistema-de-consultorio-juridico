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

    @Query("""
        SELECT fa FROM FileAsset fa
        LEFT JOIN FETCH fa.uploadedBy u
        LEFT JOIN FETCH fa.referenciaAnterior ref
        WHERE (
            (fa.resourceType = 'CONSULTA' AND fa.resourceId = :consultaId)
            OR (fa.resourceType = 'SEGUIMIENTO' AND fa.resourceId IN (
                SELECT s.id FROM Seguimiento s WHERE s.consulta.id = :consultaId
            ))
            OR ((fa.resourceType = 'RESPUESTA' OR fa.resourceType = 'SEGUIMIENTO_RESPUESTA') AND fa.resourceId IN (
                SELECT r.id FROM SeguimientoRespuesta r WHERE r.seguimiento.consulta.id = :consultaId
            ))
            OR (fa.resourceType = 'PROCESO' AND fa.resourceId IN (
                SELECT pr.id FROM Proceso pr WHERE pr.consulta.id = :consultaId
            ))
            OR (fa.resourceType = 'CONCILIACION' AND fa.resourceId IN (
                SELECT c.id FROM Conciliacion c WHERE c.consulta.id = :consultaId
            ))
        )
        AND fa.status IN :statuses
        AND (:tipoDocumental IS NULL OR UPPER(fa.tipoDocumental) = UPPER(:tipoDocumental))
        AND (:resourceType IS NULL OR fa.resourceType = :resourceType OR (:resourceType = 'RESPUESTA' AND fa.resourceType = 'SEGUIMIENTO_RESPUESTA'))
        AND (:origen IS NULL OR UPPER(fa.origen) = UPPER(:origen))
        AND (
            :autor IS NULL
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :autor, '%'))
            OR (:autorId IS NOT NULL AND u.id = :autorId)
        )
        AND (CAST(:fechaDesde AS java.time.LocalDateTime) IS NULL OR fa.createdAt >= :fechaDesde)
        AND (CAST(:fechaHasta AS java.time.LocalDateTime) IS NULL OR fa.createdAt <= :fechaHasta)
        ORDER BY fa.createdAt DESC, fa.id DESC
    """)
    List<FileAsset> findExpedienteFiles(
            @Param("consultaId") Long consultaId,
            @Param("statuses") List<FileAssetStatus> statuses,
            @Param("tipoDocumental") String tipoDocumental,
            @Param("resourceType") String resourceType,
            @Param("origen") String origen,
            @Param("autor") String autor,
            @Param("autorId") Long autorId,
            @Param("fechaDesde") java.time.LocalDateTime fechaDesde,
            @Param("fechaHasta") java.time.LocalDateTime fechaHasta);
}
