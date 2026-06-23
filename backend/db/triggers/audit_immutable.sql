-- Este script debe ser ejecutado manualmente en la base de datos PostgreSQL (Supabase)
-- para asegurar la inmutabilidad de los registros de auditoría a nivel de base de datos.

CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables y no pueden ser modificados ni eliminados por la aplicación o los administradores.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_prevent_audit_mod
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();
