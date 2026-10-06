package co.edu.ufps.legal_cases.file_storage.model;

public enum FileAssetStatus {
    /** Estados del flujo nuevo de carga de archivos. */
    UPLOADING,
    READY,
    DELETING,
    DELETED,
    ORPHANED,

    /** Estados del modelo documental versionado. */
    VIGENTE,
    HISTORICO,

    /** Estados conservados para compatibilidad durante la migración. */
    PENDING,
    ACTIVE,
    FAILED,
    DELETE_PENDING
}
