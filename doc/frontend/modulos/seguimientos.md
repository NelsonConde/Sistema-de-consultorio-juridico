# Frontend - Módulo de seguimientos y tareas

## 1. Propósito del módulo

El módulo de seguimientos se presenta en el frontend como sección de **Tareas**. Permite gestionar tareas asociadas a consultas jurídicas, responderlas desde perfil estudiante, revisar respuestas y controlar notificaciones.

La ruta principal es:

| Ruta | Componente de página | Formulario principal |
|---|---|---|
| `/tareas` | `src/app/(dashboard)/tareas/page.js` | `SeguimientosForm` |

El formulario principal es:

```text
frontend/src/components/forms/consulta/SeguimientosForm.jsx
```

## 2. Archivos fuente validados

```text
frontend/src/app/(dashboard)/tareas/page.js
frontend/src/components/forms/consulta/SeguimientosForm.jsx
frontend/src/lib/config.js
frontend/src/lib/authz.js
frontend/src/lib/permission.js
frontend/src/components/forms/parts/FormFileUpload.jsx
frontend/src/components/ui/ConfirmActionDialog.jsx
frontend/src/components/ui/Pagination.jsx
```

## 3. Modelo de interfaz

`SeguimientosForm` gestiona el flujo de tareas desde una sola vista. La interfaz carga el usuario actual, determina permisos, carga consultas permitidas, carga categorías de seguimiento cuando corresponde y muestra seguimientos asociados a la consulta seleccionada.

El formulario diferencia capacidades según permisos:

- ver tareas;
- crear tareas;
- editar tareas;
- eliminar tareas;
- responder tareas;
- revisar respuestas;
- ver alertas disciplinarias;
- gestionar categorías de seguimiento.

## 4. Permisos usados

El componente evalúa permisos desde el objeto de `/api/auth/me`. En el código existen funciones locales como:

```text
puedeAccederTareasUsuario
puedeVerConsultasUsuario
puedeCargarCategoriasUsuario
puedeCrearTarea
puedeEditarTarea
puedeEliminarTarea
puedeResponderTarea
puedeRevisarRespuestas
puedeVerAlertasDisciplinarias
```

Permisos relevantes:

| Permiso | Uso |
|---|---|
| `Acceder tareas` | Permite ingresar a la ruta. |
| `Ver seguimientos` | Permite ver tareas. |
| `Crear seguimientos` | Permite crear tareas. |
| `Editar seguimientos` | Permite modificar tareas pendientes. |
| `Eliminar seguimientos` | Permite eliminar lógicamente tareas. |
| `Responder seguimientos` | Permite al estudiante responder. |
| `Aprobar respuestas de seguimiento` | Permite revisar respuestas. |
| `Ver alertas disciplinarias` | Permite visualizar alertas marcadas. |
| `Gestionar categorías de seguimiento` | Permite cargar categorías activas para gestión. |

## 5. Carga inicial

Al montar la vista, el formulario consulta:

```text
GET /api/auth/me
```

Luego, según permisos, carga:

```text
GET /api/seguimientos/categorias/activas
GET /api/consultas
GET /api/seguimientos/respuestas/pendientes
```

El listado de respuestas pendientes se usa para usuarios con permisos de revisión.

## 6. Consulta seleccionada

El usuario selecciona una consulta para visualizar sus tareas. Según el tipo de usuario y permisos, el frontend puede cargar seguimientos mediante endpoints como:

```text
GET /api/seguimientos/consulta/{consultaId}
GET /api/seguimientos/consulta/{consultaId}/visibles-estudiante
```

La variante visible para estudiante se usa para mostrar únicamente tareas marcadas para estudiante cuando corresponde.

La interfaz distingue si la consulta permite operación. Si la consulta está cerrada o archivada, se muestra aviso de visualización histórica y se deshabilitan acciones operativas como crear, editar, responder, eliminar o cambiar estado.

## 7. Creación y edición de tareas

La creación de un seguimiento usa:

```text
POST /api/seguimientos
```

La edición usa:

```text
PUT /api/seguimientos/{id}
```

Campos gestionados por la interfaz:

```text
consultaId
categoriaSeguimientoId
descripcion
fechaEntrega
diasNotificacion
notificarPartes
notificarEstudiante
alertaDisciplinaria
```

El frontend valida datos básicos y respeta permisos antes de mostrar o enviar formularios.

