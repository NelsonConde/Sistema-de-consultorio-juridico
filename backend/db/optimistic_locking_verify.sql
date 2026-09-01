-- DB-03 - Verificacion posterior a optimistic_locking.sql
--
-- Resultado esperado:
-- 1. La primera consulta debe devolver exactamente 7 filas.
-- 2. Todas las columnas deben ser bigint, NOT NULL y con DEFAULT 0.
-- 3. Todas las consultas de filas sin version deben devolver 0.

SELECT
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'DB_consultorioJuridico'
  AND table_name IN (
      'consulta',
      'persona',
      'proceso',
      'seguimiento',
      'seguimiento_respuesta',
      'conciliacion',
      'reunion_conciliacion'
  )
  AND column_name = 'version'
ORDER BY table_name;

SELECT 'consulta' AS tabla, COUNT(*) AS filas_sin_version
FROM "DB_consultorioJuridico".consulta
WHERE version IS NULL

UNION ALL

SELECT 'persona', COUNT(*)
FROM "DB_consultorioJuridico".persona
WHERE version IS NULL

UNION ALL

SELECT 'proceso', COUNT(*)
FROM "DB_consultorioJuridico".proceso
WHERE version IS NULL

UNION ALL

SELECT 'seguimiento', COUNT(*)
FROM "DB_consultorioJuridico".seguimiento
WHERE version IS NULL

UNION ALL

SELECT 'seguimiento_respuesta', COUNT(*)
FROM "DB_consultorioJuridico".seguimiento_respuesta
WHERE version IS NULL

UNION ALL

SELECT 'conciliacion', COUNT(*)
FROM "DB_consultorioJuridico".conciliacion
WHERE version IS NULL

UNION ALL

SELECT 'reunion_conciliacion', COUNT(*)
FROM "DB_consultorioJuridico".reunion_conciliacion
WHERE version IS NULL

ORDER BY tabla;
