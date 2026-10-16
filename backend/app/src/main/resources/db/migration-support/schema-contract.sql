WITH relations AS (
    SELECT c.* FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'DB_consultorioJuridico' AND c.relname <> 'flyway_schema_history'
      AND c.relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
), facts(kind, identity, definition) AS (
    SELECT 'relation', relname::text,
           jsonb_build_array(relkind, relpersistence, relrowsecurity, relforcerowsecurity,
               reloptions, pg_get_expr(relpartbound, oid))::text FROM relations
    UNION ALL
    SELECT 'column', c.relname || '.' || a.attname,
           jsonb_build_array(format_type(a.atttypid, a.atttypmod), a.attnotnull,
               a.attidentity, a.attgenerated, pg_get_expr(d.adbin, d.adrelid),
               a.attcollation::regcollation::text)::text
    FROM relations c JOIN pg_attribute a ON a.attrelid = c.oid
    LEFT JOIN pg_attrdef d ON d.adrelid = c.oid AND d.adnum = a.attnum
    WHERE a.attnum > 0 AND NOT a.attisdropped
    UNION ALL
    SELECT 'constraint', c.relname || '.' || k.conname,
           jsonb_build_array(pg_get_constraintdef(k.oid, true), k.convalidated,
               k.condeferrable, k.condeferred)::text
    FROM relations c JOIN pg_constraint k ON k.conrelid = c.oid
    UNION ALL
    SELECT 'index', c.relname || '.' || i.relname,
           jsonb_build_array(pg_get_indexdef(x.indexrelid), x.indisvalid, x.indisready)::text
    FROM relations c JOIN pg_index x ON x.indrelid = c.oid
    JOIN pg_class i ON i.oid = x.indexrelid
    UNION ALL
    SELECT 'trigger', c.relname || '.' || t.tgname,
           jsonb_build_array(pg_get_triggerdef(t.oid, true), t.tgenabled)::text
    FROM relations c JOIN pg_trigger t ON t.tgrelid = c.oid WHERE NOT t.tgisinternal
    UNION ALL
    SELECT 'function', p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')',
           pg_get_functiondef(p.oid)
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'DB_consultorioJuridico' AND p.prokind IN ('f','p')
    UNION ALL
    SELECT 'sequence', c.relname::text,
           jsonb_build_array(format_type(s.seqtypid, NULL), s.seqstart, s.seqincrement,
               s.seqmax, s.seqmin, s.seqcache, s.seqcycle)::text
    FROM relations c JOIN pg_sequence s ON s.seqrelid = c.oid
    UNION ALL
    SELECT 'sequence-owner', c.relname::text,
           jsonb_build_array(r.relname, a.attname, d.deptype)::text
    FROM relations c JOIN pg_depend d ON d.objid = c.oid AND d.classid = 'pg_class'::regclass
    JOIN pg_class r ON r.oid = d.refobjid
    JOIN pg_attribute a ON a.attrelid = r.oid AND a.attnum = d.refobjsubid
    WHERE c.relkind = 'S' AND d.refclassid = 'pg_class'::regclass AND d.deptype IN ('a','i')
    UNION ALL
    SELECT 'view', c.relname::text, pg_get_viewdef(c.oid, true)
    FROM relations c WHERE c.relkind IN ('v','m')
    UNION ALL
    SELECT 'policy', c.relname || '.' || p.polname,
           jsonb_build_array(p.polcmd, p.polpermissive,
               (SELECT array_agg(CASE WHEN role_id = 0 THEN 'public' ELSE pg_get_userbyid(role_id) END ORDER BY role_id)
                FROM unnest(p.polroles) role_id),
               pg_get_expr(p.polqual, p.polrelid), pg_get_expr(p.polwithcheck, p.polrelid))::text
    FROM relations c JOIN pg_policy p ON p.polrelid = c.oid
    UNION ALL
    SELECT 'type', t.typname::text,
           jsonb_build_array(t.typtype, format_type(t.typbasetype, t.typtypmod),
               t.typnotnull, t.typdefault,
               (SELECT array_agg(e.enumlabel ORDER BY e.enumsortorder) FROM pg_enum e WHERE e.enumtypid = t.oid),
               (SELECT array_agg(pg_get_constraintdef(k.oid, true) ORDER BY k.conname) FROM pg_constraint k WHERE k.contypid = t.oid))::text
    FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE n.nspname = 'DB_consultorioJuridico' AND t.typtype IN ('e','d')
)
SELECT jsonb_build_array(kind, identity, definition)::text FROM facts
ORDER BY kind COLLATE "C", identity COLLATE "C", definition COLLATE "C"
