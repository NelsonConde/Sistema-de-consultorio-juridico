# API de archivos y versionado documental

La API de archivos trabaja con recursos funcionales y un modelo documental versionado. No expone claves ni carpetas del bucket. Supabase permanece privado y sus credenciales sólo se configuran en el backend.

## Flujo de carga y versionamiento

1. El frontend o cliente solicita una sesión con `POST` a la colección de archivos del recurso:
   - Puede enviar opcionalmente `documentoLogico` (UUID) para cargar una nueva versión ($N+1$) de un documento existente.
   - Puede enviar `tipoDocumental` para clasificar el documento.
   - Si no se envía `documentoLogico`, el servidor genera un nuevo documento lógico con versión inicial 1.
2. El backend valida autenticación, permisos funcionales del recurso y metadata, y devuelve una URL firmada temporal junto con `uploadId`.
3. El cliente ejecuta `PUT uploadUrl` con el archivo directamente al storage privado.
4. El cliente confirma con `POST /api/file-uploads/{uploadId}/complete`.
5. El backend verifica el objeto mediante metadata de Storage, marca la nueva versión como `VIGENTE` y transiciona cualquier versión previa a `HISTORICO`.

## Colecciones de carga por recurso

```text
POST /api/consultas/{id}/archivos/uploads
GET  /api/consultas/{id}/archivos

POST /api/seguimientos/{id}/archivos/uploads
GET  /api/seguimientos/{id}/archivos

POST /api/seguimientos/{seguimientoId}/respuestas/{respuestaId}/archivos/uploads
GET  /api/seguimientos/{seguimientoId}/respuestas/{respuestaId}/archivos

POST /api/conciliaciones/{id}/archivos/uploads
GET  /api/conciliaciones/{id}/archivos
```

## Operaciones de archivo y versiones

```text
POST   /api/file-uploads/{uploadId}/complete
GET    /api/archivos/{fileId}/download
DELETE /api/archivos/{fileId}
GET    /api/documentos/{documentoLogico}/versiones
```

## Reglas del modelo documental

1. **Primera versión = 1; Siguiente = N+1**:
   - Cada nuevo documento lógico inicia en la versión 1.
   - Las actualizaciones bajo el mismo `documentoLogico` se incrementan secuencialmente ($N+1$) bajo control transaccional y bloqueo pesimista.
2. **Una sola versión VIGENTE por documento lógico**:
   - Enforzada a nivel de base de datos mediante el índice único parcial `uk_file_asset_doc_vigente` (`WHERE status = 'VIGENTE'`).
   - Al confirmarse una versión $N+1$, la versión previa pasa automáticamente a `HISTORICO`.
3. **Inmutabilidad histórica**:
   - Los registros en estado `HISTORICO` son inmutables para garantizar valor probatorio y auditoría.
4. **Baja lógica**:
   - La eliminación (`DELETE /api/archivos/{fileId}`) marca la versión vigente como inactiva/eliminada (`DELETED`) sin destruir los metadatos históricos ni el rastro de auditoría.
5. **Cálculo exclusivo en servidor**:
   - El autor (`uploadedBy`), el origen (`origen`) y la versión (`version`) se determinan en el backend y no pueden ser falsificados por el cliente.
6. **Checksum SHA-256**:
   - Se valida y registra la huella criptográfica SHA-256 de cada archivo cargado.

## Estructura de respuesta (FileResponse)

```json
{
  "id": 42,
  "documentoLogico": "7fa8b9c0-1234-5678-9abc-def012345678",
  "version": 2,
  "tipoDocumental": "CONCILIACION_SOLICITUD",
  "origen": "CARGA_USUARIO",
  "referenciaAnteriorId": 38,
  "fileName": "solicitud_firmada.pdf",
  "size": 24576,
  "contentType": "application/pdf",
  "status": "VIGENTE",
  "createdAt": "2026-09-05T10:30:00"
}
```
