# API - Gestión Documental y Archivos

> Documentación de contratos de API y Swagger/OpenAPI del subsistema documental. Refleja los controladores `FileResourceController` y `FileUploadController`, alineados con las reglas de seguridad, inmutabilidad y consulta agregada por expediente.

---

## 1. Principios de Diseño de la API Documental

1. **Orientada a recursos del dominio**: No se exponen endpoints públicos que reciban rutas arbitrarias del sistema de archivos ni claves internas del bucket de almacenamiento.
2. **Autorización granular por alcance**: Toda operación valida previamente la autenticación del usuario y su asignación sobre la consulta raíz del expediente mediante `FileResourceAuthorizationService` y los servicios de acceso del dominio (`ConsultaAccessService`, etc.).
3. **Flujo de carga en dos fases con staging**: Se separa la iniciación (`initiate`) de la confirmación (`complete`), permitiendo el uso de URLs presignadas o streaming directo hacia almacenamiento privado de objetos (Supabase Storage / S3).
4. **Idempotencia estricta**: Los reintentos de completado retornan el mismo resultado sin duplicar registros ni corromper bytes.
5. **Opacidad de almacenamiento**: Las respuestas públicas (`FileResponse`, `ExpedienteDocumentoResponse`) ocultan por completo los nombres de bucket y las claves físicas (`objectKey`).

---

## 2. Endpoints de Iniciación de Carga (`initiate`)

Inician una sesión de carga en staging para un recurso funcional.

### Métodos HTTP y Rutas

```http
POST /api/consultas/{consultaId}/archivos/uploads
POST /api/seguimientos/{seguimientoId}/archivos/uploads
POST /api/seguimientos/{seguimientoId}/respuestas/{respuestaId}/archivos/uploads
POST /api/procesos/{procesoId}/archivos/uploads
POST /api/conciliaciones/{conciliacionId}/archivos/uploads
```

### Cabeceras

