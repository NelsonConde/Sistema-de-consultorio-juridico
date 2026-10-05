# SCRUM-285 — Restauración y suite adversarial documental

## Identificación

- **Ticket:** SCRUM-285
- **Historia relacionada:** PB-29 / SCRUM-260
- **Rama:** `test/scrum-285-document-recovery-adversarial`
- **Responsable:** Nelson
- **Fecha de ejecución:** 03/10/2026, zona `America/Bogota`
- **Ambiente:** Windows 11 local; Docker Desktop / WSL2; LocalStack Community 4.4.0 para S3 compatible; recuperación sintética en memoria y backup operacional en archivo temporal del host externo al repositorio; Mockito para fronteras de autorización
- **Java:** Oracle JDK `21.0.12.1`
- **Maven:** Maven Wrapper `3.9.14`

## Alcance

Se inspeccionó la implementación real de `FileResourceService`, `FileAssetService`,
`FileValidationService`, `FileResourceAuthorizationService`,
`FileAssetReconciliationService`, `StorageProvider`, `SupabaseStorageProvider`,
`FileAsset`, `FileAssetRepository` y `FileResourceController`, además de las pruebas
documentales existentes.

La ejecución cubre autorización por recurso, nombres hostiles, colisiones de nombre,
límites de tamaño, validación de checksum, tamaño almacenado, firma PDF, propagación
del TTL, conciliación de estados y una restauración controlada con datos sintéticos.
No se contactó producción, Supabase real ni una base de datos real. QA-DOC-13 usó
URLs firmadas reales emitidas exclusivamente contra LocalStack aislado. No se usaron
datos del consultorio, credenciales reales, tokens ni cookies.

## Casos ejecutados

| ID | Caso | Esperado | Obtenido | Estado | Evidencia / observación |
|---|---|---|---|---|---|
| QA-DOC-01 | Usuario ajeno inicia carga | Rechazo antes de persistir o firmar | `AccessDeniedException`; sin validación, persistencia ni llamada a Storage | PASS | `usuarioAjenoNoPuedeIniciarCarga` |
| QA-DOC-02 | Usuario ajeno lista archivos | Rechazo sin devolver metadata | `AccessDeniedException`; `listReady` y Storage no fueron invocados | PASS | `usuarioAjenoNoPuedeListar` |
| QA-DOC-03 | Usuario ajeno descarga | Rechazo antes de generar URL firmada | `AccessDeniedException`; `createDownloadUrl` no fue invocado | PASS | `usuarioAjenoNoPuedeDescargar` |
| QA-DOC-04 | Usuario ajeno elimina | Rechazo antes de alterar Storage o metadata | `AccessDeniedException`; no se marcó estado ni se llamó a Storage | PASS | `usuarioAjenoNoPuedeEliminar` |
| QA-DOC-05 | Traversal con cuatro variantes | Rechazo y cero persistencia | Las cuatro variantes fueron rechazadas; repositorio y Storage sin interacciones | PASS | `traversalEsRechazadoSinPersistir` (4 invocaciones) |
| QA-DOC-06 | Nombre duplicado | No sobrescribir; claves distintas; conservar nombre | Dos cargas conservaron `evidencia.txt` y generaron `objectKey` distintos bajo el prefijo del recurso | PASS | `nombreDuplicadoNoSobrescribeObjeto` |
| QA-DOC-07 | Archivo mayor de 10 MiB | Rechazo | Rechazado antes de persistir o firmar | PASS | `archivoSuperiorAlLimiteEsRechazado` |
| QA-DOC-08 | Tamaño declarado diferente al almacenado | Fallo; no READY; marcar fallida | `complete` lanzó excepción, llamó `markUploadFailed` y no llamó `markReady` | PASS | `tamanoAlmacenadoDistintoFallaCarga` |
| QA-DOC-09 | Checksum con formato inválido | Rechazo | SHA-256 mal formado rechazado antes de persistir o firmar | PASS | `checksumConFormatoInvalidoEsRechazado` |
| QA-DOC-10 | SHA-256 válido pero distinto a los bytes | Rechazo por integridad | `complete()` aceptó la carga y no lanzó excepción | FAIL | SCRUM-313; reproducción explícita en `DocumentChecksumIntegrityIT.checksumValidoPeroDistintoDebeSerRechazado` |
| QA-DOC-11 | `.pdf` y `application/pdf` sin firma `%PDF-` | Rechazo y carga fallida | Contenido rechazado, objeto eliminado y carga marcada fallida | PASS | `pdfSinFirmaEsRechazadoDuranteComplete` |
| QA-DOC-12 | TTL de descarga | Entregar al proveedor el `Duration` configurado | Se entregó exactamente `PT5M` | PASS | `descargaEntregaTtlConfiguradoAlProveedor` |
| QA-DOC-13 | URL vencida | Primera URL HTTP 200; la misma URL vencida distinta de 200; URL nueva HTTP 200 con bytes idénticos | LocalStack devolvió HTTP 200 antes y después de vencer; una URL nueva devolvió HTTP 200 y 38 bytes idénticos | BLOCKED | INCONCLUSIVE: LocalStack 4.4.0 no reproduce de forma confiable la expiración; no se registra como defecto del producto |
| QA-DOC-14 | PENDING/UPLOADING viejo | Eliminar objeto y marcar carga fallida | Ambos estados se eliminaron y se llamó `markFailedByObjectKey` | PASS | `reconciliaCargasIncompletasViejas` |
| QA-DOC-15 | DELETE_PENDING | Éxito e inexistente quedan DELETED; error temporal conserva pendiente | Éxito e inexistente llamaron `markDeleted`; error temporal no lo hizo | PASS | `reconciliaDeletePendingSegunResultadoDelProveedor` |
| QA-DOC-16 | Persistencia tras reemplazo/reinicio de backend | Bytes sobreviven y no dependen del filesystem efímero | Un contenedor A escribió y fue destruido; un contenedor B nuevo descargó desde LocalStack sin subir nuevamente y verificó bytes, SHA-256 y metadata | PASS | `DocumentContainerPersistenceIT`; recuperación operacional en 11802 ms; cleanup confirmó ausencia |
| QA-DOC-17 | Backup y restauración | Copia material independiente; eliminar activo, restaurar desde backup y verificar bytes, SHA-256 y metadata; medir y limpiar | Prueba previa sintética más restauración operacional real de 1 objeto S3 desde archivo temporal externo; pérdida confirmada; 91 bytes, SHA-256, tipo, tamaño y clave iguales; cleanup PASS | PASS | `DocumentBackupRestoreIT`; recuperación de 61,965 ms; `run-document-backup-restore.ps1` |