## 8. Regla de notificación a estudiante

El campo `notificarEstudiante` determina si la tarea será visible para el estudiante y si se generan comunicaciones orientadas a ese destinatario. El código actual del backend exige que la consulta tenga estudiante asignado y activo cuando `notificarEstudiante=true`.

En frontend, esta opción se documenta como una configuración operativa de visibilidad y notificación. Si backend rechaza la operación por regla de negocio, el mensaje se muestra al usuario mediante `toast.error`.

## 9. Alertas disciplinarias

El formulario contiene soporte para marcar tareas como alerta disciplinaria. La visualización de esta información depende del permiso `Ver alertas disciplinarias`.

## 10. Respuestas de seguimiento

Los estudiantes pueden responder seguimientos cuando tienen permiso y la tarea está en condiciones operativas.

Las respuestas se gestionan con endpoints bajo:

```text
/api/seguimientos/{seguimientoId}/respuestas
```

La interfaz permite:

- listar respuestas de una tarea;
- crear una respuesta;
- adjuntar archivos a la respuesta cuando el flujo lo permite;
- visualizar la última respuesta;
- determinar si puede responder nuevamente según estado de la respuesta anterior.

## 11. Revisión de respuestas

Los usuarios con permiso de revisión pueden aprobar o rechazar respuestas. La decisión se envía al backend y la interfaz actualiza el estado de la tarea o respuesta según la operación.

Cuando una respuesta se rechaza, el backend exige observación de revisión. La interfaz presenta los campos necesarios para que el revisor indique la observación antes de enviar la decisión.

## 12. Cambio de estado de tarea

El cambio de estado usa:

```text
PATCH /api/seguimientos/{id}/estado?estado={estado}
```

La acción está controlada por permisos y por estado operativo de la consulta.

## 13. Eliminación lógica

La eliminación de seguimiento se realiza mediante endpoint de eliminación del backend y se protege con confirmación. Al eliminar, el backend conserva trazabilidad y cancela notificaciones pendientes según sus reglas internas.

## 14. Notificaciones

El frontend configura condiciones que activan notificaciones:

- `notificarPartes`;
- `notificarEstudiante`;
- `fechaEntrega`;
- `diasNotificacion`.

El cálculo, creación y envío de notificaciones se realiza en backend. El frontend solo captura los parámetros y muestra el resultado de la operación.

## 15. Archivos de respuestas

El módulo usa `FILE_STORAGE_API_URL_BASE` o `API_URL_BASE` como base para operaciones de archivo según la función interna. Los archivos se asocian al flujo de respuesta cuando se suben como soporte.

## 16. Manejo de errores

El componente implementa una función de solicitud (`apiRequest`) que:

- envía `credentials: "include"`;
- lee respuestas JSON o texto;
- detecta errores HTTP;
- redirige al login en caso de sesión inválida;
- muestra mensajes de error mediante `toast.error`.

## 17. Relación con backend

| Regla backend | Reflejo en frontend |
|---|---|
| Consulta cerrada o archivada bloquea operación. | La UI muestra aviso histórico y deshabilita acciones. |
| Tarea pendiente puede editarse. | La UI muestra acciones según estado y permisos. |
| Estudiante solo ve tareas notificadas para estudiante. | Se usa endpoint visible para estudiante. |
| Respuesta rechazada requiere observación. | La interfaz ofrece campo de observación al revisar. |
| Notificaciones se calculan en backend. | El frontend captura flags y fechas. |

## 18. Consideraciones de mantenimiento

Al modificar este módulo debe verificarse:

1. Que los permisos locales coincidan con `PERMISOS` y backend.
2. Que los endpoints de respuestas se mantengan alineados con controllers.
3. Que la regla de consulta cerrada o archivada siga bloqueando operación.
4. Que el campo `notificarEstudiante` siga coordinado con backend.
5. Que el formulario no prometa envío de notificaciones desde frontend; esa responsabilidad es del backend.
6. Que el manejo de archivos conserve la base configurada.


## Archivos de tareas y respuestas

La vista de seguimientos gestiona archivos de soporte para dos momentos del flujo:

- documentos de tarea: `tareas-{seguimientoId}-documentos`;
- documentos de respuesta: `tareas-{seguimientoId}-respuestas-{respuestaId}`.

Ambos flujos usan los endpoints genéricos de archivos para carga múltiple, listado y descarga.
