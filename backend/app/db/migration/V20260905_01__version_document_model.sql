-- Migración para versionar modelo documental y metadatos (file_asset).
-- Ejecutar con una cuenta propietaria del esquema antes de desplegar el backend.
BEGIN;

-- 1. Agregar columnas necesarias
ALTER TABLE "DB_consultorioJuridico".file_asset
    ADD COLUMN IF NOT EXISTS documento_logico uuid,
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS tipo_documental varchar(60) DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS origen varchar(40) DEFAULT 'SISTEMA',
    ADD COLUMN IF NOT EXISTS referencia_anterior_id bigint;

-- 2. Migración y normalización de datos existentes (snapshot legado)
UPDATE "DB_consultorioJuridico".file_asset
SET documento_logico = gen_random_uuid()
WHERE documento_logico IS NULL;

UPDATE "DB_consultorioJuridico".file_asset
SET version = 1
WHERE version IS NULL;

UPDATE "DB_consultorioJuridico".file_asset
SET origen = 'MIGRADO'
WHERE origen IS NULL OR origen = 'SISTEMA';

UPDATE "DB_consultorioJuridico".file_asset
SET tipo_documental = CASE
    WHEN resource_type = 'CONCILIACION' AND original_file_name ILIKE '%solicitud%' THEN 'CONCILIACION_SOLICITUD'
    WHEN resource_type = 'CONCILIACION' AND original_file_name ILIKE '%acta%' THEN 'CONCILIACION_ACTA'
    WHEN resource_type = 'CONCILIACION' THEN 'CONCILIACION_DOCUMENTO'
    WHEN resource_type = 'CONSULTA' THEN 'CONSULTA_ANEXO'
    WHEN resource_type = 'RESPUESTA' OR resource_type = 'SEGUIMIENTO_RESPUESTA' THEN 'RESPUESTA_EVIDENCIA'
    WHEN resource_type = 'SEGUIMIENTO' THEN 'SEGUIMIENTO_ANEXO'
    ELSE 'GENERAL'
END
WHERE tipo_documental IS NULL OR tipo_documental = 'GENERAL';

-- Archivos que estaban activos/listos pasan a ser la versión VIGENTE de su documento lógico
UPDATE "DB_consultorioJuridico".file_asset
SET status = 'VIGENTE'
WHERE status IN ('READY', 'ACTIVE');

-- 3. Enforzar NOT NULL en campos requeridos
ALTER TABLE "DB_consultorioJuridico".file_asset
    ALTER COLUMN documento_logico SET NOT NULL,
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN tipo_documental SET NOT NULL,
    ALTER COLUMN origen SET NOT NULL;

-- 4. Claves foráneas y chequeos
ALTER TABLE "DB_consultorioJuridico".file_asset
    DROP CONSTRAINT IF EXISTS fk_file_asset_referencia_anterior;

ALTER TABLE "DB_consultorioJuridico".file_asset
    ADD CONSTRAINT fk_file_asset_referencia_anterior
    FOREIGN KEY (referencia_anterior_id)
    REFERENCES "DB_consultorioJuridico".file_asset (id)
    ON DELETE RESTRICT;

ALTER TABLE "DB_consultorioJuridico".file_asset
    DROP CONSTRAINT IF EXISTS chk_file_asset_version;

ALTER TABLE "DB_consultorioJuridico".file_asset
    ADD CONSTRAINT chk_file_asset_version
    CHECK (version >= 1);

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
            'DELETE_PENDING',
            'VIGENTE',
            'HISTORICO'
        )
    );

-- 5. Restricción de versión única por documento lógico
ALTER TABLE "DB_consultorioJuridico".file_asset
    DROP CONSTRAINT IF EXISTS uk_file_asset_doc_version;

ALTER TABLE "DB_consultorioJuridico".file_asset
    ADD CONSTRAINT uk_file_asset_doc_version
    UNIQUE (documento_logico, version);

-- 6. Índices optimizados y unicidad de una sola versión VIGENTE por documento lógico
DROP INDEX IF EXISTS "DB_consultorioJuridico".uk_file_asset_doc_vigente;
CREATE UNIQUE INDEX uk_file_asset_doc_vigente
    ON "DB_consultorioJuridico".file_asset (documento_logico)
    WHERE (status = 'VIGENTE');

CREATE INDEX IF NOT EXISTS idx_file_asset_resource
    ON "DB_consultorioJuridico".file_asset (resource_type, resource_id, status);

CREATE INDEX IF NOT EXISTS idx_file_asset_doc_logico
    ON "DB_consultorioJuridico".file_asset (documento_logico);

CREATE INDEX IF NOT EXISTS idx_file_asset_status
    ON "DB_consultorioJuridico".file_asset (status);

CREATE INDEX IF NOT EXISTS idx_file_asset_ref_anterior
    ON "DB_consultorioJuridico".file_asset (referencia_anterior_id);

COMMIT;
