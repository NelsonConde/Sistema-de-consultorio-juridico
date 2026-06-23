# Backend - Archivos y almacenamiento documental

> Documento ajustado contra el código fuente actual. Describe la implementación real de almacenamiento genérico y su uso por módulos funcionales.

## 1. Propósito

El backend incluye un módulo de almacenamiento para cargar, listar y descargar archivos. Este módulo es genérico y sirve como soporte documental para consultas, seguimientos y conciliaciones.

---

## 2. Componentes principales

| Componente | Responsabilidad |
|---|---|
| `FileUploadController` | Expone endpoints bajo `/api/files`. |
| `FileStorageService` | Guarda, carga, lista archivos y directorios. |
| `FileStorageException` | Excepción de almacenamiento. |
| `FileNotFoundException` | Excepción de archivo o directorio no encontrado. |
| `ConciliacionDocumentoService` | Usa almacenamiento para solicitud y acta PDF de conciliación. |

---

## 3. Configuración

El directorio raíz se configura mediante:

```properties
file.upload-dir
```

Al iniciar, `FileStorageService` normaliza la ruta, crea el directorio si no existe y lo usa como raíz de almacenamiento.

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

La carga múltiple registra un resultado por archivo y permite respuestas mixtas de éxito y error dentro de la misma lista.

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
- rechaza subdirectorios que contengan `..`;
- normaliza rutas antes de almacenar o cargar;
- crea directorios si son necesarios.

El contrato funcional espera rutas relativas bajo la raíz configurada de almacenamiento.

---

## 7. Validación de tipo documental

El almacenamiento genérico no valida extensión o MIME type de forma funcional. Es decir, `/api/files/upload` y `/api/files/upload-multiple` guardan el archivo recibido si supera las validaciones de ruta.

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
