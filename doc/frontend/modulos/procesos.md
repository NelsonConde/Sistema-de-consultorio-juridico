# Frontend - Módulo de procesos

## 1. Propósito del módulo

El módulo frontend de procesos permite registrar, listar, editar, cambiar estado funcional y desactivar lógicamente procesos asociados a consultas jurídicas. En la implementación actual se compone de dos vistas principales:

| Ruta | Componente de página | Componente principal |
|---|---|---|
| `/nuevoproceso` | `src/app/(dashboard)/nuevoproceso/page.js` | `NuevoProcesoForm` |
| `/procesos` | `src/app/(dashboard)/procesos/page.js` | `ProcesosForm` |

El módulo consume endpoints del backend bajo `API_URL_BASE` y usa la sesión obtenida desde `/api/auth/me` para validar permisos y mostrar acciones disponibles.

## 2. Archivos fuente validados

```text
frontend/src/app/(dashboard)/nuevoproceso/page.js
frontend/src/app/(dashboard)/procesos/page.js
frontend/src/components/forms/procesos/NuevoProcesosForm.jsx
frontend/src/components/forms/procesos/ProcesosForm.jsx
frontend/src/components/ui/ConfirmActionDialog.jsx
frontend/src/lib/config.js
frontend/src/lib/authz.js
frontend/src/lib/permission.js
```

## 3. Permisos usados en la interfaz

Los componentes evalúan permisos con `tieneAlgunPermiso` y constantes de `PERMISOS`.

| Permiso | Uso en frontend |
|---|---|
| `ACCEDER_PROCESOS` | Permite acceso visual a rutas del módulo. |
| `VER_PROCESOS` | Permite cargar el listado de procesos. |
| `GESTIONAR_PROCESOS` | Habilita creación, edición, cambio de estado y eliminación lógica. |
| `VER_CONSULTAS` | Permite cargar consultas para asociar procesos. |
| `VER_CATALOGOS` / `GESTIONAR_CATALOGOS` | Permite cargar departamentos, órganos de control y especialidades. |

El backend aplica validaciones adicionales de permiso y alcance; por eso las validaciones frontend son complementarias y no sustituyen la seguridad del servidor.

## 4. Vista de creación: `/nuevoproceso`

`NuevoProcesoForm` permite registrar un proceso nuevo asociado a una consulta.

### Carga inicial

La vista consulta:

```text
GET /api/auth/me
GET /api/departamentos
GET /api/organos-control
GET /api/especialidades
GET /api/consultas
```

Los catálogos y consultas se cargan según permisos. Si la sesión vence o el usuario no tiene autorización, el componente muestra mensajes de error y puede redirigir.

### Campos administrados

```text
numeroRadicado
departamentoId
consultaId
organoControlId
especialidadId
```

El payload enviado a backend normaliza campos vacíos como `null` para relaciones opcionales y para el radicado.

### Creación sin radicado

La interfaz permite crear un proceso sin radicado. Esta regla corresponde al backend, donde todo proceso nuevo inicia como `PENDIENTE`.

Si el usuario informa número de radicado, la interfaz valida longitud de 23 caracteres antes de enviar.

### Consulta asociada

La consulta se selecciona mediante un modal de búsqueda. La consulta seleccionada se envía como `consultaId`.

### Órgano y especialidad

La especialidad depende del órgano de control. El frontend filtra especialidades por `organoControlId` cuando hay órgano seleccionado. Si se selecciona especialidad sin órgano, se muestra error antes de enviar.

### Envío

```text
POST /api/procesos
```

## 5. Vista de administración: `/procesos`

`ProcesosForm` permite consultar y gestionar procesos existentes.

### Carga de datos

La vista consulta:

```text
GET /api/auth/me
GET /api/procesos
GET /api/departamentos
GET /api/organos-control
GET /api/especialidades
GET /api/consultas
```

Los procesos se ordenan por id ascendente en frontend. Los catálogos auxiliares permiten mostrar nombres legibles de departamento, órgano, especialidad y consulta.

### Listado

