# Matriz de Trazabilidad y Guion de Demostración: Gestión Documental

> Documento de aseguramiento de calidad, trazabilidad integral de ingeniería y protocolo de demostración funcional del subsistema de gestión documental del Consultorio Jurídico.

---

## 1. Matriz de trazabilidad funcional: Requisito - PR - Prueba - Evidencia

> **Conciliación CP-09 (16/10/2026):** Solo se corrigió la fila de cierre. El PR #2 y la prueba mencionada anteriormente no acreditaban el caso. Los demás registros fueron revisados documentalmente en SCRUM-321 según la nota y los límites consignados a continuación. La corrección documental no cambia el resultado del auditor ni constituye ejecución de pruebas.

> **Revisión SCRUM-321 — 02/11/2026:** Se corrigen las referencias restantes mediante inspección de las clases aportadas y búsquedas Git comunicadas por el ejecutor. Los identificadores RF históricos se conservan como tales y no sustituyen el SRS/EP5 vigente. El commit de referencia comunicado es `21d752b66ed574f1be2f646cb21052fc74242bcd`; se debe confirmar que los cuatro archivos de pruebas no presentan diferencias respecto a él antes de cerrar la revisión. La existencia de pruebas no acredita su ejecución. CP-09 y el acta histórica se conservan.