Resumen de casos: **15 PASS, 1 FAIL, 1 BLOCKED**.

## QA-DOC-13 — Expiración real de URL firmada

- **Proveedor:** LocalStack Community 4.4.0
- **Servicio:** S3 compatible
- **Ambiente:** Docker Desktop / WSL2 / local
- **Bucket:** `legal-documents`
- **Contenido:** sintético, 38 bytes
- **TTL probado:** 2 segundos
- **Espera aplicada:** 3 segundos
- **Primera solicitud:** HTTP 200, 38 bytes
- **Misma URL tras expiración:** HTTP 200, 38 bytes
- **Nueva URL:** HTTP 200, 38 bytes
- **Integridad:** los bytes obtenidos mediante la URL nueva coincidieron exactamente
- **Limpieza:** objeto sintético eliminado; `head()` posterior confirmó inexistencia
- **Resultado técnico:** `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0` — `BUILD FAILURE`
- **Estado QA:** BLOCKED / INCONCLUSIVE

No se registró la URL firmada, su query string, `X-Amz-Signature`, credenciales,
tokens ni cookies. LocalStack 4.4.0 no aplicó la expiración de URL firmada en esta
ejecución. El resultado del proveedor emulado es no concluyente respecto del
comportamiento de Supabase/S3 real y no se registra como defecto del producto.

## QA-DOC-16 — Reemplazo real de contenedor backend

