-- Explicit operator action, never discovered as a Flyway migration.
-- psql --set ON_ERROR_STOP=1 --set runtime_role=app_runtime --file <this-file>
-- Connect as owner. The runtime login must already exist; credentials are not stored here.
BEGIN;
SELECT set_config('migration.runtime_role', :'runtime_role', true);
DO $$
DECLARE runtime_oid oid := current_setting('migration.runtime_role')::regrole;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles
               WHERE (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
                 AND pg_has_role(runtime_oid, oid, 'MEMBER'))
       OR EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'DB_consultorioJuridico'
                  AND pg_has_role(runtime_oid, nspowner, 'MEMBER')) THEN
        RAISE EXCEPTION 'Runtime must not be privileged, schema owner, or a member of its owner role';
    END IF;
END $$;
REVOKE ALL ON SCHEMA "DB_consultorioJuridico" FROM :"runtime_role";
GRANT USAGE ON SCHEMA "DB_consultorioJuridico" TO :"runtime_role";
REVOKE ALL ON ALL TABLES IN SCHEMA "DB_consultorioJuridico" FROM :"runtime_role";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA "DB_consultorioJuridico" TO :"runtime_role";
REVOKE ALL ON "DB_consultorioJuridico".flyway_schema_history FROM :"runtime_role";
GRANT SELECT ON "DB_consultorioJuridico".flyway_schema_history TO :"runtime_role";
REVOKE ALL ON ALL SEQUENCES IN SCHEMA "DB_consultorioJuridico" FROM :"runtime_role";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA "DB_consultorioJuridico" TO :"runtime_role";
DO $$
DECLARE runtime_oid oid := current_setting('migration.runtime_role')::regrole;
BEGIN
    IF has_schema_privilege(runtime_oid, 'DB_consultorioJuridico', 'CREATE')
       OR has_database_privilege(runtime_oid, current_database(), 'CREATE')
       OR EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                  WHERE n.nspname='DB_consultorioJuridico'
                    AND CASE WHEN c.relkind IN ('r','p','v','m','f')
                             THEN has_table_privilege(runtime_oid, c.oid, 'TRUNCATE,TRIGGER') ELSE false END)
       OR EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                  WHERE n.nspname='DB_consultorioJuridico'
                    AND CASE WHEN c.relkind='S'
                             THEN has_sequence_privilege(runtime_oid, c.oid, 'UPDATE') ELSE false END) THEN
        RAISE EXCEPTION 'Runtime retains unsafe inherited privileges; review memberships, ownership and PUBLIC grants';
    END IF;
END $$;
COMMIT;
