# SCRUM-358 — Propuesta de corrección del manual de despliegue

Estado: borrador documental pendiente de validación técnica en SCRUM-333.
Fecha: 02/11/2026.
Preparación documental: Juan Camilo Muñoz Niño.
Versión inspeccionada: 26e42a6f1a4386d01d0858f632af62cdcdb53b91.
Documento objetivo: doc/mantenimiento/manual-despliegue-recuperacion.md.
Implementación relacionada: SCRUM-332, PR #62.

Esta propuesta documenta la implementación inspeccionada. No acredita
aprobación de la estrategia, ejecución de migraciones, despliegue en producción
ni recuperación completa de PostgreSQL y Storage.

## 1. Comparación y correcciones propuestas

| Sección del manual | Situación actual | Corrección propuesta |
|---|---|---|
| Introducción | Presenta protocolos de respaldo y restauración como ejecutados. | Describir procedimientos documentados y separar las ejecuciones acreditadas mediante evidencia. |
| 1. Base de datos | Afirma que Flyway no está integrado. | Documentar Flyway mediante job separado y validación en runtime. |
| 2. Variables | Presenta la cuenta de aplicación como administrador y omite las variables del job. | Diferenciar credenciales de runtime y migraciones; documentar las variables explícitas de la implementación. |
| 3. Migraciones | Afirma que no existen dependencias Flyway ni el directorio de recursos. | Sustituir por las rutas reales, operaciones permitidas y condiciones de adopción del baseline. |
| 3. SQL históricos | Indica aplicar manualmente el historial de reasignaciones antes del despliegue de forma general. | Limitar esas instrucciones a transiciones legadas revisadas; no repetirlas indiscriminadamente sobre una base creada con el nuevo baseline. |
| 4. Recuperación | Afirma ensayo exitoso y 100 % de coincidencias sin identificar muestra ni evidencia. | Retirar esos resultados hasta vincular ambiente, fecha, muestra, registro y responsable. |
| 5. Rollback | No distingue claramente los scripts históricos del mecanismo Flyway incorporado. | Separar ambos mecanismos; no presentar los scripts históricos como reversión general del baseline. |
| 6. Reconciliación | Resume estados sin detallar sus ramas. | Incorporar el tratamiento observado en el servicio y sus límites. |

## 2. Texto propuesto — Arquitectura de base de datos

La implementación inspeccionada incorpora Flyway para PostgreSQL mediante un
job de migraciones separado del backend. El comando DatabaseMigrationCommand
ejecuta las operaciones admitidas sin iniciar Spring ni el servidor HTTP.

En el arranque de la aplicación, RuntimeMigrationValidation sustituye la
estrategia de migración por una llamada a Flyway validate(). Hibernate mantiene
la configuración ${DB_DDL_AUTO:validate}. Estos controles de código no acreditan
las variables efectivas ni la ejecución del job en un despliegue concreto.

## 3. Texto propuesto — Variables y separación de cuentas

Las credenciales se suministran mediante el entorno seguro y no se incluyen
en documentos, capturas ni registros de evidencia.

| Variable | Uso documentado |
|---|---|
| DB_URL | URL de conexión usada por application.properties para el backend. |
| DB_USERNAME | Usuario de conexión del backend; debe distinguirse de la cuenta de migraciones. |
| DB_PASSWORD | Contraseña de conexión del backend. |
| DB_DDL_AUTO | Configuración Hibernate; valor predeterminado en código: validate. |
| MIGRATION_DB_URL | URL JDBC PostgreSQL del job, sin credenciales incorporadas. |
| MIGRATION_DB_USERNAME | Usuario del job de migraciones. |
| MIGRATION_DB_PASSWORD | Contraseña del job de migraciones. |
| MIGRATION_ENVIRONMENT | Identificador de ambiente; acepta letras, números, guion, punto y guion bajo, entre 1 y 64 caracteres. |
| MIGRATION_RELEASE | Identificador de release con formato hexadecimal de 40 caracteres; registrar el commit correspondiente al artefacto revisado. |
| MIGRATION_BASELINE_ACK | Para baseline explícito: valor 20261015.01. |
| MIGRATION_BACKUP_SHA256 | Para baseline explícito: SHA-256 del respaldo, con 64 caracteres hexadecimales. |

El comando comprueba el formato del hash del respaldo; esa comprobación no
sustituye verificar la existencia, integridad y restaurabilidad del respaldo.

Las variables SPRING_DATASOURCE_* y SPRING_JPA_HIBERNATE_DDL_AUTO del manual
deben reconciliarse con los placeholders anteriores y cualquier sobrescritura
real del despliegue. No se afirma que sean inválidas como configuración de
Spring; se requiere documentar cuál mecanismo se utiliza efectivamente.

## 4. Texto propuesto — Migraciones y baseline

### Ubicaciones

- Migraciones Flyway empaquetadas:
  backend/app/src/main/resources/db/migration/
- Baseline implementado:
  backend/app/src/main/resources/db/migration/V20261015_01__baseline_current_schema.sql
- Manifiesto de migraciones:
  backend/app/src/main/resources/db/migration-support/migrations.sha256
- SQL operativos históricos:
  backend/app/db/migration/ y backend/db/

La ubicación configurada para descubrimiento Flyway es classpath:db/migration.
No copiar el directorio histórico completo a esa ubicación: contiene
verificadores y scripts de recuperación que no son migraciones ordinarias.

### Operaciones implementadas

