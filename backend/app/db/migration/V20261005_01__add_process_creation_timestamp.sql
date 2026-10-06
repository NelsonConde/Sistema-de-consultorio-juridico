ALTER TABLE "DB_consultorioJuridico".proceso
    ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP WITHOUT TIME ZONE;

-- Los procesos historicos no almacenaban una fecha propia de creacion.
-- Se usa la fecha de la consulta asociada para conservar la semantica previa.
UPDATE "DB_consultorioJuridico".proceso p
SET fecha_creacion = c.fecha::timestamp
FROM "DB_consultorioJuridico".consulta c
WHERE c.id = p.consulta_id
  AND p.fecha_creacion IS NULL;

DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM "DB_consultorioJuridico".proceso
        WHERE fecha_creacion IS NULL
    ) THEN
        RAISE EXCEPTION
            'No se puede establecer proceso.fecha_creacion como NOT NULL: existen procesos sin fecha recuperable';
    END IF;
END
$migration$;

ALTER TABLE "DB_consultorioJuridico".proceso
    ALTER COLUMN fecha_creacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_creacion SET NOT NULL;
