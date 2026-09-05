-- Verificación para V20260905_01__version_document_model.sql
-- Ejecutar para verificar consistencia tras aplicar la migración.

DO $verify$
DECLARE
    v_total_records integer;
    v_null_doc_logico integer;
    v_null_version integer;
    v_invalid_version integer;
    v_duplicate_vigente integer;
    v_duplicate_version integer;
    v_orphan_referencia integer;
BEGIN
    SELECT count(*) INTO v_total_records FROM "DB_consultorioJuridico".file_asset;

    SELECT count(*) INTO v_null_doc_logico
    FROM "DB_consultorioJuridico".file_asset
    WHERE documento_logico IS NULL;

    IF v_null_doc_logico > 0 THEN
        RAISE EXCEPTION 'Falla en verificación: Existen % registros con documento_logico nulo', v_null_doc_logico;
    END IF;

    SELECT count(*) INTO v_null_version
    FROM "DB_consultorioJuridico".file_asset
    WHERE version IS NULL;

    IF v_null_version > 0 THEN
        RAISE EXCEPTION 'Falla en verificación: Existen % registros con version nula', v_null_version;
    END IF;

    SELECT count(*) INTO v_invalid_version
    FROM "DB_consultorioJuridico".file_asset
    WHERE version < 1;

    IF v_invalid_version > 0 THEN
        RAISE EXCEPTION 'Falla en verificación: Existen % registros con version < 1', v_invalid_version;
    END IF;

    -- Verificar que no existan múltiples versiones VIGENTE para un mismo documento lógico
    SELECT count(*) INTO v_duplicate_vigente
    FROM (
        SELECT documento_logico
        FROM "DB_consultorioJuridico".file_asset
        WHERE status = 'VIGENTE'
        GROUP BY documento_logico
        HAVING count(*) > 1
    ) sub;

    IF v_duplicate_vigente > 0 THEN
        RAISE EXCEPTION 'Falla en verificación: Existen % documentos lógicos con más de una versión VIGENTE', v_duplicate_vigente;
    END IF;

    -- Verificar que no existan versiones duplicadas bajo el mismo documento lógico
    SELECT count(*) INTO v_duplicate_version
    FROM (
        SELECT documento_logico, version
        FROM "DB_consultorioJuridico".file_asset
        GROUP BY documento_logico, version
        HAVING count(*) > 1
    ) sub;

    IF v_duplicate_version > 0 THEN
        RAISE EXCEPTION 'Falla en verificación: Existen % duplicados de versión para el mismo documento lógico', v_duplicate_version;
    END IF;

    -- Verificar integridad de referencia_anterior_id
    SELECT count(*) INTO v_orphan_referencia
    FROM "DB_consultorioJuridico".file_asset a
    WHERE a.referencia_anterior_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM "DB_consultorioJuridico".file_asset b WHERE b.id = a.referencia_anterior_id
      );

    IF v_orphan_referencia > 0 THEN
        RAISE EXCEPTION 'Falla en verificación: Existen % registros con referencia_anterior_id huérfana', v_orphan_referencia;
    END IF;

    RAISE NOTICE 'Verificación exitosa: % registros validados correctamente.', v_total_records;
END
$verify$;
