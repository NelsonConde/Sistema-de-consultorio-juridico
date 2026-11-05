# Migraciones operativas

Los scripts de este directorio se aplican en orden de versión sobre el esquema objetivo y se conservan como evidencia reproducible. Antes de ejecutar una migración se debe crear un respaldo verificable del esquema y de sus datos.

## V20260830_01 - Auditoría probatoria

1. Respaldar `"DB_consultorioJuridico".audit_logs`.
2. Ejecutar `V20260830_01__restructure_audit_log.sql` con el propietario del esquema.
3. Verificar que un `INSERT` funciona y que `UPDATE` y `DELETE` son rechazados.
4. Desplegar el backend que usa el contrato estructurado.

## V20260905_01 - Versionado documental y metadatos (file_asset)

1. Respaldar `"DB_consultorioJuridico".file_asset`.
2. Ejecutar `V20260905_01__version_document_model.sql` con el propietario del esquema.
3. Ejecutar `V20260905_01__version_document_model_verify.sql` para validar que todas las filas legadas recibieron `documento_logico`, `version = 1`, `origen = 'MIGRADO'`, `tipo_documental` clasificado y `status = 'VIGENTE'` sin duplicados.
4. Desplegar el backend con el nuevo modelo documental versionado.

### Plan de rollback:
En caso de contingencia o fallo de despliegue:
1. Ejecutar `V20260905_01__version_document_model_rollback.sql` o restaurar el respaldo de la tabla `"DB_consultorioJuridico".file_asset`.
2. Desplegar la versión previa del backend.

## V20261011_01 - Historial de reasignaciones de consultas

1. Crear un respaldo verificable del esquema `"DB_consultorioJuridico"`.
2. Ejecutar `V20261011_01__create_consulta_reasignacion_historial.sql` con el propietario del esquema.
3. Ejecutar `V20261011_01__create_consulta_reasignacion_historial_verify.sql`.
4. Desplegar el backend solo después de que la verificación finalice sin excepciones.

### Recuperación segura

`V20261011_01__create_consulta_reasignacion_historial_recovery.sql` solo elimina los objetos cuando la tabla está vacía. Si ya existen eventos, el script cancela la operación: se debe conservar el historial y preparar una migración correctiva hacia adelante. No ejecutar la recuperación sin respaldo y revisión previa.