- **Proveedor:** LocalStack Community 4.4.0 / S3 compatible
- **Ambiente:** Docker Desktop + WSL2 local; red `scrum285-net`
- **Tipo:** prueba operacional de reemplazo de contenedor
- **Datos:** 100 % sintéticos
- **Sesión:** `305ba3c1ed2b498fbc43ec30b60b24b7`
- **Contenedor A:** nombre `scrum285-doc16-write-305ba3c1ed2b`; ID `61ff8650ee25`
- **Resultado WRITE:** PASS; objeto almacenado, releído y validado
- **Contenedor A destruido:** sí; `docker run --rm` y ausencia confirmada antes de B
- **Contenedor B:** nombre `scrum285-doc16-verify-305ba3c1ed2b`; ID `fc4c0fd117a3`
- **Nuevo contenedor confirmado:** sí; nombre e ID diferentes de A
- **Carga desde B:** no; la fase VERIFY no invoca `store()` (`storeCalled=false`)
- **Resultado VERIFY:** PASS
- **Bytes:** coinciden; 80 bytes
- **SHA-256 antes/después:** `076ff3d34a86142d8451153ca8084e18b6c1286d2ef0f7749b11b1d9df3b09b9`
- **Metadata:** coincide; `contentLength=80`, `contentType=text/plain`
- **Tiempo de recuperación:** 11802 ms, medido desde el inicio de `docker run` de B hasta la verificación satisfactoria
- **Contenedor CLEANUP:** ID `f8b83511a54d`
- **Cleanup:** objeto eliminado y ausencia confirmada mediante `head()`
- **Estado:** PASS

Los contenedores se ejecutaron desde la misma imagen Java 21 inmutable, sin Spring
Context, sin montar el workspace y sin volúmenes o archivos compartidos. El contexto
temporal de construcción excluyó `src/main/resources`; la única continuidad documental
entre A y B fue el objeto almacenado en LocalStack. La información compartida de control
se limitó al identificador de sesión y la configuración S3 sintética.

## Restauración

### QA-DOC-17 — Restauración operacional real desde backup material

- **Responsable:** Nelson
- **Fecha local:** 03/10/2026, `America/Bogota`
- **Proveedor:** LocalStack Community 4.4.0 / S3 compatible
- **Ambiente:** Docker Desktop + WSL2 local; test ejecutado desde el host Windows con Java 21 y Maven Wrapper 3.9.14
- **Datos:** 100 % sintéticos
- **Sesión:** `c9d88b92-27e0-43e4-bda2-51ba6974aa4b`
- **Bucket:** `legal-documents`
- **ObjectKey original/restaurado:** `scrum285/qa-doc-17/c9d88b92-27e0-43e4-bda2-51ba6974aa4b/evidencia.txt`
- **Cantidad de objetos:** 1
- **Backup:** descarga real desde `StorageProvider.load()` a un archivo temporal del host externo al repositorio; manifiesto independiente con `objectKey`, `contentType`, `size` y `sha256`, sin secretos
- **Backup material confirmado:** sí; archivo existente y SHA-256 recalculado igual al original
- **Objeto activo eliminado antes de restaurar:** sí; `delete()` seguido de `head()` con `FileNotFoundException`; backup aún presente
- **Fuente exclusiva de restauración:** stream del archivo backup, con metadata releída del manifiesto; los arrays originales solo se usaron para verificación
- **Bytes originales/restaurados:** 91; comparación byte por byte satisfactoria
- **Metadata original/restaurada:** `contentLength=91`, `contentType=text/plain`, mismo `objectKey`
- **SHA-256 original:** `7d481b9e0a2da9f52486ab81133513b036fd1161a6de7cf8097831b8763f0f1f`
- **SHA-256 backup:** `7d481b9e0a2da9f52486ab81133513b036fd1161a6de7cf8097831b8763f0f1f`
- **SHA-256 restaurado:** `7d481b9e0a2da9f52486ab81133513b036fd1161a6de7cf8097831b8763f0f1f`
- **Inicio de restauración (UTC):** `2026-10-04T01:18:26.627287400Z` (03/10/2026 20:18:26, Bogotá)
- **Fin de restauración (UTC):** `2026-10-04T01:18:26.692831600Z` (03/10/2026 20:18:26, Bogotá)
- **Tiempo real de recuperación:** 61,965 ms, medido con `System.nanoTime()` inmediatamente antes de leer/subir el backup y hasta completar `head()`, `load()` y todas las comparaciones
- **Cleanup:** PASS; objeto restaurado ausente mediante `head()`; backup, manifiesto y directorio temporal eliminados y ausencia comprobada
- **Estado definitivo:** PASS

No se eliminaron buckets ni otros objetos. No se usó Supabase ni PostgreSQL real.
Esta ejecución demuestra restauración documental real desde una copia material hacia
S3 local; no es una restauración de base de datos ni de un backup de producción.

