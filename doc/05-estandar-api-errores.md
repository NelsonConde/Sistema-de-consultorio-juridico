# Estándar de API y manejo de errores

## Convención general

El backend expone una API REST bajo el prefijo:

```text
/api
```

Los controllers usan métodos HTTP según la operación:

| Método | Uso general |
|---|---|
| `GET` | Consulta de recursos, listados, detalle, reportes y descargas. |
| `POST` | Creación, acciones con cuerpo multipart o acciones funcionales que crean artefactos. |
| `PUT` | Actualización de datos generales. |
| `PATCH` | Cambio de estado, activación/desactivación o acciones parciales. |
| `DELETE` | Eliminación lógica o desactivación según módulo. |

## Separación entre edición y ciclo de vida

El código fuente distingue edición de datos generales y cambios de estado. En módulos como consultas, procesos, seguimientos, conciliaciones, roles, permisos y perfiles, los cambios de estado o activación usan endpoints `PATCH` específicos.

Esto permite que un `PUT` opere sobre la información editable del recurso y que el ciclo de vida se gestione mediante endpoints dedicados.

## Autenticación en API

Salvo endpoints públicos declarados en `SecurityConfig`, las peticiones requieren autenticación. El token JWT viaja en cookie HTTP-only y el frontend envía peticiones autenticadas con:

```javascript
credentials: "include"
```

## Endpoints públicos

La configuración de seguridad permite como públicos:

```text
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/solicitar-recuperacion
POST /api/auth/restablecer-password
GET  /v3/api-docs/**
GET  /swagger-ui/**
GET  /swagger-ui.html
```

También permite `OPTIONS /**` para preflight CORS.

## Respuestas producidas por manejadores de excepciones

Los errores procesados por `GlobalExceptionHandler` y `SecurityExceptionHandler` utilizan `ErrorResponseDTO`.

### `ErrorResponseDTO`

```json
{
  "fecha": "2026-01-01T10:00:00",
  "estado": 400,
  "error": "Error de negocio",
  "mensaje": "Mensaje descriptivo",
  "ruta": "/api/recurso"
}
```

Cuando existen errores de validación por campo, la respuesta incluye `detalles`:

```json
{
  "fecha": "2026-01-01T10:00:00",
  "estado": 400,
  "error": "Error de validación",
  "mensaje": "Uno o más campos no son válidos",
  "ruta": "/api/recurso",
  "detalles": {
    "campo": "Mensaje de validación"
  }
}
```

El campo `detalles` solo se serializa cuando tiene valor.

### Respuestas de `GlobalExceptionHandler`

| Condición procesada | Estado HTTP | `error` de la respuesta | Contenido |
|---|---:|---|---|
| `BusinessException` | `400 Bad Request` | `Error de negocio` | Usa el mensaje funcional de la excepción. |
| Validación de DTO con `@Valid` | `400 Bad Request` | `Error de validación` | Incluye `detalles` por campo. |
| Violación de restricciones en parámetros | `400 Bad Request` | `Error de validación` | Incluye `detalles` por parámetro. |
| Tipo de parámetro inválido | `400 Bad Request` | `Solicitud inválida` | Describe el parámetro recibido. |
| Parámetro obligatorio ausente | `400 Bad Request` | `Solicitud inválida` | Identifica el parámetro faltante. |
| Cuerpo JSON no legible | `400 Bad Request` | `Solicitud inválida` | Informa que el cuerpo no es válido. |
| Método HTTP no soportado | `405 Method Not Allowed` | `Método no permitido` | Informa que el método no aplica al recurso. |
| Acceso denegado procesado por advice | `403 Forbidden` | `No autorizado` | Informa ausencia de permisos. |
| Excepción general procesada por advice | `500 Internal Server Error` | `Error interno del servidor` | Retorna un mensaje general. |

### Respuestas de `SecurityExceptionHandler`

Las respuestas generadas directamente por la cadena de Spring Security mantienen la estructura `ErrorResponseDTO`:

| Condición procesada | Estado HTTP | `error` | `mensaje` |
|---|---:|---|---|
| Petición sin autenticación válida para un recurso protegido | `401 Unauthorized` | `No autenticado` | `Debe iniciar sesión para acceder a este recurso` |
| Petición autenticada sin autoridad suficiente | `403 Forbidden` | `No autorizado` | `No tiene permisos para acceder a este recurso` |

## Respuestas construidas por endpoints específicos

Determinados controllers construyen directamente el cuerpo de respuesta de operaciones especializadas. Sus contratos se documentan como parte de la API del módulo.

### Archivos: `/api/files`

`FileUploadController` construye los resultados de carga y las respuestas de consulta del almacenamiento:

| Endpoint | Resultado procesado | Estado HTTP | Cuerpo |
|---|---|---:|---|
| `POST /api/files/upload` | Archivo almacenado | `200 OK` | Objeto con `fileName`, `fileSize` y `message`. |
| `POST /api/files/upload` | Error durante almacenamiento | `500 Internal Server Error` | Objeto con campo `error`. |
| `POST /api/files/upload-multiple` | Procesamiento del conjunto de archivos | `200 OK` | Lista de objetos; cada elemento contiene datos de éxito o campo `error`. |
| `GET /api/files/download/**` | Archivo localizado | `200 OK` | Recurso descargable. |
| `GET /api/files/download/**` | Archivo no localizado | `404 Not Found` | Sin cuerpo. |
| `GET /api/files/download/**` | Error durante consulta | `500 Internal Server Error` | Sin cuerpo. |
| `GET /api/files/list` o `/list/{subDir}` | Listado disponible | `200 OK` | Lista de nombres de archivo. |
| `GET /api/files/list` o `/list/{subDir}` | Directorio no localizado | `404 Not Found` | Sin cuerpo. |
| `GET /api/files/list` o `/list/{subDir}` | Error durante consulta | `500 Internal Server Error` | Sin cuerpo. |
| `GET /api/files/directories` | Consulta procesada | `200 OK` | Lista de directorios. |
| `GET /api/files/directories` | Error durante consulta | `500 Internal Server Error` | Sin cuerpo. |

El contrato completo de carga, descarga y listado se encuentra en `doc/api/archivos.md`.

### Importación de estudiantes: `POST /api/estudiantes/importar`

`EstudianteController.importar(...)` construye la respuesta de la importación multipart:

| Resultado procesado | Estado HTTP | Cuerpo |
|---|---:|---|
| Archivo procesado, con filas exitosas o rechazadas | `200 OK` | `ImportacionEstudiantesDTO` con `totalFilas`, `exitosos`, `fallidos` y `errores`. |
| Argumento de importación inválido | `400 Bad Request` | Texto con el mensaje procesado. |
| Error de ejecución procesado por el endpoint | `500 Internal Server Error` | Texto iniciado por `Error interno: `. |

El contrato de importación se encuentra en `doc/api/perfiles.md`.

## Códigos HTTP descritos en los contratos implementados

| Código | Uso documentado |
|---|---|
| `200 OK` | Consulta o acción exitosa con cuerpo; también resultado compuesto de importación o carga múltiple. |
| `201 Created` | Creación exitosa cuando el controller lo declara. |
| `204 No Content` | Acción exitosa sin cuerpo. |
| `400 Bad Request` | Regla de negocio, validación o solicitud inválida; texto de entrada inválida en importación de estudiantes. |
| `401 Unauthorized` | Sesión inexistente o autenticación no establecida para una ruta protegida. |
| `403 Forbidden` | Autenticación sin autoridad o alcance suficiente. |
| `404 Not Found` | Archivo o directorio no localizado en operaciones de consulta y descarga de `/api/files`. |
| `405 Method Not Allowed` | Método HTTP no soportado procesado por el manejador global. |
| `500 Internal Server Error` | Error general procesado por el manejador global o respuesta específica de un endpoint que la construye. |

## Manejo frontend

El frontend contiene utilidades en `src/lib/api.js` para interpretar cuerpos JSON, texto o respuestas vacías:

| Utilidad | Comportamiento |
|---|---|
| `readResponseBody(response)` | Retorna `null` para `204` o cuerpo vacío; parsea JSON o conserva texto plano. |
| `getApiErrorMessages(payload)` | Extrae detalles desde `detalles`, `details`, `errors`, `fieldErrors` y `validaciones`. |
| `getApiErrorTitle(payload, fallback)` | Obtiene mensaje principal desde `mensaje`, `message`, `descripcion` o `error`; también acepta texto plano. |
| `getApiErrorDescription(payload, fallback)` | Construye descripción desde detalles o mensaje disponible. |

`useApiForm` realiza manejo dedicado de `401` y `403`. Para otras respuestas no exitosas utiliza el payload procesado por estas utilidades.

## Endpoints por módulo

Los contratos detallados se documentan en `doc/api/`. La relación general de controllers observados es:

| Módulo | Prefijo API |
|---|---|
| Autenticación | `/api/auth` |
| Usuarios | `/api/usuarios-sistema` |
| Roles | `/api/roles` |
| Permisos | `/api/permisos` |
| Auditoría | `/api/audit` |
| Archivos | `/api/files` |
| Catálogos | `/api/areas`, `/api/temas`, `/api/tipos`, `/api/sedes`, entre otros |
| Personas | `/api/personas`, `/api/empresas`, `/api/condiciones`, entre otros |
| Perfiles | `/api/administrativos`, `/api/asesores`, `/api/estudiantes`, `/api/monitores`, `/api/conciliadores` |
| Consultas | `/api/consultas` |
| Procesos | `/api/procesos`, `/api/organos-control`, `/api/especialidades` |
| Seguimientos | `/api/seguimientos` |
| Conciliaciones | `/api/conciliaciones` |
| Estadísticas | `/api/estadisticas` |

## Archivos y multipart

Las operaciones de archivos y algunos flujos documentales usan `multipart/form-data`, por ejemplo:

- carga general de archivos;
- creación de conciliación con solicitud;
- finalización de conciliación con acta;
- reemplazo de solicitud;
- importación de estudiantes desde archivo;
- respuestas de seguimiento con archivos cuando aplica.

Los formatos construidos directamente por archivos e importación se describen en los contratos API correspondientes.
