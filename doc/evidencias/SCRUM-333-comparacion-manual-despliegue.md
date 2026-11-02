# SCRUM-333 — Comparación y reconciliación del manual de despliegue

Fecha de revisión: 02/11/2026 (UTC).
Base revisada: `develop` en `26e42a6f1a4386d01d0858f632af62cdcdb53b91`.
Resultado: **INCONCLUSO / BLOQUEADO para cierre**.

## Alcance ejecutado

Se comparó el manual vigente con la configuración y el código de migraciones y
reconciliación presentes en `develop`. Esta entrega modifica únicamente
documentación versionada. No aplica SQL, no usa credenciales, no modifica
migraciones ya publicadas y no ejecuta restauraciones.

SCRUM-332 está integrado mediante el PR #62 y cambió la situación descrita en la
línea base original: Flyway sí está incorporado, pero la migración se ejecuta en
un artefacto separado y el backend web solo valida. No se encontró rama ni PR
que identifique una entrega de SCRUM-358 en la búsqueda del repositorio realizada
el 02/11/2026. Como SCRUM-358 es predecesora declarada, esta propuesta no reclama
la aceptación ni el cierre de SCRUM-333.

## Comparación

| Tema | Documento anterior | Propuesta reconciliada |
|---|---|---|
| Ejecución de migraciones | Indicaba que Flyway no estaba integrado y describía aplicación manual. | Documenta `legal-cases-backend-migrations.jar` como job previo y separado; el runtime ejecuta validación, no DDL. |
| Ruta automática | Trataba `backend/app/db/migration/` como ubicación operativa principal. | Limita el descubrimiento automático a `backend/app/src/main/resources/db/migration/`; clasifica `backend/app/db/migration/` y `backend/db/` como históricos, verificadores o recuperaciones explícitas. |
| Archivos inexistentes | Conservaba contexto de V23/V25/V26/U26. | Declara que no existen y evita usarlos como instrucción. |
| Credenciales | Mezclaba nombres genéricos de Spring y una cuenta administradora. | Separa `MIGRATION_*` del job y `DB_*` del runtime DML sin DDL. |
| Base vacía o legada | No reflejaba el contrato implementado por SCRUM-332. | Diferencia `migrate` para base vacía, `inspect` y baseline explícito para una base legada equivalente, y detención para una base divergente. |
| Rollback | Podía interpretarse como ejecución de un script descendente genérico. | Prioriza restauración verificada o corrección hacia adelante; prohíbe `clean`, editar SQL aplicado y borrar historial. Enlaza únicamente scripts reales y limita su alcance. |
| Respaldo/restauración | Presentaba el procedimiento como ejecutado y afirmaba coincidencia total sin ambiente ni muestra. | Lo identifica como procedimiento recomendado y exige ambiente, muestra, hashes y evidencia antes de declarar un resultado. |
| Reconciliación | Debía aclarar el criterio efectivo. | Registra `updatedAt`, corte estricto de una hora, cron horario y estados `PENDING`, `UPLOADING`, `DELETE_PENDING` y `DELETING`. |

## Evidencia estática contrastada

- `backend/app/src/main/resources/application.properties`: Flyway habilitado,
  ubicación `classpath:db/migration`, `baseline-on-migrate=false`,
  `clean-disabled=true`, `out-of-order=false` y Hibernate
  `${DB_DDL_AUTO:validate}`.
- `DatabaseMigrationCommand`: operaciones permitidas `migrate`, `validate`,
  `baseline` e `inspect`; PostgreSQL 16; bloqueo de concurrencia; credenciales
  de migración separadas; reconocimiento explícito para baseline.
- `RuntimeMigrationValidation`: el backend sustituye la estrategia de migración
  por `flyway.validate()`.
- `backend/app/Dockerfile`: empaqueta `app.jar` y `migrations.jar`, pero el
  entrypoint inicia únicamente el backend.
- `backend/app/src/main/resources/db/migration/`: contiene
  `V20261015_01__baseline_current_schema.sql`.
- `backend/app/db/migration/`: contiene scripts históricos reales, verificadores
  y recuperaciones citados por el manual.
- `FileAssetReconciliationService`: `STALE_HOURS = 1`, cron
  `0 0 * * * *` y consulta por `updatedAt` para los cuatro estados documentados.

## Validación realizada

- **PASS — inspección estática:** rutas citadas existen en `develop` y las rutas
  V23/V25/V26/U26 no se usan como instrucciones.
- **PASS — revisión de texto:** no quedan enlaces locales absolutos, afirmaciones
  de Flyway ausente ni resultados totales sin evidencia en los archivos modificados.
- **PASS — comprobación de parche:** sin espacios finales ni errores de diff.
- **NO EJECUTADO:** migraciones, consultas SQL, backup, restore, rollback,
  despliegue y pruebas contra una base de datos.
- **PENDIENTE:** evidencia entregada por SCRUM-358 y revisión independiente de
  José Manuel Pérez Rodríguez o Nelson Enrrique Conde Tarazona.

## Publicación y límites

Esta es la versión propuesta para revisión en GitHub. La publicación o reemplazo
del documento oficial en Drive corresponde al dueño documental autorizado.
No se reutiliza la CI de SCRUM-332 como prueba nueva de esta corrección documental.