La tabla muestra:

- id;
- radicado;
- departamento;
- consulta;
- órgano;
- especialidad;
- estado;
- acciones cuando el usuario puede gestionar.

La vista tiene búsqueda textual por radicado, departamento, órgano o consulta, paginación local y botón de actualización.

### Edición

La edición usa:

```text
PUT /api/procesos/{id}
```

El formulario de edición permite modificar:

- número de radicado;
- departamento;
- órgano de control;
- especialidad.

La consulta aparece como campo bloqueado. Esto coincide con el backend, que no permite cambiar `consultaId` desde la edición del proceso.

La edición no cambia el estado funcional ni la marca `activo`.

### Cambio de estado funcional

El cambio de estado usa:

```text
PATCH /api/procesos/{id}/estado?estado={estado}
```

Antes de enviar un estado final, el frontend valida que el proceso tenga radicado y que el radicado tenga 23 caracteres. Esta validación acompaña la regla del backend.

Estados finales manejados por la interfaz:

```text
SENTENCIA_FAVORABLE
SENTENCIA_DESFAVORABLE
DESISTIMIENTO
RECHAZO
PRESCRIPCION
```

### Desactivación lógica

La vista usa:

```text
DELETE /api/procesos/{id}
```

La acción se muestra solo para usuarios con permiso de gestión y usa `ConfirmActionDialog` antes de ejecutar.

La acción visible de ciclo de vida en esta pantalla es la desactivación lógica mediante `DELETE /api/procesos/{id}`.

## 6. Relación con backend

| Regla backend | Reflejo frontend |
|---|---|
| Proceso nuevo inicia en `PENDIENTE`. | La creación no solicita estado funcional. |
| Radicado opcional mientras está pendiente. | El campo radicado puede quedar vacío en creación. |
| Radicado obligatorio para estados finales. | La UI bloquea visualmente el cambio a estado final sin radicado. |
| Consulta no cambia en edición. | El campo consulta está deshabilitado en el modal de edición. |
| Especialidad depende de órgano. | La UI filtra especialidades y valida órgano requerido. |
| Gestión protegida por permiso. | Las acciones se muestran según `GESTIONAR_PROCESOS`. |
| Listado protegido por permiso. | La vista exige `VER_PROCESOS` para cargar procesos. |
| Desactivación lógica del proceso. | La UI llama `DELETE` con confirmación. |

## 7. Manejo de errores

El módulo implementa utilidades locales `apiGet` y `apiEnviar` para:

- leer respuestas JSON o texto;
- manejar `401` como sesión vencida;
- manejar `403` como falta de permisos;
- mostrar mensajes con `toast.error`;
- redirigir al login cuando corresponde.

Mensajes visibles del módulo incluyen:

- no tienes permiso para acceder a procesos;
- no tienes permiso para ver procesos;
- no tienes permiso para crear procesos;
- el número de radicado debe tener exactamente 23 caracteres;
- antes de finalizar el proceso debes registrar y guardar un número de radicado;
- selecciona un departamento;
- selecciona una consulta;
- selecciona primero un órgano de control.

## 8. Componentes relacionados

| Componente | Función |
|---|---|
| `NuevoProcesoForm` | Creación de proceso. |
| `ProcesosForm` | Listado, edición, cambio de estado y desactivación lógica. |
| `ConfirmActionDialog` | Confirmación de eliminación. |

## 9. Consideraciones de mantenimiento

Al modificar el módulo frontend de procesos debe verificarse:

1. que `ESTADOS_FINALES` coincida con `EstadoProceso` del backend;
2. que la longitud de radicado siga siendo 23 caracteres;
3. que `PUT /api/procesos/{id}` no se use para cambiar estado;
4. que `PATCH /api/procesos/{id}/estado` siga usando query param `estado`;
5. que la desactivación visible en la pantalla siga usando `DELETE`;
6. que las acciones visibles del ciclo de vida correspondan a los controles implementados en la interfaz;
7. que el permiso efectivo de carga de procesos sea compatible con `VER_PROCESOS`.
