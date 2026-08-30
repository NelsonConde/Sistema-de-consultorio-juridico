BEGIN;

ALTER TABLE "DB_consultorioJuridico".consulta
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE "DB_consultorioJuridico".consulta
SET version = 0
WHERE version IS NULL;

ALTER TABLE "DB_consultorioJuridico".consulta
    ALTER COLUMN version SET DEFAULT 0;

ALTER TABLE "DB_consultorioJuridico".consulta
    ALTER COLUMN version SET NOT NULL;

ALTER TABLE "DB_consultorioJuridico".persona
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE "DB_consultorioJuridico".persona
SET version = 0
WHERE version IS NULL;

ALTER TABLE "DB_consultorioJuridico".persona
    ALTER COLUMN version SET DEFAULT 0;

ALTER TABLE "DB_consultorioJuridico".persona
    ALTER COLUMN version SET NOT NULL;

COMMIT;
