# Inventario de API

Esta carpeta documenta los endpoints REST expuestos por el backend del sistema de gestión de casos jurídicos.

La documentación está orientada al consumo desde frontend, pruebas funcionales e integración entre módulos. Cada documento describe rutas, métodos HTTP, permisos, datos de entrada, respuestas esperadas y reglas relevantes aplicadas por backend.

## Base general

```text
/api
```

El frontend consume esta base mediante la configuración centralizada en `API_URL_BASE`.

## Autenticación y sesión

Los endpoints protegidos requieren cookie de sesión válida. El frontend envía solicitudes protegidas usando:

```javascript
credentials: "include"
```

La autenticación se documenta en:

```text
api/autenticacion.md
```

## Convenciones de contenido

| Tipo | Uso |
|---|---|
| JSON | Endpoints de consulta, creación, actualización y cambio de estado. |
| `multipart/form-data` | Endpoints con carga de documentos o archivos. |
| PDF | Descarga de reportes o documentos almacenados cuando aplica. |

## Documentos de API

| Documento | Módulo |
|---|---|
| `autenticacion.md` | Login, sesión, usuario actual, logout y recuperación/restablecimiento de contraseña. |
| `usuarios-roles-permisos.md` | Usuarios del sistema, roles, permisos, asignación de permisos y cambio de perfil. |
| `perfiles.md` | Administrativos, asesores, monitores, estudiantes y conciliadores. |
| `personas.md` | Personas naturales, empresas y búsqueda de personas activas. |
| `catalogos.md` | Tipos de documento, sedes, áreas, temas, tipos, órganos de control y especialidades. |
| `consultas.md` | Consultas jurídicas, responsables, cambio de estado, archivo y desarchivo. |
| `procesos.md` | Procesos asociados a consulta, radicado, estados y catálogos procesales. |
| `seguimientos.md` | Seguimientos, respuestas, revisión, categorías y notificaciones. |
| `conciliaciones.md` | Conciliaciones, responsables, reuniones, actas, estados y documentos. |
| `estadisticas.md` | Estadísticas por semestre, rango libre, perfil y reportes PDF. |
| `archivos.md` | Carga, descarga, listado y gestión de archivos. |
| `auditoria.md` | Consulta de registros de auditoría. |

## Permisos

La API combina permisos funcionales con reglas de alcance. La presencia de un permiso habilita una acción general, pero el backend puede aplicar controles adicionales según el perfil activo, la consulta relacionada, los responsables asignados o el estado funcional del recurso.

### Representación documental de permisos

`PermisoNombre` centraliza permisos como constantes Java cuyo valor es el nombre funcional utilizado por Spring Security y expuesto por los DTOs.

| Forma | Ejemplo | Uso en la documentación |
|---|---|---|
| Constante Java | `VER_CONSULTAS` | Referencia técnica empleada por controllers y anotaciones de autorización. |
| Valor textual del permiso | `"Ver consultas"` | Autoridad evaluada y valor retornado en campos como `UsuarioSistemaDTO.permisos` y `PermisoDTO.nombre`. |

Las tablas de autorización pueden citar constantes Java para corresponder con el código fuente. Los ejemplos de respuestas API que incluyen permisos utilizan el valor textual expuesto por los mappers.

Ejemplos:

| Módulo | Control aplicado |
|---|---|
| Consultas | Permiso funcional y alcance por responsable o perfil. |
| Procesos | Alcance heredado desde la consulta asociada. |
| Seguimientos | Alcance según consulta, autor, estudiante y permisos de revisión. |
| Conciliaciones | Alcance por conciliador, estudiante asignado y permisos administrativos. |
| Estadísticas | Permisos `VER_REPORTES` y `VER_CONSULTAS` según endpoint. |
| Auditoría | Permiso administrativo de consulta de auditoría. |

## Respuestas y errores

Los errores procesados por los manejadores globales y de seguridad utilizan `ErrorResponseDTO`. Algunos endpoints construyen cuerpos específicos de su operación, entre ellos las cargas y consultas de `/api/files` y la importación de estudiantes en `POST /api/estudiantes/importar`.

Las reglas generales y los contratos de respuesta específicos se describen en:

```text
../05-estandar-api-errores.md
```

Estados HTTP documentados en los contratos implementados:

| Estado | Uso |
|---|---|
| `200 OK` | Consulta o acción exitosa con cuerpo; también resultado compuesto de carga múltiple o importación. |
| `201 Created` | Creación exitosa cuando el controller lo define. |
| `204 No Content` | Operación exitosa sin cuerpo de respuesta. |
| `400 Bad Request` | Validación de negocio o datos inválidos; texto procesado en importación cuando corresponde. |
| `401 Unauthorized` | Sesión ausente o autenticación no establecida para una ruta protegida. |
| `403 Forbidden` | Usuario autenticado sin permiso o alcance suficiente. |
| `404 Not Found` | Archivo o directorio no localizado en operaciones documentadas de `/api/files`. |
| `405 Method Not Allowed` | Método HTTP no soportado procesado por el manejador global. |
| `500 Internal Server Error` | Error procesado por el manejador global o por un endpoint con contrato específico. |

## Relación con frontend

Los documentos de API se complementan con:

```text
frontend/configuracion-api.md
frontend/servicios-api.md
frontend/manejo-errores.md
frontend/modulos/*.md
```

El frontend no sustituye las validaciones del backend; únicamente organiza formularios, navegación, consumo de API y retroalimentación visual al usuario.
