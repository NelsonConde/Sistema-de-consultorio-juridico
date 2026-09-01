-- DB-03 - Plan de recuperacion para control de concurrencia optimista
--
-- IMPORTANTE:
-- Este script elimina las columnas de version agregadas por DB-03.
-- Solo debe ejecutarse si tambien se revierte el codigo de aplicacion
-- que utiliza @Version y los contratos HTTP que envian/validan version.
--
-- No debe ejecutarse de forma aislada sobre una version de la aplicacion
-- que todavia dependa de estas columnas.

BEGIN;

ALTER TABLE "DB_consultorioJuridico".reunion_conciliacion
    DROP COLUMN IF EXISTS version;

ALTER TABLE "DB_consultorioJuridico".conciliacion
    DROP COLUMN IF EXISTS version;

ALTER TABLE "DB_consultorioJuridico".seguimiento_respuesta
    DROP COLUMN IF EXISTS version;

ALTER TABLE "DB_consultorioJuridico".seguimiento
    DROP COLUMN IF EXISTS version;

ALTER TABLE "DB_consultorioJuridico".proceso
    DROP COLUMN IF EXISTS version;

ALTER TABLE "DB_consultorioJuridico".persona
    DROP COLUMN IF EXISTS version;

ALTER TABLE "DB_consultorioJuridico".consulta
    DROP COLUMN IF EXISTS version;

COMMIT;
