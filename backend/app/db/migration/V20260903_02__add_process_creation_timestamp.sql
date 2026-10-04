ALTER TABLE "DB_consultorioJuridico".proceso
    ADD COLUMN fecha_creacion TIMESTAMP;

-- Los procesos históricos no almacenaban una fecha propia de creación.
-- Para conservar la semántica estadística previa, se usa la fecha de la
-- consulta asociada como valor heredado de compatibilidad.
UPDATE "DB_consultorioJuridico".proceso p
SET fecha_creacion = c.fecha::timestamp
FROM "DB_consultorioJuridico".consulta c
WHERE c.id = p.consulta_id
  AND p.fecha_creacion IS NULL;

ALTER TABLE "DB_consultorioJuridico".proceso
    ALTER COLUMN fecha_creacion SET NOT NULL;
