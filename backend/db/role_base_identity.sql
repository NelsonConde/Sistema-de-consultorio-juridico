BEGIN;

-- ============================================================================
-- Identidad estable de roles base
--
-- Este script incorpora una identidad técnica e inmutable para los cinco
-- roles base del sistema.
--
-- El nombre del rol continúa siendo una etiqueta editable y no debe utilizarse
-- como identificador funcional para autorización, registro de usuarios o
-- inicialización de datos.
--
-- Los roles personalizados conservan codigo_base en NULL.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. Agregar columna de identidad estable.
-- ----------------------------------------------------------------------------

ALTER TABLE "DB_consultorioJuridico".rol
ADD COLUMN IF NOT EXISTS codigo_base VARCHAR(30);


-- ----------------------------------------------------------------------------
-- 2. Adoptar los cinco roles base existentes.
--
-- El nombre se utiliza únicamente durante esta adopción del modelo anterior.
-- Después de la migración, la aplicación resuelve los roles base mediante
-- codigo_base y no mediante nombre.
-- ----------------------------------------------------------------------------

UPDATE "DB_consultorioJuridico".rol
SET codigo_base = 'ADMINISTRADOR'
WHERE codigo_base IS NULL
  AND nombre = 'Administrador'
  AND tipo_perfil = 'ADMINISTRATIVO';

UPDATE "DB_consultorioJuridico".rol
SET codigo_base = 'ASESOR'
WHERE codigo_base IS NULL
  AND nombre = 'Asesor'
  AND tipo_perfil = 'ASESOR';

UPDATE "DB_consultorioJuridico".rol
SET codigo_base = 'ESTUDIANTE'
WHERE codigo_base IS NULL
  AND nombre = 'Estudiante'
  AND tipo_perfil = 'ESTUDIANTE';

UPDATE "DB_consultorioJuridico".rol
SET codigo_base = 'MONITOR'
WHERE codigo_base IS NULL
  AND nombre = 'Monitor'
  AND tipo_perfil = 'MONITOR';

UPDATE "DB_consultorioJuridico".rol
SET codigo_base = 'CONCILIADOR'
WHERE codigo_base IS NULL
  AND nombre = 'Conciliador'
  AND tipo_perfil = 'CONCILIADOR';


-- ----------------------------------------------------------------------------
-- 3. Verificar que la adopción haya identificado exactamente un rol base
--    para cada identidad requerida.
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    cantidad_administrador INTEGER;
    cantidad_asesor INTEGER;
    cantidad_estudiante INTEGER;
    cantidad_monitor INTEGER;
    cantidad_conciliador INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO cantidad_administrador
    FROM "DB_consultorioJuridico".rol
    WHERE codigo_base = 'ADMINISTRADOR'
      AND tipo_perfil = 'ADMINISTRATIVO';

    SELECT COUNT(*)
    INTO cantidad_asesor
    FROM "DB_consultorioJuridico".rol
    WHERE codigo_base = 'ASESOR'
      AND tipo_perfil = 'ASESOR';

    SELECT COUNT(*)
    INTO cantidad_estudiante
    FROM "DB_consultorioJuridico".rol
    WHERE codigo_base = 'ESTUDIANTE'
      AND tipo_perfil = 'ESTUDIANTE';

    SELECT COUNT(*)
    INTO cantidad_monitor
    FROM "DB_consultorioJuridico".rol
    WHERE codigo_base = 'MONITOR'
      AND tipo_perfil = 'MONITOR';

    SELECT COUNT(*)
    INTO cantidad_conciliador
    FROM "DB_consultorioJuridico".rol
    WHERE codigo_base = 'CONCILIADOR'
      AND tipo_perfil = 'CONCILIADOR';

    IF cantidad_administrador <> 1
       OR cantidad_asesor <> 1
       OR cantidad_estudiante <> 1
       OR cantidad_monitor <> 1
       OR cantidad_conciliador <> 1 THEN

        RAISE EXCEPTION
            'No fue posible identificar exactamente los cinco roles base requeridos';
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- 4. Restringir los valores permitidos para codigo_base.
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'rol_codigo_base_check'
          AND conrelid = '"DB_consultorioJuridico".rol'::regclass
    ) THEN

        ALTER TABLE "DB_consultorioJuridico".rol
        ADD CONSTRAINT rol_codigo_base_check
        CHECK (
            codigo_base IS NULL
            OR codigo_base IN (
                'ADMINISTRADOR',
                'ASESOR',
                'ESTUDIANTE',
                'MONITOR',
                'CONCILIADOR'
            )
        );
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- 5. Garantizar coherencia entre la identidad del rol base y su tipo de perfil.
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'rol_codigo_base_tipo_perfil_check'
          AND conrelid = '"DB_consultorioJuridico".rol'::regclass
    ) THEN

        ALTER TABLE "DB_consultorioJuridico".rol
        ADD CONSTRAINT rol_codigo_base_tipo_perfil_check
        CHECK (
            codigo_base IS NULL
            OR (
                codigo_base = 'ADMINISTRADOR'
                AND tipo_perfil = 'ADMINISTRATIVO'
            )
            OR (
                codigo_base = 'ASESOR'
                AND tipo_perfil = 'ASESOR'
            )
            OR (
                codigo_base = 'ESTUDIANTE'
                AND tipo_perfil = 'ESTUDIANTE'
            )
            OR (
                codigo_base = 'MONITOR'
                AND tipo_perfil = 'MONITOR'
            )
            OR (
                codigo_base = 'CONCILIADOR'
                AND tipo_perfil = 'CONCILIADOR'
            )
        );
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- 6. Cada identidad de rol base debe ser única.
--
-- PostgreSQL permite múltiples NULL en una restricción UNIQUE, por lo que
-- diferentes roles personalizados pueden conservar codigo_base = NULL.
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_rol_codigo_base'
          AND conrelid = '"DB_consultorioJuridico".rol'::regclass
    ) THEN

        ALTER TABLE "DB_consultorioJuridico".rol
        ADD CONSTRAINT uk_rol_codigo_base
        UNIQUE (codigo_base);
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- 7. Los roles base forman parte de la configuración mínima del sistema
--    y no pueden quedar inactivos.
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'rol_base_activo_check'
          AND conrelid = '"DB_consultorioJuridico".rol'::regclass
    ) THEN

        ALTER TABLE "DB_consultorioJuridico".rol
        ADD CONSTRAINT rol_base_activo_check
        CHECK (
            codigo_base IS NULL
            OR activo = TRUE
        );
    END IF;
END $$;


COMMIT;


-- ============================================================================
-- Verificación posterior
-- ============================================================================

SELECT
    id,
    nombre,
    tipo_perfil,
    codigo_base,
    activo
FROM "DB_consultorioJuridico".rol
WHERE codigo_base IS NOT NULL
ORDER BY id;