-- Compatibiliza la restricción heredada con el flujo de carga por URL firmada.
-- Aplicar en el esquema objetivo antes de usar FileAssetStatus.UPLOADING/READY.
ALTER TABLE "DB_consultorioJuridico".file_asset
    DROP CONSTRAINT IF EXISTS file_asset_status_check;

ALTER TABLE "DB_consultorioJuridico".file_asset
    ADD CONSTRAINT file_asset_status_check CHECK (
        status IN (
            'UPLOADING',
            'READY',
            'DELETING',
            'DELETED',
            'ORPHANED',
            'PENDING',
            'ACTIVE',
            'FAILED',
            'DELETE_PENDING'
        )
    );
