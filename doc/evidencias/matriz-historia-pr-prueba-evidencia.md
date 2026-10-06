# Matriz de Trazabilidad y Guion de Demostración: Gestión Documental

> Documento de aseguramiento de calidad, trazabilidad integral de ingeniería y protocolo de demostración funcional del subsistema de gestión documental del Consultorio Jurídico.

---

## 1. Matriz Integral de Trazabilidad: Requisito - Historia - PR - Prueba - Evidencia

| Requisito SRS | Historia (PB) | Hallazgo Atendido | Rama / PR | Pruebas Automatizadas | Evidencia de Aceptación |
|---|---|---|---|---|---|
| **RF16**<br>Cargar archivo asociado | **PB-27** (HU-31)<br>Modelo documental versionado | `SEC-01`<br>`SEC-02` | `feature/document-model-versioning`<br>PR #1 | `FileResourceServiceVersioningTest`<br>• `iniciarCargaPrimeraVersion`<br>`FileResourceControllerTest`<br>• `iniciarCargaConsulta` | Carga en staging sin rutas libres. Creación de `file_asset` con `version = 1`, `status = 'PENDING'` y `documento_logico`. |
| **RF17**<br>Consultar archivo asociado | **PB-27** (HU-32)<br>Consultar documentos almacenados | `SEC-01`<br>`OPS-01` | `feature/document-model-versioning`<br>PR #1 | `FileResourceServiceVersioningTest`<br>• `listarVersionesDocumentoLogico`<br>`FileResourceControllerTest`<br>• `listarVersiones` | Listado histórico ordenado descendente por versión. Ocultación total de `bucket` y `objectKey` en `FileResponse`. |
| **RF18**<br>Descargar archivo asociado | **PB-27** (HU-33)<br>Descargar documentos de forma segura | `SEC-01`<br>`SEC-02` | `feature/document-model-versioning`<br>PR #1 | `FileResourceServiceVersioningTest`<br>• `prepararDescargaAutorizada`<br>• `prepararDescargaSinAlcance` | URL presignada con expiración corta (15 min). Respuestas `403 Forbidden` ante intentos sin alcance. |
| **RF19**<br>Cerrar consulta jurídica | **PB-29** (HU-11)<br>Cierre formal de consulta | `DATA-01`<br>`BL-08` | `feature/aggregated-expediente-documents`<br>PR #2 | `ConsultaCierreValidationTest`<br>• `bloqueaEdicionDocumentalTrasCierre` | Inmutabilidad garantizada de todo el acervo probatorio una vez cerrada la consulta. |
| **RF20**<br>Controlar acceso por permisos | **PB-27**<br>**PB-29**<br>Control de acceso | `SEC-01`<br>`SEC-05`<br>`SEC-06` | `feature/aggregated-expediente-documents`<br>PR #2 | `FileResourceControllerExpedienteSecurityTest`<br>• `deniegaAccesoExpedienteAUsuarioSinPermiso`<br>• `deniegaAccesoAUsuarioNoAsignado` | Validación de 5 perfiles (`Estudiante`, `Asesor`, `Monitor`, `Conciliador`, `Administrativo`). Aislamiento estricto de expedientes. |
| **RF21**<br>Crear proceso jurídico | **PB-29** (HU-26)<br>Proceso judicial derivado | `WP-01`<br>`BL-08` | `feature/aggregated-expediente-documents`<br>PR #2 | `FileResourceServiceExpedienteTest`<br>• `authorizationServiceSoportaProcesoParaUploadYRead` | Incorporación de `PROCESO` en `FileResourceType`. Resolución de documentos procesales hacia la consulta raíz. |
| **RF59**<br>Crear perfil / Trazabilidad autor | **PB-27**<br>Trazabilidad documental | `AUD-01`<br>`DATA-01` | `feature/document-model-versioning`<br>PR #1 | `FileAssetVersioningTest`<br>• `servidorAsignaAutorYOrigen` | Asignación obligatoria de `uploaded_by_id` desde sesión de Spring Security. Origen `CARGA_USUARIO` calculado en servidor. |
| **RF78 / RF79**<br>Subida individual y múltiple | **PB-28** (HU-34)<br>Flujo idempotente de carga | `OPS-01`<br>`DATA-01` | `feature/document-model-versioning`<br>PR #1 | `FileResourceServiceVersioningTest`<br>• `completeIdempotenteRetornaMismoResultado`<br>• `concurrenciaNoCreaDosVersionesVigentes` | Reintento de `/complete` no duplica filas. Bloqueo pesimista e índice único parcial `uk_file_asset_doc_vigente`. |
| **Consulta Agregada Expediente** | **PB-29** (HU-20)<br>Trazabilidad completa expediente | `SEC-01`<br>`OPS-01`<br>`DATA-01` | `feature/aggregated-expediente-documents`<br>PR #2 | `FileResourceServiceExpedienteTest`<br>• `listarArchivosExpedienteFiltraPorTipoYOrigen`<br>• `listarArchivosExpedienteAplicaFiltroFechas` | Endpoint `GET /api/consultas/{id}/expediente/archivos`. Agregación sin N+1 (`LEFT JOIN FETCH`). Filtros combinados. |

---

## 2. Guion de Demostración Funcional (Demo Script)

Este guion describe la secuencia de pasos a ejecutar durante la presentación técnica ante directivos, auditores y evaluadores del sistema.

### Preparación del Entorno
- Base de datos con migraciones Flyway ejecutadas hasta la versión `V26`.
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
> *Se certifica que la implementación del subsistema documental cumple a cabalidad con los requisitos funcionales del SRS (RF16-RF21, RF59, RF78-RF80), resuelve satisfactoriamente los hallazgos críticos de seguridad y persistencia (SEC-01, SEC-02, OPS-01, DATA-01), supera el 100% de las pruebas automatizadas y garantiza la inmutabilidad y resiliencia del acervo probatorio del Consultorio Jurídico.*