> **PR retirados como evidencia documental:** [PR #1](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/1), «fix(sec-06): scope disciplinary alerts by user access», y [PR #2](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/2), «fix: reintegrate DB-03 optimistic locking». Títulos y enlaces aportados por el ejecutor; no se atribuye un PR sustituto sin verificación.

| Requisito SRS | Funcionalidad relacionada | Rama / PR | Pruebas Automatizadas | Evidencia de Aceptación |
|---|---|---|---|---|
| **RF16 (histórico)** | Carga y primera versión | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileAssetVersioningTest.primeraVersionEsUnoYGeneraDocumentoLogicoNuevo`; `FileResourceServiceVersioningTest.initiatePropagaDocumentoLogicoYTipoDocumental` | Inspección estática: Versión 1, UUID, autor de sesión y origen CARGA_USUARIO; estado inicial UPLOADING e inactivo. Pruebas con dependencias simuladas; no acreditan carga en staging ni persistencia real. Ejecución no acreditada en esta revisión. |
| **RF17 (histórico)** | Consulta de versiones | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileResourceServiceVersioningTest.listVersionsAutorizaYRetornaVersiones`; `FileAssetVersioningTest.listVersionsRetornaTodasLasVersionesOrdenadasDescendente` | Inspección estática: Autorización de lectura y respuesta de versiones. El repositorio está simulado: no acredita ordenamiento SQL real ni ausencia de campos sensibles en JSON. Ejecución no acreditada en esta revisión. |
| **RF18 (histórico)** | Descarga segura | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `No se localizaron prepararDescargaAutorizada ni prepararDescargaSinAlcance en la búsqueda realizada.` | Inspección estática: Descarga, TTL y rechazo sin alcance no acreditados mediante las cuatro clases revisadas. No se infiere defecto funcional. Ejecución no acreditada en esta revisión. |
| **RF05**<br>Cerrar/archivar lógicamente consulta (histórico RF19) | Cierre con resultado final y sin pendientes; archivo lógico diferenciado | Código inspeccionado en `develop`: `ConsultaEstadoService`, `ConsultaAccessService`, `ConsultaController`, `ConsultasJuridicasForm.jsx`. **No se acreditó un PR específico del cierre**; PR #2 se refiere a DB-03. | **No acreditada.** No se localizó `ConsultaCierreValidationTest` ni `bloqueaEdicionDocumentalTrasCierre`; no afirmar prueba automatizada aprobada. | Correspondencia vigente RF05 / RN-05 / CU-03 / PB-09; se requiere evidencia adicional para considerar ejecutado CP-09. Resultado histórico externo preservado en el registro único de Sprint 2. |
| **RF20 (histórico)** | Permisos y alcance del expediente | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileResourceControllerExpedienteSecurityTest.usuarioSinPermisoRecibe403Forbidden`; `perfilEstudianteConExpedienteAjenoRecibe403Forbidden`; `perfilAsesorConExpedienteAjenoRecibe403Forbidden`; `perfilMonitorConExpedienteAjenoRecibe403Forbidden`; `perfilConciliadorRecibe403ForbiddenEnExpedienteGeneral` | Inspección estática: MockMvc standalone y dependencias simuladas. Incluye acceso permitido de Administrador y perfiles asignados; Conciliador recibe 403 en expediente general. No acredita cadena completa de seguridad ni acceso de un perfil Administrativo. Ejecución no acreditada en esta revisión. |
| **RF21 (histórico)** | Recursos de proceso | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileResourceServiceExpedienteTest.authorizationServiceSoportaProcesoParaUploadYRead`; `FileAssetVersioningTest.resolveTipoDocumentalParaProcesoRetornaProcesoDocumento` | Inspección estática: Delegación a controles de actualización/lectura de procesos y asignación de PROCESO_DOCUMENTO. No acredita creación de procesos ni resolución SQL completa del expediente. Ejecución no acreditada en esta revisión. |
| **RF59 (histórico)** | Autoría documental | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileAssetVersioningTest.primeraVersionEsUnoYGeneraDocumentoLogicoNuevo` | Inspección estática: Comprueba autor obtenido del servicio de usuario actual y origen CARGA_USUARIO al iniciar. No acredita creación de perfiles ni auditoría extremo a extremo. Ejecución no acreditada en esta revisión. |
| **RF78 / RF79 (históricos)** | Versionado; idempotencia y concurrencia pendientes de evidencia | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileAssetVersioningTest.siguienteVersionEsNMasUnoYEnlazaReferenciaAnterior`; `completarCargaTransicionaAnteriorAHistoricoYMarcaNuevaVigente`; `versionesHistoricasSonInmutablesYNoPuedenEliminarse` | Inspección estática: Versionado secuencial y rechazo de dos operaciones de borrado sobre históricos con repositorio simulado. No se localizaron completeIdempotenteRetornaMismoResultado ni concurrenciaNoCreaDosVersionesVigentes; no se acredita idempotencia, concurrencia real ni inmutabilidad universal. Ejecución no acreditada en esta revisión. |
| **Consulta agregada del expediente** | DTO, autorización y filtros | PR documental no acreditado; se retira la asociación anterior a PR #1/#2. | `FileResourceServiceExpedienteTest.listExpedienteFilesValidaAutorizacionDeConsultaPrimero`; `listExpedienteFilesLanzaExcepcionSiFechaDesdeEsPosteriorAFechaHasta`; `listExpedienteFilesMapeaCorrectamenteADtoSinExponerBucketNiObjectKey`; `FileResourceControllerExpedienteSecurityTest.filtrosPropagadosCorrectamenteAServicio`; `FileAssetVersioningTest.findExpedienteFilesPropagaParametrosYFiltraEstadosVisibles` | Inspección estática: Comprueba llamadas de autorización, mapeo, rango inválido y propagación de parámetros. No acredita orden de llamadas, ausencia explícita de bucket/objectKey en JSON, filtrado SQL, consulta única ni ausencia de N+1. Ejecución no acreditada en esta revisión. |
---

## 1.1. Evidencia posterior de integridad y limitaciones del proveedor

- **Histórico QA-DOC-10:** conservar FAIL y los resultados originales de la ejecución del 03/10/2026. No reclasificar retroactivamente.
- **Corrección:** [SCRUM-313](https://gestion-casos-juridicos.atlassian.net/browse/SCRUM-313) figura Finalizada. Su comentario de cierre identifica [PR #23](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/pull/23), revisión aprobada y merge `f95dc693a5999a0b95762ba6efc2a2994e379705`.
- **Código inspeccionado:** `FileResourceService.complete()` invoca `validateStoredContent()` antes de `markReady()`. Cuando existe checksum declarado, calcula SHA-256 de los bytes leídos y compara ignorando mayúsculas/minúsculas; una diferencia marca la carga fallida y lanza excepción. Sin checksum declarado no realiza esa comparación. Archivo comunicado sin diferencias respecto a `21d752b66ed574f1be2f646cb21052fc74242bcd`.
- **CI histórica:** [ejecución 37401326094](https://github.com/NelsonConde/Sistema-de-consultorio-juridico/actions/runs/37401326094), commit `c02f66d04b1baabb6a29d01e5768fbad9ed93d9c`. El ejecutor comunicó la línea de log de `FileResourceChecksumTest`: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`. Son cuatro escenarios parametrizados para CONSULTA y CONCILIACION, con mocks. No constituye ejecución nueva ni integración contra Supabase real, ni acredita un cambio documental posterior.
- **QA-DOC-13:** conservar BLOCKED / INCONCLUSIVE según el registro histórico de recuperación `document-recovery-adversarial-evidence.md` (ruta versionada exacta pendiente de confirmar). LocalStack Community 4.4.0 devolvió HTTP 200 antes y después del vencimiento. El fallo técnico de la aserción no se clasifica como defecto del producto ni como PASS. No se repitió la ejecución en esta revisión.

## 2. Guion de Demostración Funcional (Demo Script)

Este guion describe la secuencia de pasos a ejecutar durante la presentación técnica ante directivos, auditores y evaluadores del sistema.

### Preparación del Entorno

- Verificar la preparación de la base de datos contra el procedimiento vigente. Se identificó el script [`V20260905_01__version_document_model.sql`](../../backend/app/db/migration/V20260905_01__version_document_model.sql), que modifica una tabla `file_asset` existente y normaliza sus metadatos. No se acredita aquí su ejecución ni su gestión automática mediante Flyway. La referencia anterior a `V26` no se localizó en la rama ni en el historial disponible revisados.

- Usuario 1: `estudiante@ufps.edu.co` (Asignado a Consulta #10).

- Usuario 2: `asesor@ufps.edu.co` (Supervisor de Consulta #10).

- Usuario 3: `ajeno@ufps.edu.co` (Estudiante sin relación con Consulta #10).

---

### Paso 1: Carga de Documento Inicial (Versión 1)

1. **Acción**: Iniciar sesión como `estudiante@ufps.edu.co` y navegar al expediente de la Consulta #10.

2. **Operación**: Adjuntar el archivo `cedula_consultante.pdf` bajo clasificación `CONSULTA_ANEXO`.

3. **Validación Técnica**:

   - Se invoca `POST /api/consultas/10/archivos/uploads`.

   - Se suben los bytes al bucket privado.

   - Se confirma con `POST /api/file-uploads/{uploadId}/complete`.

   - La respuesta retorna `version: 1`, `status: "VIGENTE"`, `documentoLogico: "<UUID>"`.

   - El autor registrado es `estudiante@ufps.edu.co` (no especificado por el cliente).

---

### Paso 2: Versionamiento Secuencial ($N+1$) e Inmutabilidad Histórica

1. **Acción**: El estudiante detecta una corrección en el documento y solicita cargar una nueva versión del documento anterior.

2. **Operación**: Seleccionar "Reemplazar documento" y cargar `cedula_consultante_actualizada.pdf`.

3. **Validación Técnica**:

   - Se envía `reemplazaDocumentoLogico: "<UUID>"`.

   - Al completar la carga, la nueva fila recibe `version: 2`, `status: "VIGENTE"`.

   - La versión 1 pasa automáticamente a `status: "HISTORICO"`.

   - Al consultar `/api/documentos/{documentoLogico}/versiones`, se listan ambas versiones en orden cronológico inverso, garantizando la inmutabilidad de la versión 1.

---

### Paso 3: Flujo Idempotente ante Fallos de Red

1. **Acción**: Simular una retransmisión de la llamada de completado por latencia o reintento automático del navegador.

2. **Operación**: Ejecutar un segundo `POST /api/file-uploads/{uploadId}/complete` con el mismo `uploadId`.

3. **Validación Técnica**:

   - El servidor responde `200 OK` retornando exactamente el mismo objeto JSON.

   - No se duplican registros en la tabla `file_asset`.

   - No se alteran los datos ni se generan versiones $N+2$ espurias.

---

### Paso 4: Consulta Agregada del Expediente

1. **Acción**: Navegar a la pestaña "Acervo Documental del Expediente".

2. **Operación**: Consultar el acervo agregado de la Consulta #10.

3. **Validación Técnica**:

   - Se invoca `GET /api/consultas/10/expediente/archivos`.

   - En una sola pantalla consolidada aparecen:

     - El anexo de la consulta inicial.

     - Los documentos de los seguimientos asignados.

     - Las piezas procesales cargadas en el proceso judicial vinculado.

     - El acta de audiencia del trámite de conciliación remitido.

   - Probar los filtros por tipo (`PROCESO_DOCUMENTO`), origen (`CARGA_USUARIO`) y rango de fechas.

---

### Paso 5: Demostración de Seguridad y Aislamiento de Casos

1. **Acción**: Iniciar sesión como `ajeno@ufps.edu.co` (usuario sin asignación en Consulta #10).

2. **Operación**: Intentar acceder al expediente documental mediante `GET /api/consultas/10/expediente/archivos` o descargar un archivo mediante su `fileId`.

3. **Validación Técnica**:

   - El sistema responde categóricamente con `403 Forbidden` (`BusinessException: "No tiene permisos para acceder a esta consulta jurídica"`).

   - Se confirma que ningún estudiante o asesor puede ver o descargar archivos pertenecientes a casos ajenos.

---

## 3. Acta de Revisión Técnica y Aprobación

| Rol de Aprobación | Responsable Técnico | Estado | Fecha de Validación |

|---|---|---|---|

| **Líder Backend y Seguridad** | Nelson Conde | Aprobado | 2026-09-05 |

| **Ingeniería de Base de Datos** | Juan Camilo | Aprobado | 2026-09-05 |

| **Aseguramiento de Calidad (QA)** | Edwin | Aprobado | 2026-09-05 |

| **Integración Frontend / API** | Juan Diego | Aprobado | 2026-09-05 |

| **Arquitectura y Almacenamiento** | José Manuel | Aprobado | 2026-09-05 |

**Conclusión del Comité Técnico**:

> **Se certifica que la implementación del subsistema documental cumple a cabalidad con los requisitos funcionales del SRS (RF16-RF21, RF59, RF78-RF80), resuelve satisfactoriamente los hallazgos críticos de seguridad, operación y persistencia, supera el 100% de las pruebas automatizadas y garantiza la inmutabilidad y resiliencia del acervo probatorio del Consultorio Jurídico.**

La mención al 100 % pertenece al acta histórica del 2026-09-05 y se limita al conjunto de pruebas automatizadas del subsistema documental ejecutado y revisado en esa validación. No representa el resultado de la suite completa actual del backend ni acredita una ejecución integral de CI.
