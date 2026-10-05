-- Rollback para V20260905_01__version_document_model.sql
-- Ejecutar para revertir la estructura de versionado documental en caso de contingencia.
BEGIN;

-- 1. Eliminar índices creados
DROP INDEX IF EXISTS "DB_consultorioJuridico".uk_file_asset_doc_vigente;
DROP INDEX IF EXISTS "DB_consultorioJuridico".idx_file_asset_resource;
DROP INDEX IF EXISTS "DB_consultorioJuridico".idx_file_asset_doc_logico;
DROP INDEX IF EXISTS "DB_consultorioJuridico".idx_file_asset_status;
DROP INDEX IF EXISTS "DB_consultorioJuridico".idx_file_asset_ref_anterior;

-- 2. Eliminar restricciones añadidas
ALTER TABLE "DB_consultorioJuridico".file_asset
    DROP CONSTRAINT IF EXISTS uk_file_asset_doc_version,
    DROP CONSTRAINT IF EXISTS chk_file_asset_version,
    DROP CONSTRAINT IF EXISTS fk_file_asset_referencia_anterior;

-- 3. Revertir status a valores previos
UPDATE "DB_consultorioJuridico".file_asset
SET status = 'READY'
WHERE status IN ('VIGENTE', 'HISTORICO');

-- 4. Restaurar restricción previa de estados
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

-- 5. Eliminar columnas de versionado
ALTER TABLE "DB_consultorioJuridico".file_asset
    DROP COLUMN IF EXISTS referencia_anterior_id,
    DROP COLUMN IF EXISTS origen,
    DROP COLUMN IF EXISTS tipo_documental,
    DROP COLUMN IF EXISTS version,
    DROP COLUMN IF EXISTS documento_logico;

COMMIT;
