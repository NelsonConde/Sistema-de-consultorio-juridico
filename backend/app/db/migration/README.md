# Migraciones operativas

Los scripts de este directorio se aplican en orden de versión sobre el esquema objetivo y se conservan como evidencia reproducible. Antes de ejecutar una migración se debe crear un respaldo verificable del esquema y de sus datos.

## V20260830_01 - Auditoría probatoria

1. Respaldar `"DB_consultorioJuridico".audit_logs`.
2. Ejecutar `V20260830_01__restructure_audit_log.sql` con el propietario del esquema.
3. Verificar que un `INSERT` funciona y que `UPDATE` y `DELETE` son rechazados.
4. Desplegar el backend que usa el contrato estructurado.

La migración elimina `details` porque podía contener argumentos sensibles y representaciones de objetos JVM. Ante una incidencia, el plan de recuperación es restaurar el respaldo completo de la tabla y desplegar la versión anterior del backend; no se intenta reconstruir ese contenido inseguro.
