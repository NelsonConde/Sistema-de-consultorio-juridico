# Backend - Auditoría probatoria

## Propósito

El módulo conserva una bitácora estructurada, consultable e inmutable de operaciones críticas. La auditoría es independiente de los logs operativos: no almacena trazas, argumentos completos, contraseñas, tokens, cookies, documentos, correos ni representaciones `toString()` de objetos JVM.

## Contrato de evento

Cada fila registra:

- actor (`actorUsername`), acción y entidad/identificador explícitos;
- resultado `SUCCESS`, `FAILURE` o `DENIED`;
- instante UTC, origen `HTTP`/`SYSTEM`, IP, agente y correlación;
- código/motivo funcional cuando está permitido;
- snapshots escalares anterior/nuevo declarados mediante `trackedFields`;
- metadatos escalares incluidos en la lista permitida de la anotación.

`AuditExpressionEvaluator` acepta únicamente escalares y rechaza claves sensibles. `AuditStateSnapshotService` sólo consulta propiedades declaradas; no descubre identificadores ni serializa objetos por heurística.

## Ejecución y transacciones

El modelo elegido es síncrono, sin `@Async`:

- un éxito de escritura participa en la transacción del caso de uso;
- si esa transacción hace rollback, el éxito desaparece y se crea evidencia `FAILURE` en una transacción independiente;
- una excepción o denegación se persiste con `REQUIRES_NEW`, por lo que sobrevive al rollback;
- lecturas y descargas, cuyas transacciones son read-only, se registran en una transacción independiente.

El orden transaccional está declarado en `App` y `AuditAspect`. Un fallo al escribir la auditoría de una operación crítica impide presentar esa operación como correctamente auditada.

## Cobertura

`@Auditable` cubre comandos de consultas, personas, perfiles, procesos, seguimientos y conciliaciones. También se auditan:

- lectura individual/listado de personas y consultas jurídicas;
- listado de archivos y preparación de una descarga firmada;
- excepciones de autorización dentro de casos de uso;
- rechazos 401/403 generados antes de entrar a un caso de uso.

## Consulta y autorización

`GET /api/audit` requiere exclusivamente `Ver auditoría`. La página debe estar entre 0 y el tamaño entre 1 y 100; el ordenamiento usa una lista blanca. Se admiten filtros por actor, acción, entidad, resultado, correlación e intervalo UTC.

El permiso se crea a partir de `PermisoNombre` y debe asignarse expresamente al rol autorizado. `Acceder administración` no concede acceso implícito a la bitácora.

## Inmutabilidad y despliegue

La migración `backend/app/db/migration/V20260830_01__restructure_audit_log.sql`:

- migra actor e instante heredados;
- elimina `details` y las columnas antiguas;
- crea restricciones e índices;
- instala un trigger que rechaza `UPDATE` y `DELETE` en el esquema institucional.

El procedimiento de aplicación y recuperación se documenta en `backend/app/db/migration/README.md`.