```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

### Cuerpo de la Petición (`FileUploadRequest`)

```json
{
  "originalFileName": "poder_especial.pdf",
  "contentType": "application/pdf",
  "size": 2457600,
  "tipoDocumental": "PROCESO_DOCUMENTO",
  "reemplazaDocumentoLogico": null
}
```

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `originalFileName` | String | Sí | Nombre original del archivo (sanitizado contra traversal `..`). |
| `contentType` | String | Sí | Tipo MIME admitido (ej. `application/pdf`, `image/jpeg`). |
| `size` | Long | Sí | Tamaño esperado en bytes (máximo 10 MB). |
| `tipoDocumental` | String | No | Clasificación funcional (`CONSULTA_ANEXO`, `PROCESO_DOCUMENTO`, etc.). Por defecto `GENERAL`. |
| `reemplazaDocumentoLogico` | UUID | No | Si se envía, la carga se procesará como una nueva versión ($N+1$) del documento indicado. |

### Respuesta `200 OK` (`FileUploadResponse`)

```json
{
  "uploadId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "uploadUrl": "https://<supabase-host>/storage/v1/s3/legal-documents/temp/3fa85f64...?[presigned-query]",
  "expiresAt": "2026-09-05T16:00:00Z"
}
```

---

## 3. Endpoints de Finalización y Cancelación

### Completar carga (`complete`)

```http
POST /api/file-uploads/{uploadId}/complete
```

Cuerpo opcional (`FileUploadCompletionRequest`):

```json
{
  "parentId": null
}
```

#### Comportamiento

1. Verifica que el archivo exista en el bucket privado.
2. Compara el tamaño real y calcula la suma criptográfica SHA-256.
3. Actualiza el estado a `VIGENTE` (y si es una nueva versión, marca la anterior como `HISTORICO`).
4. Si se repite la llamada con el mismo `uploadId`, devuelve de forma idempotente el mismo resultado sin error.

#### Respuesta `200 OK` (`FileResponse`)

```json
{
  "id": 105,
  "uploadId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "originalFileName": "poder_especial.pdf",
  "contentType": "application/pdf",
  "fileSize": 2457600,
  "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "resourceType": "PROCESO",
  "resourceId": 42,
  "documentoLogico": "8f8b7e21-0a14-419b-bc9d-4c3d82a17011",
  "version": 1,
  "status": "VIGENTE",
  "tipoDocumental": "PROCESO_DOCUMENTO",
  "origen": "CARGA_USUARIO",
  "uploadedByUsername": "estudiante@ufps.edu.co",
  "createdAt": "2026-09-05T15:45:00Z"
}
```

### Cancelar o abortar carga (`abort`)

```http
DELETE /api/file-uploads/{uploadId}
```

#### Respuesta `204 No Content`

Marca el archivo como `FAILED` o `DELETE_PENDING` y compensa eliminando los bytes huérfanos del almacenamiento.

---

## 4. Endpoints de Descarga y Versiones

### Preparar descarga segura (`prepareDownload`)

```http
GET /api/archivos/{fileId}/download
```

Parámetros opcionales:
- `parentId` (Long): Requerido cuando se descargan archivos pertenecientes a respuestas de seguimiento.

#### Respuesta `200 OK` (`FileDownloadResponse`)

```json
{
  "fileId": 105,
  "fileName": "poder_especial.pdf",
  "contentType": "application/pdf",
  "downloadUrl": "https://<supabase-host>/storage/v1/s3/legal-documents/proceso/42/poder_especial.pdf?[presigned-token]",
  "expiresAt": "2026-09-05T16:15:00Z"
}
```

### Consultar versiones de un documento lógico (`listVersions`)

```http
GET /api/documentos/{documentoLogico}/versiones
```

#### Respuesta `200 OK` (`List<FileResponse>`)

Retorna la lista de todas las versiones del documento lógico ordenadas descendente por número de versión ($N, N-1, \dots, 1$), identificando claramente la versión `VIGENTE` y las versiones en estado `HISTORICO`.

### Baja lógica de archivo (`delete`)

```http
DELETE /api/archivos/{fileId}
```

#### Respuesta `204 No Content`

Establece `active = false` y `status = 'ANULADO'` en el registro bajo control de auditoría. No destruye el registro físico de la base de datos para preservar la trazabilidad procesal.

---

## 5. Consulta Documental Agregada por Expediente

Consolida en una única respuesta todos los documentos vigentes vinculados a la consulta raíz y a sus entidades derivadas (`CONSULTA`, `SEGUIMIENTO`, `RESPUESTA`, `PROCESO`, `CONCILIACION`).

### Método HTTP y Ruta

```http
GET /api/consultas/{consultaId}/expediente/archivos
```

### Parámetros de Consulta (Query Params)

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `tipoDocumental` | String | No | Filtrar por clasificación (ej. `CONSULTA_ANEXO`, `PROCESO_DOCUMENTO`). |
| `resourceType` | String | No | Filtrar por módulo (`CONSULTA`, `SEGUIMIENTO`, `RESPUESTA`, `PROCESO`, `CONCILIACION`). |
| `origen` | String | No | Filtrar por origen (`CARGA_USUARIO`, `SISTEMA`, `MIGRADO`). |
| `autor` | String | No | Filtrar por autor (ID numérico o nombre de usuario). |
| `fechaDesde` | LocalDate | No | Fecha mínima de carga (`YYYY-MM-DD`). |
| `fechaHasta` | LocalDate | No | Fecha máxima de carga (`YYYY-MM-DD`). |

### Respuesta `200 OK` (`List<ExpedienteDocumentoResponse>`)

```json
[
  {
    "fileId": 105,
    "originalFileName": "poder_especial.pdf",
    "contentType": "application/pdf",
    "fileSize": 2457600,
    "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "resourceType": "PROCESO",
    "resourceId": 42,
    "documentoLogico": "8f8b7e21-0a14-419b-bc9d-4c3d82a17011",
    "version": 1,
    "tipoDocumental": "PROCESO_DOCUMENTO",
    "origen": "CARGA_USUARIO",
    "uploadedBy": "estudiante@ufps.edu.co",
    "createdAt": "2026-09-05T15:45:00Z"
  },
  {
    "fileId": 89,
    "originalFileName": "cedula_consultante.pdf",
    "contentType": "application/pdf",
    "fileSize": 1048576,
    "checksum": "fa8762...55",
    "resourceType": "CONSULTA",
    "resourceId": 10,
    "documentoLogico": "5a4c3b21-9988-4433-2211-001122334455",
    "version": 1,
    "tipoDocumental": "CONSULTA_ANEXO",
    "origen": "CARGA_USUARIO",
    "uploadedBy": "asesor@ufps.edu.co",
    "createdAt": "2026-09-04T10:00:00Z"
  }
]
```

---

## 6. Manejo de Errores y Códigos HTTP

El subsistema utiliza el manejador de errores global del backend (`GlobalExceptionHandler`):

| Código HTTP | Escenario | Mensaje de Ejemplo |
|---|---|---|
| `400 Bad Request` | Parámetros inválidos, archivo corrupto o fechas invertidas. | `"La fecha final no puede ser anterior a la fecha inicial"` |
| `401 Unauthorized` | Petición sin token JWT o con token revocado/expirado. | `"No autenticado"` |
| `403 Forbidden` | Usuario sin rol suficiente o fuera del alcance de la consulta/expediente. | `"No tiene permisos para acceder a esta consulta jurídica"` |
| `404 Not Found` | Recurso funcional, expediente o `uploadId` inexistente. | `"El recurso especificado no fue localizado"` |
| `409 Conflict` | Conflicto de versiones concurrentes sobre un mismo documento lógico. | `"Ya existe una versión en trámite para este documento lógico"` |
| `503 Service Unavailable` | Fallo de conexión con Supabase Storage o MinIO. | `"Almacenamiento no disponible. Intente más tarde."` |