El artefacto de migraciones admite migrate, validate, baseline e inspect.
La implementación restringe el procedimiento a PostgreSQL 16.

- migrate: aplica las migraciones descubiertas mediante Flyway.
- validate: valida mediante Flyway.
- inspect: no solicita aplicar migraciones y devuelve información estructural.
- baseline: registra explícitamente el punto de partida de un esquema legado,
  previa comprobación del contrato estructural y ausencia de historial.

El baseline usa la versión Flyway 20261015.01. Su archivo conserva el nombre
V20261015_01__baseline_current_schema.sql.

Para una base vacía, el flujo propuesto aplica el baseline SQL mediante migrate.
Para una copia legada equivalente, baseline exige el reconocimiento explícito
y el hash de respaldo, además del contrato estructural comprobado por el
comando. Una copia divergente requiere detener la adopción y preparar una
transición revisada; baseline no crea ni repara ese esquema.

La configuración impide baseline automático durante migrate, migraciones
fuera de orden y clean. El comando verifica los archivos contra el manifiesto
empaquetado e incorpora un bloqueo para evitar jobs concurrentes.

Antes de servir tráfico, el responsable técnico debe verificar la secuencia
efectiva del job y el arranque del backend con el artefacto revisado. Esta
propuesta no acredita que dicha secuencia esté configurada en producción.

### SQL históricos

Las instrucciones manuales del historial de reasignaciones se conservan como
referencia histórica para esquemas de partida que requieran esa transición,
previa revisión técnica. No constituyen un paso universal posterior a la
creación del esquema mediante el nuevo baseline.

## 5. Texto propuesto — Respaldo y recuperación

Los comandos de respaldo y restauración del manual constituyen procedimientos
documentados. Para presentarlos como ejecutados deben vincularse fecha,
ambiente, operador, alcance, muestra, manifiestos y resultados reales.

Retirar la afirmación de recuperación completa y el resultado de 100 % de
coincidencias mientras no se identifique su evidencia. La recuperación de una
base sintética o de un objeto aislado no acredita recuperación conjunta de
PostgreSQL y Storage en producción.

Los scripts históricos de rollback y recovery no equivalen a una reversión
general del baseline Flyway. La recuperación documental histórica elimina
metadatos y requiere evaluar pérdida de información. La recuperación del
historial de reasignaciones cancela la operación si existen eventos.

No eliminar registros de flyway_schema_history ni usar clean como mecanismo
de reversión. El responsable técnico debe vincular la restauración o
compensación ensayada correspondiente a la versión y al esquema de partida.

## 6. Texto propuesto — Reconciliación de almacenamiento

FileAssetReconciliationService tiene cron 0 0 * * * * y selecciona registros
cuyo updatedAt sea estrictamente anterior a la hora actual menos una hora.

| Estado seleccionado | Flujo observado |
|---|---|
| PENDING, UPLOADING y DELETING | Intenta eliminar el objeto y después llama a markFailedByObjectKey. Si ocurre RuntimeException, registra el error. |
| DELETE_PENDING | Intenta eliminar el objeto y después llama a markDeleted. Si el proveedor lanza FileNotFoundException, también llama a markDeleted. Para otras RuntimeException registra el error. |

Las llamadas describen el flujo inspeccionado; no acreditan ejecución exitosa
sobre objetos concretos. La revisión técnica debe confirmar los efectos de los
métodos delegados y la habilitación efectiva del scheduler.

## 7. Fuentes versionadas

Todas las rutas siguientes corresponden al commit inspeccionado:

- [Implementación de referencia](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/tree/26e42a6f1a4386d01d0858f632af62cdcdb53b91)
- [Manual objetivo](manual-despliegue-recuperacion.md)
- [Estrategia SCRUM-331](../decisiones/estrategia-migraciones.md)
- [Comando de migraciones](../../backend/app/src/main/java/co/edu/ufps/legal_cases/migration/DatabaseMigrationCommand.java)
- [Validación en runtime](../../backend/app/src/main/java/co/edu/ufps/legal_cases/migration/RuntimeMigrationValidation.java)
- [Baseline SQL](../../backend/app/src/main/resources/db/migration/V20261015_01__baseline_current_schema.sql)
- [Configuración](../../backend/app/src/main/resources/application.properties)
- [Reconciliación](../../backend/app/src/main/java/co/edu/ufps/legal_cases/file_storage/service/FileAssetReconciliationService.java)
- [PR #62](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/62)

Los enlaces relativos deben consultarse en la versión indicada para reproducir
la inspección; una rama posterior puede contener modificaciones.

## 8. Observaciones para SCRUM-333

1. La estrategia de SCRUM-331 todavía figura como propuesta pendiente de
   validación y contiene afirmaciones históricas de ausencia de Flyway.
   La implementación posterior no sustituye el registro de aprobación.
   El responsable debe conciliar ese estado preservando su trazabilidad.
2. Validar rutas, variables, baseline, recuperación y efectos de los métodos
   delegados de reconciliación.
3. Vincular la evidencia real de SCRUM-332 y distinguirla de esta inspección.
4. Confirmar por separado la presencia en main y la versión desplegada.
5. Incorporar la propuesta al manual y a la línea base documental después de
   la revisión correspondiente. La publicación oficial en Drive corresponde
   al dueño documental autorizado.

## 9. Alcance de la entrega

Se entrega borrador, comparación, rutas, versión y observaciones para revisión.
No se ejecutaron migraciones, restauraciones ni pruebas nuevas como parte de
esta preparación. La validación técnica corresponde a SCRUM-333.
