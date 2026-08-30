-- AUD-01 / AUD-02: bitácora probatoria estructurada e inmutable.
-- Ejecutar con una cuenta propietaria del esquema antes de desplegar el backend.
BEGIN;

DROP TRIGGER IF EXISTS trigger_prevent_audit_mod
    ON "DB_consultorioJuridico".audit_logs;
DROP FUNCTION IF EXISTS "DB_consultorioJuridico".prevent_audit_modification();

ALTER TABLE "DB_consultorioJuridico".audit_logs
    ADD COLUMN IF NOT EXISTS actor_username varchar(150),
    ADD COLUMN IF NOT EXISTS outcome varchar(20),
    ADD COLUMN IF NOT EXISTS occurred_at timestamptz,
    ADD COLUMN IF NOT EXISTS source varchar(20),
    ADD COLUMN IF NOT EXISTS correlation_id varchar(100),
    ADD COLUMN IF NOT EXISTS ip_address varchar(45),
    ADD COLUMN IF NOT EXISTS user_agent varchar(512),
    ADD COLUMN IF NOT EXISTS reason_code varchar(120),
    ADD COLUMN IF NOT EXISTS reason text,
    ADD COLUMN IF NOT EXISTS before_state_json text,
    ADD COLUMN IF NOT EXISTS after_state_json text,
    ADD COLUMN IF NOT EXISTS metadata_json text;

-- Conserva actor e instante de filas heredadas sin trasladar `details`, porque
-- contenía representaciones JVM y argumentos potencialmente sensibles.
DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'DB_consultorioJuridico'
          AND table_name = 'audit_logs'
          AND column_name = 'username'
    ) THEN
        EXECUTE 'UPDATE "DB_consultorioJuridico".audit_logs
                 SET actor_username = COALESCE(actor_username, username, ''SYSTEM'')';
    ELSE
        UPDATE "DB_consultorioJuridico".audit_logs
        SET actor_username = COALESCE(actor_username, 'SYSTEM');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'DB_consultorioJuridico'
          AND table_name = 'audit_logs'
          AND column_name = 'timestamp'
    ) THEN
        EXECUTE 'UPDATE "DB_consultorioJuridico".audit_logs
                 SET occurred_at = COALESCE(
                     occurred_at,
                     "timestamp" AT TIME ZONE ''America/Bogota'',
                     CURRENT_TIMESTAMP)';
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'DB_consultorioJuridico'
          AND table_name = 'audit_logs'
          AND column_name = 'created_date'
    ) THEN
        EXECUTE 'UPDATE "DB_consultorioJuridico".audit_logs
                 SET occurred_at = COALESCE(
                     occurred_at,
                     created_date AT TIME ZONE ''America/Bogota'',
                     CURRENT_TIMESTAMP)';
    ELSE
        UPDATE "DB_consultorioJuridico".audit_logs
        SET occurred_at = COALESCE(occurred_at, CURRENT_TIMESTAMP);
    END IF;
END
$migration$;

UPDATE "DB_consultorioJuridico".audit_logs
SET outcome = COALESCE(outcome, 'SUCCESS'),
    source = COALESCE(source, 'SYSTEM'),
    correlation_id = COALESCE(correlation_id, 'legacy-' || id::text);

ALTER TABLE "DB_consultorioJuridico".audit_logs
    DROP COLUMN IF EXISTS username,
    DROP COLUMN IF EXISTS "timestamp",
    DROP COLUMN IF EXISTS created_date,
    DROP COLUMN IF EXISTS details,
    ALTER COLUMN actor_username SET NOT NULL,
    ALTER COLUMN action TYPE varchar(100),
    ALTER COLUMN action SET NOT NULL,
    ALTER COLUMN entity_name TYPE varchar(100),
    ALTER COLUMN entity_name SET NOT NULL,
    ALTER COLUMN entity_id TYPE varchar(150),
    ALTER COLUMN outcome SET NOT NULL,
    ALTER COLUMN occurred_at SET NOT NULL,
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN correlation_id SET NOT NULL;

ALTER TABLE "DB_consultorioJuridico".audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_outcome_check,
    DROP CONSTRAINT IF EXISTS audit_logs_source_check,
    ADD CONSTRAINT audit_logs_outcome_check
        CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    ADD CONSTRAINT audit_logs_source_check
        CHECK (source IN ('HTTP', 'SYSTEM'));

CREATE INDEX IF NOT EXISTS idx_audit_logs_occurred_at
    ON "DB_consultorioJuridico".audit_logs (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_occurred_at
    ON "DB_consultorioJuridico".audit_logs (actor_username, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id
    ON "DB_consultorioJuridico".audit_logs (correlation_id);

CREATE FUNCTION "DB_consultorioJuridico".prevent_audit_modification()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables: UPDATE y DELETE están prohibidos';
END
$function$;

CREATE TRIGGER trigger_prevent_audit_mod
BEFORE UPDATE OR DELETE ON "DB_consultorioJuridico".audit_logs
FOR EACH ROW
EXECUTE FUNCTION "DB_consultorioJuridico".prevent_audit_modification();

COMMIT;
