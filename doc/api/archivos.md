# API - Archivos

> Documento ajustado contra el código fuente actual. Describe los endpoints implementados por `FileUploadController`.

## 1. Ruta base

```http
/api/files
```

Todos los endpoints requieren usuario autenticado mediante la anotación del controller:

```java
@PreAuthorize("isAuthenticated()")
```

El controller opera con autenticación general para este grupo de rutas.

---

## 2. Carga individual

```http
POST /api/files/upload
```

Tipo de contenido:

```http
multipart/form-data
```

Parámetros:

| Nombre | Tipo | Requerido | Descripción |
|---|---|---|---|
| `file` | archivo | Sí | Archivo a cargar. |
| `path` | texto | No | Subdirectorio relativo de destino. |

### Respuesta `200 OK`

```json
{
  "fileName": "consulta-10/documento.pdf",
  "fileSize": 12345,
  "message": "¡Archivo cargado exitosamente!"
}
```

Cuando se envía `path`, `fileName` puede incluir el subdirectorio relativo junto al nombre del archivo.

### Respuesta `500 Internal Server Error`

Si ocurre una excepción durante el almacenamiento, el controller responde con un objeto propio de la operación:

```json
{
  "error": "No se pudo cargar el archivo: <detalle>"
}
```

---

## 3. Carga múltiple

```http
POST /api/files/upload-multiple
```

Tipo de contenido:

```http
multipart/form-data
```

Parámetros:

| Nombre | Tipo | Requerido | Descripción |
|---|---|---|---|
| `files` | arreglo de archivos | Sí | Archivos a cargar. |
| `path` | texto | No | Subdirectorio relativo de destino. |

### Respuesta `200 OK`

El endpoint recorre los archivos recibidos y retorna una lista de resultados. Cada elemento informa el resultado de su archivo:

```json
[
  {
    "fileName": "consulta-10/documento1.pdf",
    "fileSize": 12345,
    "message": "Cargado exitosamente"
  },
  {
    "error": "Error al cargar: <detalle>"
  }
]
```

La respuesta HTTP es `200 OK` una vez procesada la lista, incluso cuando un elemento contiene el campo `error`.

---

## 4. Descarga

```http
GET /api/files/download/{ruta-relativa}
```

El mapping del controller recibe toda la ruta posterior a `/download/`:

```http
GET /api/files/download/**
```

La ruta se decodifica y se consulta en el almacenamiento.

### Respuestas

| Estado | Cuerpo | Comportamiento |
|---|---|---|
| `200 OK` | `Resource` | Retorna el archivo con `Content-Disposition: attachment`. |
| `404 Not Found` | Sin cuerpo | El archivo solicitado no fue localizado. |
| `500 Internal Server Error` | Sin cuerpo | Ocurrió una excepción durante la consulta o construcción de la descarga. |

Cuando no se puede determinar el tipo de contenido del archivo, la respuesta usa:

```text
application/octet-stream
```

---

## 5. Listado de archivos

```http
GET /api/files/list
GET /api/files/list/{subDir}
```

El mapping implementado para el listado con subdirectorio es:

```http
GET /api/files/list/{subDir:.+}
```

### Respuestas

| Estado | Cuerpo | Comportamiento |
|---|---|---|
| `200 OK` | Lista de texto | Retorna nombres de archivos en la raíz o en el subdirectorio indicado. |
| `404 Not Found` | Sin cuerpo | El directorio solicitado no fue localizado. |
| `500 Internal Server Error` | Sin cuerpo | Ocurrió una excepción durante el listado. |

---

## 6. Listado de directorios

```http
GET /api/files/directories
```

### Respuestas

| Estado | Cuerpo | Comportamiento |
|---|---|---|
| `200 OK` | Lista de texto | Retorna directorios disponibles bajo la raíz configurada. |
| `500 Internal Server Error` | Sin cuerpo | Ocurrió una excepción durante la consulta. |

---

## 7. Contratos de respuesta del controller

`FileUploadController` construye directamente sus respuestas:

| Operación | Formato de respuesta |
|---|---|
| Carga individual exitosa | Objeto con `fileName`, `fileSize` y `message`. |
| Error de carga individual | Objeto con `error`. |
| Carga múltiple | Lista de objetos con resultado individual de cada archivo. |
| Descarga no disponible o con error | Respuesta HTTP sin cuerpo. |
| Listado no disponible o con error | Respuesta HTTP sin cuerpo. |

Estos contratos son interpretables por las utilidades frontend que leen JSON o cuerpo vacío, según la operación consumida.

---

## 8. Validación de rutas

`FileStorageService` limpia nombres de archivo, normaliza rutas y rechaza nombres o subdirectorios que contengan secuencias `..`. El contrato funcional espera rutas relativas bajo la raíz configurada de almacenamiento.

---

## 9. Validaciones documentales específicas

Los endpoints genéricos de `/api/files` reciben y almacenan archivos. Las reglas de formato documental aplicadas por módulos funcionales, como los documentos PDF de conciliación, se ejecutan en los servicios de esos módulos.
