BEGIN;

DO $$
BEGIN
    IF TO_REGCLASS('"DB_consultorioJuridico".consulta_reasignacion_historial') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM "DB_consultorioJuridico".consulta_reasignacion_historial
       ) THEN
        RAISE EXCEPTION
            'Recuperación cancelada: el historial contiene datos. Conservar la tabla y aplicar una migración correctiva hacia adelante.';
    END IF;
END;
$$;

DROP TABLE IF EXISTS "DB_consultorioJuridico".consulta_reasignacion_historial;
DROP FUNCTION IF EXISTS "DB_consultorioJuridico".validar_responsables_reasignacion_consulta();
DROP FUNCTION IF EXISTS "DB_consultorioJuridico".prevenir_modificacion_historial_reasignacion();

COMMIT;
