DO $$
DECLARE
    cantidad BIGINT;
BEGIN
    IF TO_REGCLASS('"DB_consultorioJuridico".consulta_reasignacion_historial') IS NULL THEN
        RAISE EXCEPTION 'No existe la tabla consulta_reasignacion_historial';
    END IF;

    SELECT COUNT(*) INTO cantidad
    FROM information_schema.columns
    WHERE table_schema = 'DB_consultorioJuridico'
      AND table_name = 'consulta_reasignacion_historial'
      AND (
          (column_name = 'id' AND data_type = 'bigint'
              AND is_nullable = 'NO' AND is_identity = 'YES')
          OR (column_name = 'consulta_id' AND data_type = 'bigint'
              AND is_nullable = 'NO')
          OR (column_name = 'tipo_responsable' AND data_type = 'character varying'
              AND character_maximum_length = 20 AND is_nullable = 'NO')
          OR (column_name = 'responsable_anterior_id' AND data_type = 'bigint'
              AND is_nullable = 'YES')
          OR (column_name = 'responsable_nuevo_id' AND data_type = 'bigint'
              AND is_nullable = 'NO')
          OR (column_name = 'motivo' AND data_type = 'text'
              AND is_nullable = 'NO')
          OR (column_name = 'actor_usuario_id' AND data_type = 'bigint'
              AND is_nullable = 'NO')
          OR (column_name = 'ocurrido_en' AND data_type = 'timestamp with time zone'
              AND is_nullable = 'NO' AND column_default IS NOT NULL)
      );
    IF cantidad <> 8 THEN
        RAISE EXCEPTION
            'Columnas incompatibles con el contrato: encontradas % de 8', cantidad;
    END IF;

    SELECT COUNT(*) INTO cantidad
    FROM information_schema.table_constraints
    WHERE table_schema = 'DB_consultorioJuridico'
      AND table_name = 'consulta_reasignacion_historial'
      AND constraint_name IN (
          'consulta_reasignacion_historial_pkey',
          'fk_reasignacion_historial_consulta',
          'fk_reasignacion_historial_actor',
          'ck_reasignacion_historial_tipo',
          'ck_reasignacion_historial_anterior_positivo',
          'ck_reasignacion_historial_nuevo_positivo',
          'ck_reasignacion_historial_responsables_distintos',
          'ck_reasignacion_historial_motivo'
      );
    IF cantidad <> 8 THEN
        RAISE EXCEPTION 'Faltan restricciones esperadas: encontradas % de 8', cantidad;
    END IF;

    SELECT COUNT(*) INTO cantidad
    FROM pg_indexes
    WHERE schemaname = 'DB_consultorioJuridico'
      AND tablename = 'consulta_reasignacion_historial'
      AND indexname IN (
          'idx_reasignacion_historial_consulta_fecha',
          'idx_reasignacion_historial_actor_fecha'
      );
    IF cantidad <> 2 THEN
        RAISE EXCEPTION 'Faltan índices esperados: encontrados % de 2', cantidad;
    END IF;

    SELECT COUNT(*) INTO cantidad
    FROM pg_trigger trigger_def
    JOIN pg_class tabla ON tabla.oid = trigger_def.tgrelid
    JOIN pg_namespace esquema ON esquema.oid = tabla.relnamespace
    WHERE esquema.nspname = 'DB_consultorioJuridico'
      AND tabla.relname = 'consulta_reasignacion_historial'
      AND NOT trigger_def.tgisinternal
      AND trigger_def.tgname IN (
          'trg_validar_responsables_reasignacion',
          'trg_historial_reasignacion_inmutable'
      );
    IF cantidad <> 2 THEN
        RAISE EXCEPTION 'Faltan triggers esperados: encontrados % de 2', cantidad;
    END IF;

    SELECT COUNT(*) INTO cantidad
    FROM "DB_consultorioJuridico".consulta_reasignacion_historial
    WHERE tipo_responsable NOT IN ('ESTUDIANTE', 'ASESOR', 'MONITOR')
       OR responsable_nuevo_id <= 0
       OR (responsable_anterior_id IS NOT NULL AND responsable_anterior_id <= 0)
       OR responsable_anterior_id = responsable_nuevo_id
       OR LENGTH(BTRIM(motivo)) = 0;
    IF cantidad <> 0 THEN
        RAISE EXCEPTION 'Se encontraron % registros que incumplen las invariantes', cantidad;
    END IF;
END;
$$;
