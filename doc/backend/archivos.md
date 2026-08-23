# Backend - Archivos y almacenamiento documental

> Documento ajustado contra el código fuente actual. Describe la implementación real de almacenamiento genérico y su uso por módulos funcionales.

## 1. Propósito

El backend incluye un módulo de almacenamiento para cargar, listar y descargar archivos. La fachada conserva el contrato existente, pero el proveedor de producción utiliza el bucket privado `legal-documents` de Supabase Storage mediante su API S3 compatible.

---

## 2. Componentes principales

| Componente | Responsabilidad |
|---|---|
| `FileUploadController` | Expone endpoints bajo `/api/files`. |
| `FileStorageService` | Fachada compatible con la API que delega en el proveedor de objetos. |
| `StorageProvider` | Contrato interno para desacoplar la aplicación del proveedor físico. |
| `SupabaseStorageProvider` | Implementación S3 para Supabase Storage. |
| `FileStorageException` | Excepción de almacenamiento. |
| `FileNotFoundException` | Excepción de archivo o directorio no encontrado. |
| `ConciliacionDocumentoService` | Usa almacenamiento para solicitud y acta PDF de conciliación. |

---

## 3. Configuración

La conexión a Supabase Storage se configura mediante:

```properties
supabase.storage.endpoint=${SUPABASE_STORAGE_ENDPOINT}
supabase.storage.region=${SUPABASE_STORAGE_REGION}
supabase.storage.access-key=${SUPABASE_STORAGE_ACCESS_KEY}
supabase.storage.secret-key=${SUPABASE_STORAGE_SECRET_KEY}
supabase.storage.bucket=${SUPABASE_STORAGE_BUCKET:legal-documents}
```

Las credenciales son obligatorias y deben inyectarse en Railway. No se deben guardar claves reales en el repositorio.

---

## 4. Carga individual y múltiple

La carga individual usa:

```http
POST /api/files/upload
```

La carga múltiple usa:

```http
POST /api/files/upload-multiple
```

Ambos endpoints reciben `MultipartFile` y un `path` opcional. Si se envía `path`, el archivo se almacena bajo ese subdirectorio relativo.

La carga múltiple registra un resultado por archivo y permite respuestas mixtas de éxito y error dentro de la misma lista. La petición multipart continúa siendo compatible con el frontend actual.

---

## 5. Descarga y listado

El controller expone:

```http
GET /api/files/download/**
GET /api/files/list
GET /api/files/list/{subDir}
GET /api/files/directories
```

La descarga retorna un `Resource`. El listado de archivos retorna nombres de archivo del directorio solicitado. El listado de directorios recorre la raíz configurada y devuelve rutas relativas de directorios.

---

## 6. Seguridad de rutas

`FileStorageService` aplica las siguientes reglas:

- limpia nombres de archivo con `StringUtils.cleanPath`;
- rechaza nombres de archivo que contengan `..`;
- rechaza rutas absolutas;
- normaliza claves antes de almacenarlas o cargarlas;
- delega la escritura y lectura en `SupabaseStorageProvider`.

El contrato funcional espera rutas relativas bajo la raíz configurada de almacenamiento.

---

## 7. Validación de tipo documental

El almacenamiento genérico todavía no valida el contenido real del archivo. En este bloque se establece un límite multipart de 10 MB, coherente con el bucket `legal-documents`; la validación de tipos y contenido se implementará en el bloque de seguridad.

Las reglas de tipo documental se aplican en el módulo que usa el archivo. Por ejemplo, `ConciliacionDocumentoService` exige PDF para solicitud y acta.

---

## 8. Rutas lógicas usadas por módulos

Los módulos funcionales usan rutas lógicas sobre el almacenamiento:

| Módulo | Ruta lógica |
|---|---|
| Consultas | Directorios asociados al id de la consulta. |
| Seguimientos - tarea | `tareas-{seguimientoId}-documentos` |
| Seguimientos - respuesta | `tareas-{seguimientoId}-respuestas-{respuestaId}` |
| Conciliación - solicitud | `conciliacion/{id}/solicitud.pdf` |
| Conciliación - acta | `conciliacion/{id}/acta.pdf` |

Estas rutas se usan como contrato lógico entre backend, frontend y almacenamiento.