### Evidencia previa de restauración sintética en memoria

- **Tipo:** restauración controlada de un snapshot sintético en memoria hacia una instancia vacía
- **Estado:** PASS para el escenario automatizado sintético; no representa una restauración de backup de producción
- **Inicio registrado por la prueba:** `2026-10-03T23:58:51.813648400Z`
- **Fin registrado por la prueba:** `2026-10-03T23:58:51.816107800Z`
- **Duración medida:** `585500 ns`
- **Cantidad de objetos:** 2
- **Bytes antes/después:** iguales para ambos objetos
- **Metadata comparada:** clave, `contentType` y `contentLength`

| Objeto sintético | SHA-256 antes | SHA-256 después | contentType | longitud |
|---|---|---|---|---:|
| `consulta/41/uno.txt` | `de27fffc346bfaed4d4163367bf76eeefecf8af6c5ef4a13b20a79c6954a115e` | `de27fffc346bfaed4d4163367bf76eeefecf8af6c5ef4a13b20a79c6954a115e` | `text/plain` | 10 |
| `conciliacion/9/dos.pdf` | `f7f0e5a495ff7b0abe34689b20c06c4fac90badfad82599095b754eeaf8a66ac` | `f7f0e5a495ff7b0abe34689b20c06c4fac90badfad82599095b754eeaf8a66ac` | `application/pdf` | 14 |

El reemplazo real del contenedor backend se ejecutó satisfactoriamente para QA-DOC-16.
QA-DOC-17 se complementó con backup material y restauración operacional contra S3 local,
documentados arriba. No se ejecutaron operaciones destructivas contra producción ni datos reales.

## Ejecución Maven

Compilación previa de pruebas:

```text
Tests compilados: 76 clases fuente de test
BUILD SUCCESS
```

Suite nueva aislada histórica, antes de incorporar las clases IT
(`.\mvnw.cmd "-Dtest=Scrum285*" test`):

```text
Tests run: 19, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

Regresión documental conjunta histórica, antes de separar QA-DOC-10
(`.\mvnw.cmd "-Dtest=FileValidationServiceTest,FileResourceServiceDb03Test,Scrum285*" test`):

```text
FileResourceServiceDb03Test: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
FileValidationServiceTest: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
DocumentAdversarialTest: Tests run: 15, Failures: 1, Errors: 0, Skipped: 0
DocumentReconciliationTest: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
DocumentRecoveryTest: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
Total: Tests run: 27, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

El `BUILD FAILURE` es el resultado esperado de QA para el defecto funcional demostrado
en QA-DOC-10. Las ocho pruebas documentales preexistentes continúan pasando.

Compilación después de incorporar QA-DOC-16:

```text
Compiling 78 source files with javac [debug parameters release 21]
BUILD SUCCESS
```

Regresión permitida después de QA-DOC-16
(`.\mvnw.cmd "-Dtest=DocumentRecoveryTest,DocumentReconciliationTest" test`):

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Compilación y ejecución operacional de QA-DOC-17 mediante
`docs/testing/ops/run-document-backup-restore.ps1`:

```text
Java: 21.0.12.1
Maven Wrapper: 3.9.14
.\mvnw.cmd "-DskipTests" test-compile: BUILD SUCCESS (79 fuentes de test compiladas)
.\mvnw.cmd "-Dtest=DocumentBackupRestoreIT" test
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

La suite adversarial con el fallo conocido QA-DOC-10 / SCRUM-313 no se volvió a
ejecutar durante esta operación. Los resultados QA-DOC-13 y QA-DOC-16 se conservaron.

## Preparación para integración — separación de QA-DOC-10

Fecha: 03/10/2026, `America/Bogota`. Java confirmado: `21.0.12.1`;
Maven Wrapper confirmado: `3.9.14`. Rama conservada:
`test/scrum-285-document-recovery-adversarial`.

Se movió exclusivamente QA-DOC-10 a `DocumentChecksumIntegrityIT`, conservando
los datos, los dobles y la aserción de rechazo originales. No se usó `@Disabled`,
no se corrigió producción y no se agregó ninguna configuración de Surefire.
Las IT operacionales y diagnósticas requieren selección explícita mediante `-Dtest`.

| Ejecución | Tests run | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---|
| `test-compile` | No aplica; 80 fuentes de test compiladas | — | — | — | BUILD SUCCESS |
| Suite SCRUM-285 normal | 18 | 0 | 0 | 0 | BUILD SUCCESS |
| Regresión documental conjunta | 26 | 0 | 0 | 0 | BUILD SUCCESS |
| `mvnw.cmd test` completo | 320 | 1 | 7 | 0 | BUILD FAILURE |
| `DocumentChecksumIntegrityIT` explícita | 1 | 1 | 0 | 0 | BUILD FAILURE: SCRUM-313 reproducido |

Comandos de selección:

```powershell
.\mvnw.cmd "-DskipTests" test-compile
.\mvnw.cmd "-Dtest=DocumentAdversarialTest,DocumentReconciliationTest,DocumentRecoveryTest" test
.\mvnw.cmd "-Dtest=FileValidationServiceTest,FileResourceServiceDb03Test,DocumentAdversarialTest,DocumentReconciliationTest,DocumentRecoveryTest" test
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=DocumentChecksumIntegrityIT" test
```

Para la suite completa se establecieron solo en el proceso:
`SPRING_PROFILES_ACTIVE=test` y
`SPRING_CONFIG_LOCATION=classpath:/application-test.properties`.
Esto evitó cargar la configuración local protegida como configuración de Spring.
Los contenedores PostgreSQL de la suite preexistente usaron datos sintéticos de test.
Surefire no ejecutó ninguna `Scrum285*IT` durante `mvnw.cmd test`.
QA-DOC-13 no se volvió a ejecutar ni se convirtió en PASS o FAIL de producto.

Fallos de la suite completa, sin relación con el traslado de QA-DOC-10:

- `LoggingSafetyConfigurationTest.applicationPropertiesShouldUseSafeLoggingDefaults`:
  aserción `assertTrue` fallida en línea 27. Esa prueba preexistente lee internamente
  `application.properties`; no se abrió ni imprimió el contenido del archivo ni se
  modificó. Su contenido no se investigó por la restricción del usuario.
- `LegalCasesApplicationTests.contextLoads`: error al cargar el contexto;
  causa registrada: falta `app.mail.from-name` en la configuración restringida de test.
- `AdministracionInvariantConcurrencyTest.dosRetirosConcurrentesNoEliminanUltimaCapacidadDeRecuperacion`:
  error por umbral de fallo del contexto compartido.
- `AdministracionInvariantConcurrencyTest.dosDesactivacionesConcurrentesNoDejanCeroAdministradores`:
  mismo error de contexto.
- `AdministracionInvariantConcurrencyTest.dosRetirosConcurrentesNoDejanCeroDirectoras`:
  mismo error de contexto.
- `AdministracionInvariantRepositoryJpaTest.bloqueaYCargaSoloRolesAdministrativosEnOrdenEstable`:
  mismo error de contexto.
- `AdministracionInvariantRepositoryJpaTest.cargaCatalogoRealDePermisos`:
  mismo error de contexto.
- `AdministracionInvariantRepositoryJpaTest.cargaPermisosAsociadosSoloARolesAdministrativos`:
  mismo error de contexto.

No se alteraron tests ajenos ni configuración productiva para obtener verde.
El cambio de selección de QA-DOC-10 está verificado, pero la regresión global del
workspace no está verde. Los estados QA-DOC-01..17 permanecen exactamente en
**15 PASS, 1 FAIL y 1 BLOCKED**; los fallos ajenos de la suite completa no se suman a
esa matriz.

## Defectos encontrados

### SCRUM-313 — No se comprueba el checksum contra el contenido almacenado

`FileResourceService.complete()` valida que el tamaño almacenado coincida con el
declarado, pero no carga los bytes ni recalcula SHA-256 para compararlo con
`FileAsset.checksum`. Una huella formalmente válida pero incorrecta permite marcar el
activo como listo. Evidencia reproducible:
`DocumentChecksumIntegrityIT.checksumValidoPeroDistintoDebeSerRechazado`.

La reproducción fue trasladada desde `DocumentAdversarialTest` a esta IT sin
cambiar la expectativa de rechazo ni duplicar el caso. El sufijo IT mantiene la prueba
fuera de la detección normal de Surefire; se ejecuta mediante selección explícita.
QA-DOC-10 sigue siendo FAIL y SCRUM-313 sigue abierto funcionalmente; una regresión
normal exitosa no significa que el sistema verifique correctamente el SHA-256.

Los patrones explícitos amplios como `-Dtest=Scrum285*` pueden seleccionar también
las IT. Para regresión normal de este ticket se deben usar los tres nombres Test
exactos documentados en la sección de preparación para integración.

No se modificó código productivo para corregirlo dentro de SCRUM-285.

## Conclusión

Se obtuvieron **15 PASS, 1 FAIL y 1 BLOCKED** sobre los 17 casos. QA-DOC-10 mantiene
el defecto de integridad SCRUM-313. QA-DOC-13 queda BLOCKED / INCONCLUSIVE porque
LocalStack 4.4.0 no reproduce confiablemente la expiración de URL y no se dispone de
otro proveedor aislado adecuado. QA-DOC-16 pasó mediante reemplazo real de contenedores
efímeros y confirmó que bytes y metadata permanecieron en Storage externo.
QA-DOC-17 pasó también mediante restauración operacional real desde un backup en archivo
temporal externo, tras eliminar el activo, con integridad y metadata iguales y limpieza
completa. Los totales siguen siendo **15 PASS, 1 FAIL y 1 BLOCKED**.

## Nombres neutrales del código documental

Se renombraron las siete clases y los dos scripts operacionales para retirar los
identificadores internos de Jira del código. La trazabilidad SCRUM-285 / SCRUM-313
permanece en este documento. Las referencias a clases y scripts anteriores se muestran
con sus nombres actuales; los datos históricos, hashes, tiempos y estados no cambian.
Los nombres históricos de contenedores y objectKeys registrados arriba corresponden
a las ejecuciones originales y se conservan como evidencia.

Las variables de entorno usan ahora el prefijo `DOCUMENT_`; los logs operacionales
usan `QA_DOC_16` y `QA_DOC_17`. Los nuevos objetos sintéticos usan el prefijo
`document/qa-doc-XX/`. Estos cambios de identificación no alteran las aserciones ni
las fases operacionales. Los bytes sintéticos de una futura ejecución tendrán el texto
neutral y su propio hash; no se atribuyen a los hashes de las ejecuciones históricas.

Para reutilizar la infraestructura actual, proporcionar externamente:

```powershell
$env:DOCUMENT_S3_CONTAINER = 'scrum285-localstack'
$env:DOCUMENT_DOCKER_NETWORK = 'scrum285-net'
$env:DOCUMENT_QA_BRANCH = 'test/scrum-285-document-recovery-adversarial'
```

Las demás variables son `DOCUMENT_PHASE`, `DOCUMENT_SESSION_ID`,
`DOCUMENT_S3_ENDPOINT`, `DOCUMENT_S3_REGION`, `DOCUMENT_S3_ACCESS_KEY`,
`DOCUMENT_S3_SECRET_KEY` y `DOCUMENT_S3_BUCKET`. Los scripts generan la sesión
como antes; el script de backup usa el endpoint del host y el de contenedores recibe
el endpoint dentro de Docker. No se renombraron la rama ni recursos Docker existentes.

Verificación después del renombre (03/10/2026, Bogotá; Java 21.0.12.1 y Maven 3.9.14):

| Comando | Resultado |
|---|---|
| `.\mvnw.cmd "-DskipTests" test-compile` | BUILD SUCCESS; 80 fuentes de test |
| `.\mvnw.cmd "-Dtest=DocumentAdversarialTest,DocumentReconciliationTest,DocumentRecoveryTest" test` | Tests run: 18, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS |
| `.\mvnw.cmd "-Dtest=FileValidationServiceTest,FileResourceServiceDb03Test,DocumentAdversarialTest,DocumentReconciliationTest,DocumentRecoveryTest" test` | Tests run: 26, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS |
| `.\mvnw.cmd "-Dtest=DocumentChecksumIntegrityIT" test` | Tests run: 1, Failures: 1, Errors: 0, Skipped: 0; BUILD FAILURE; SCRUM-313 reproducido |

Los scripts renombrados pasaron el parser de PowerShell sin errores. No se repitieron
las operaciones Docker de QA-DOC-16/17 en esta fase de renombre; sus tiempos y hashes
registrados se conservan como resultados históricos reales. La matriz permanece en
15 PASS, 1 FAIL y 1 BLOCKED.
