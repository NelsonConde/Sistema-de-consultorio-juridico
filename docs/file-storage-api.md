# API de archivos

La API de archivos trabaja con recursos funcionales y no expone claves ni
carpetas del bucket. Supabase permanece privado y sus credenciales sólo se
configuran en el backend.

## Flujo de carga

1. El frontend solicita una sesión con `POST` a la colección de archivos del
   recurso.
2. El backend valida autenticación, permisos y metadata, y devuelve una URL
   firmada temporal junto con `uploadId`.
3. El frontend ejecuta `PUT uploadUrl` con el archivo.
4. El frontend confirma con `POST /api/file-uploads/{uploadId}/complete`. Para
   una respuesta de seguimiento envía `{"parentId": seguimientoId}`.
5. El backend verifica el objeto mediante metadata de Storage y lo marca como
   `READY`.

## Colecciones

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

## Operaciones de archivo

```text
POST   /api/file-uploads/{uploadId}/complete
GET    /api/archivos/{fileId}/download
DELETE /api/archivos/{fileId}
```

Las operaciones sobre una respuesta reciben `parentId` como query parameter
en descarga y eliminación. El `parentId` sólo sirve para reconstruir la
relación de dominio y nunca se usa para construir una clave de Storage.

La respuesta de listado no incluye `objectKey`:

```json
{
  "id": 42,
  "fileName": "soporte.pdf",
  "size": 24576,
  "contentType": "application/pdf",
  "status": "READY",
  "createdAt": "2026-08-23T10:30:00"
}
```

Los endpoints antiguos basados en `path` fueron retirados. Para cargas
atómicas de conciliación, los endpoints de negocio todavía reciben multipart,
pero internamente usan el mismo `FileResourceService` y registran referencias
por ID.

Una carga que no se confirma permanece en `UPLOADING` y se limpia mediante el
reconciliador. Una eliminación pasa por `DELETE_PENDING`; esto permite retomar
la operación si la aplicación se detiene entre Storage y la base de datos.

## Configuración de Supabase

El bucket debe permanecer privado. Para cargas directas desde el navegador,
el endpoint S3 de Supabase debe permitir CORS únicamente para los orígenes del
frontend. No se debe configurar ninguna clave de Supabase en `NEXT_PUBLIC_*`.
