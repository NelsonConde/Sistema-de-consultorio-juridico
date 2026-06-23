# API - Seguimientos, respuestas y categorías

## Alcance

Este documento describe los endpoints implementados por los controllers del módulo de seguimientos. La documentación se basa en el código fuente actual y cubre tres grupos de operaciones:

- seguimientos asociados a consultas jurídicas;
- respuestas del estudiante a seguimientos visibles;
- categorías de seguimiento usadas para clasificar tareas.

Los endpoints principales se exponen bajo:

```text
/api/seguimientos
```

Controllers relacionados:

| Controller | Responsabilidad |
|---|---|
| `SeguimientoController` | Consulta, creación, edición, cambio de estado y eliminación lógica de seguimientos. |
| `SeguimientoRespuestaController` | Creación, edición, consulta y revisión de respuestas del estudiante. |
| `CategoriaSeguimientoController` | Administración y consulta de categorías de seguimiento. |

## Consideraciones generales

Los listados operativos de seguimientos y respuestas trabajan con registros activos y excluyen consultas archivadas. Las consultas archivadas representan un flujo histórico, por lo que el módulo evita exponerlas en operaciones activas de tareas, respuestas y revisión.

Las operaciones de escritura también validan que la consulta asociada permita operación operativa. Una consulta cerrada o archivada bloquea creación, edición, respuesta, cambio de estado y eliminación lógica de seguimientos.

## Estados de seguimiento

El enum `EstadoSeguimiento` contiene:

| Estado | Uso |
|---|---|
| `PENDIENTE` | Estado inicial de todo seguimiento nuevo. Permite edición, respuesta y notificaciones activas. |
| `COMPLETADO` | Seguimiento cumplido. Cancela notificaciones pendientes. |
| `CANCELADO` | Seguimiento cancelado. Cancela notificaciones pendientes. |

## Estados de respuesta

El enum `EstadoRespuestaSeguimiento` contiene:

| Estado | Uso |
|---|---|
| `PENDIENTE` | Respuesta enviada y pendiente de revisión. |
| `APROBADA` | Respuesta aceptada por usuario autorizado. Completa automáticamente el seguimiento asociado. |
| `RECHAZADA` | Respuesta rechazada con observación de revisión. Permite nuevo intento del estudiante. |

---

# DTOs

## `SeguimientoRequestDTO`

Se usa para crear y actualizar seguimientos.

| Campo | Tipo | Regla |
|---|---|---|
| `id` | `Long` | No debe enviarse en creación. Si viene en actualización, debe coincidir con el id de la ruta. |
| `descripcion` | `String` | Obligatoria. Máximo 200 caracteres. Se normaliza antes de guardar. |
| `fechaEntrega` | `LocalDate` | Opcional. No puede ser anterior a la fecha actual. |
| `diasNotificacion` | `Integer` | Opcional. No puede ser negativo. Si se informa, debe existir `fechaEntrega`. |
| `notificarPartes` | `Boolean` | Opcional. Si llega `null`, se trata como `false`. |
| `notificarEstudiante` | `Boolean` | Opcional. Si llega `true`, la consulta debe tener estudiante asignado y activo. Si llega `null`, se trata como `false`. |
| `alertaDisciplinaria` | `Boolean` | Opcional. Si llega `null`, se trata como `false`. |
| `categoriaSeguimientoId` | `Long` | Obligatorio. Debe corresponder a una categoría activa. |
| `consultaId` | `Long` | Obligatorio. Define el contexto operativo del seguimiento. |

La actualización usa `PUT`, por lo que el cuerpo esperado corresponde al DTO editable del seguimiento y no a una actualización parcial tipo `PATCH`.

## `SeguimientoResponseDTO`

Se usa como salida de seguimientos.

| Campo | Descripción |
|---|---|
| `id` | Identificador del seguimiento. |
| `descripcion` | Descripción normalizada. |
| `fechaEntrega` | Fecha límite configurada. |
| `diasNotificacion` | Días previos de recordatorio. |
| `notificarPartes` | Indica si se notifican persona principal, partes y contrapartes. |
| `notificarEstudiante` | Indica si el seguimiento es visible/notificable para estudiante. |
| `alertaDisciplinaria` | Indica alerta disciplinaria. |
| `estado` | Estado funcional del seguimiento. |
| `categoriaSeguimientoId` | Categoría asociada. |
| `categoriaSeguimientoNombre` | Nombre de categoría. |
| `consultaId` | Consulta asociada. |
| `autorId` | Usuario del sistema que creó el seguimiento. |
| `autorUsername` | Usuario del autor. |
| `fechaCreacion` | Fecha de creación. |
| `fechaActualizacion` | Fecha de última actualización. |

## `SeguimientoRespuestaRequestDTO`

Se usa para crear o editar una respuesta del estudiante.

| Campo | Tipo | Regla |
|---|---|---|
| `id` | `Long` | No debe enviarse en creación. Si viene en actualización, debe coincidir con el id de la ruta. |
| `contenido` | `String` | Obligatorio. Máximo 1000 caracteres. |

## `SeguimientoRespuestaDecisionDTO`

Se usa para aprobar o rechazar una respuesta.

| Campo | Tipo | Regla |
|---|---|---|
| `estado` | `EstadoRespuestaSeguimiento` | Obligatorio. Solo se admite `APROBADA` o `RECHAZADA`. |
| `observacionRevision` | `String` | Máximo 500 caracteres. Obligatoria cuando la decisión es `RECHAZADA`. Opcional para `APROBADA` en backend. |

## `SeguimientoRespuestaResponseDTO`

| Campo | Descripción |
|---|---|
| `id` | Identificador de la respuesta. |
| `seguimientoId` | Seguimiento respondido. |
| `consultaId` | Consulta asociada al seguimiento. |
| `estudianteId` | Estudiante que respondió. |
| `estudianteNombre` | Nombre del estudiante. |
| `contenido` | Contenido de la respuesta. |
| `estado` | Estado de revisión. |
| `fueraPlazo` | Indica si la respuesta fue enviada o editada después de la fecha de entrega. |
| `observacionRevision` | Observación de revisión. |
| `revisadoPorId` | Usuario que revisó. |
| `revisadoPorUsername` | Usuario del revisor. |
| `activo` | Marca lógica de actividad. |
| `fechaCreacion` | Fecha de creación. |
| `fechaActualizacion` | Fecha de actualización. |
| `fechaDecision` | Fecha en que se aprobó o rechazó. |

## `CategoriaSeguimientoDTO`

| Campo | Tipo | Regla |
|---|---|---|
| `id` | `Long` | No debe enviarse en creación. En edición debe coincidir con ruta. |
| `nombre` | `String` | Obligatorio. Máximo 50 caracteres. Debe ser único ignorando mayúsculas/minúsculas. |
| `activo` | `Boolean` | Si llega `null` en creación, el servicio asume categoría activa. |

---

# Endpoints de seguimientos

## Listar seguimientos por consulta

```text
GET /api/seguimientos/consulta/{consultaId}
```

Permiso: `VER_SEGUIMIENTOS`.

Retorna seguimientos activos asociados a una consulta no archivada, respetando alcance del usuario.

Reglas de acceso:

- El estudiante no usa este endpoint general. Si intenta usarlo, el backend indica que debe consultar solo seguimientos visibles.
- Asesor, monitor y administrador pueden usarlo según permisos y alcance sobre la consulta.
- El conciliador no tiene alcance operativo sobre seguimientos en el flujo actual.

## Listar seguimientos visibles para estudiante

```text
GET /api/seguimientos/consulta/{consultaId}/visibles-estudiante
```

Permiso: `VER_SEGUIMIENTOS`.

Retorna seguimientos activos de una consulta no archivada con `notificarEstudiante=true`, siempre respetando alcance. Es el endpoint que debe usar el perfil estudiante para consultar las tareas visibles para él.

## Listar seguimientos por autor

```text
GET /api/seguimientos/autor/{autorId}
```

Permiso: `VER_SEGUIMIENTOS`.

Retorna seguimientos activos creados por el autor indicado y asociados a consultas no archivadas.

Regla de acceso:

- Administrador puede consultar por cualquier autor.
- Los demás usuarios solo pueden consultar seguimientos creados por su propio usuario del sistema.

## Listar alertas disciplinarias

```text
GET /api/seguimientos/alertas-disciplinarias
```

Permiso: `VER_ALERTAS_DISCIPLINARIAS`.

Retorna seguimientos activos marcados con `alertaDisciplinaria=true` y asociados a consultas no archivadas. Es un listado funcional habilitado por permiso específico.

## Listar por fecha de entrega

```text
GET /api/seguimientos/fecha-entrega?fechaEntrega=YYYY-MM-DD
```

Permiso: `VER_SEGUIMIENTOS`.

El parámetro `fechaEntrega` es obligatorio y usa formato ISO de fecha. El resultado se filtra por alcance del usuario y excluye consultas archivadas.

## Obtener seguimiento por id

```text
GET /api/seguimientos/{id}
```

Permiso: `VER_SEGUIMIENTOS`.

Retorna un seguimiento activo, siempre que la consulta asociada no esté archivada y el usuario tenga alcance sobre el registro. El estudiante solo puede verlo si `notificarEstudiante=true`.

## Crear seguimiento

```text
POST /api/seguimientos
```

Permiso: `CREAR_SEGUIMIENTOS`.

Cuerpo: `SeguimientoRequestDTO`.

Ejemplo:

```json
{
  "descripcion": "Aportar documento de soporte",
  "fechaEntrega": "2026-06-15",
  "diasNotificacion": 3,
  "notificarPartes": false,
  "notificarEstudiante": true,
  "alertaDisciplinaria": false,
  "categoriaSeguimientoId": 1,
  "consultaId": 20
}
```

Reglas:

- El seguimiento nace en `PENDIENTE`.
- El seguimiento nace con `activo=true`.
- El autor se toma del usuario autenticado.
- La consulta debe existir y permitir operación operativa.
- La categoría debe estar activa.
- Estudiante y conciliador no crean seguimientos.
- Si `notificarEstudiante=true`, la consulta debe tener estudiante asignado y activo.
- Booleanos nulos se tratan como `false`.
- Las notificaciones se sincronizan después de guardar.

## Actualizar seguimiento

```text
PUT /api/seguimientos/{id}
```

Permiso: `EDITAR_SEGUIMIENTOS`.

Cuerpo: `SeguimientoRequestDTO`.

Reglas:

- Solo se editan seguimientos en estado `PENDIENTE`.
- La consulta asociada no se puede cambiar.
- La consulta debe permitir operación operativa.
- Deben existir cambios reales.
- Estudiante y conciliador no modifican seguimientos.
- Asesor y monitor solo modifican seguimientos creados por ellos dentro de su alcance.
- La actualización recalcula efectos de notificación según el estado resultante.

## Cambiar estado de seguimiento

```text
PATCH /api/seguimientos/{id}/estado?estado=COMPLETADO
```

Permiso: `EDITAR_SEGUIMIENTOS`.

Valores aceptados:

```text
PENDIENTE
COMPLETADO
CANCELADO
```

Reglas de transición:

- No se permite cambiar a un estado igual al actual.
- Para cambiar a `COMPLETADO`, el seguimiento no puede tener respuestas pendientes.
- Si el seguimiento tiene `notificarEstudiante=true`, para cambiar a `COMPLETADO` debe existir una respuesta aprobada.
- Para volver a `PENDIENTE`, no debe existir respuesta aprobada.
- Al quedar `PENDIENTE`, el backend sincroniza notificaciones.
- Al quedar `COMPLETADO` o `CANCELADO`, el backend cancela notificaciones pendientes.

## Eliminar seguimiento

```text
DELETE /api/seguimientos/{id}
```

Permiso: `ELIMINAR_SEGUIMIENTOS`.

La eliminación es lógica. El seguimiento queda inactivo y antes de desactivarse se cancelan notificaciones pendientes. Las notificaciones ya enviadas se conservan como historial.

---

# Endpoints de respuestas

## Crear respuesta

```text
POST /api/seguimientos/{seguimientoId}/respuestas
```

Permiso: `RESPONDER_SEGUIMIENTOS`.

Cuerpo: `SeguimientoRespuestaRequestDTO`.

Reglas:

- Solo el perfil estudiante puede responder.
- El seguimiento debe estar activo, pendiente y asociado a consulta operativa.
- El seguimiento debe tener `notificarEstudiante=true`.
- El estudiante debe tener alcance sobre la consulta.
- La respuesta nace `PENDIENTE` y `activo=true`.
- Si la respuesta se envía después de `fechaEntrega`, queda marcada con `fueraPlazo=true`.
- Si ya existe una respuesta pendiente del estudiante para ese seguimiento, no se permite nuevo intento.
- Si ya existe una respuesta aprobada, no se permiten nuevos intentos.
- Si la última respuesta activa fue rechazada, se permite un nuevo intento.

## Actualizar respuesta

```text
PUT /api/seguimientos/respuestas/{id}
```

Permiso: `RESPONDER_SEGUIMIENTOS`.

Reglas:

- Solo el estudiante autor de la respuesta puede editarla.
- Solo se editan respuestas `PENDIENTE`.
- El seguimiento debe seguir pendiente y visible para estudiante.
- La consulta debe permitir operación operativa.
- Deben existir cambios reales.
- Si la edición se realiza fuera de plazo, la respuesta queda o permanece marcada con `fueraPlazo=true`.

## Obtener respuesta por id

```text
GET /api/seguimientos/respuestas/{id}
```

Permiso: `VER_SEGUIMIENTOS`.

Retorna una respuesta activa cuyo seguimiento esté activo y cuya consulta no esté archivada. El estudiante solo puede ver sus propias respuestas de seguimientos visibles.

## Listar respuestas por seguimiento

```text
GET /api/seguimientos/{seguimientoId}/respuestas
```

Permiso: `VER_SEGUIMIENTOS`.

Lista respuestas activas de un seguimiento activo y de consulta no archivada, respetando alcance. El estudiante solo ve respuestas propias si el seguimiento es visible para él.

## Listar respuestas pendientes

```text
GET /api/seguimientos/respuestas/pendientes
```

Permiso: `APROBAR_RESPUESTAS_SEGUIMIENTO`.

Retorna respuestas pendientes de revisión dentro del alcance del usuario. Estudiante y conciliador no pueden revisar respuestas.

## Aprobar o rechazar respuesta

```text
PATCH /api/seguimientos/respuestas/{id}/decision
```

Permiso: `APROBAR_RESPUESTAS_SEGUIMIENTO`.

Cuerpo: `SeguimientoRespuestaDecisionDTO`.

Ejemplo de aprobación:

```json
{
  "estado": "APROBADA",
  "observacionRevision": "Respuesta revisada correctamente."
}
```

Ejemplo de rechazo:

```json
{
  "estado": "RECHAZADA",
  "observacionRevision": "Debe complementar la respuesta con los documentos solicitados."
}
```

Reglas:

- Solo se aceptan decisiones `APROBADA` o `RECHAZADA`.
- Solo se revisan respuestas `PENDIENTE`.
- `RECHAZADA` exige observación de revisión.
- La observación no puede superar 500 caracteres.
- La aprobación puede tener observación opcional en backend.
- Al aprobar, el backend completa automáticamente el seguimiento asociado y cancela notificaciones pendientes.

---

# Endpoints de categorías de seguimiento

## Listar categorías administrativas

```text
GET /api/seguimientos/categorias
```

Permiso: `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

Lista todas las categorías, incluidas activas e inactivas.

## Listar categorías activas

```text
GET /api/seguimientos/categorias/activas
```

Permisos aceptados: `VER_SEGUIMIENTOS`, `CREAR_SEGUIMIENTOS` o `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

Lista categorías activas ordenadas por nombre para uso en formularios de seguimiento.

## Obtener categoría por id

```text
GET /api/seguimientos/categorias/{id}
```

Permiso: `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

## Crear categoría

```text
POST /api/seguimientos/categorias
```

Permiso: `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

Reglas:

- `nombre` obligatorio.
- Máximo 50 caracteres.
- Nombre único ignorando mayúsculas/minúsculas.
- La categoría se crea activa.

## Actualizar categoría

```text
PUT /api/seguimientos/categorias/{id}
```

Permiso: `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

Actualiza el nombre sin cambiar el estado activo. Para activar o desactivar se usa el endpoint de estado.

## Cambiar estado activo de categoría

```text
PATCH /api/seguimientos/categorias/{id}/activo?activo=true
```

Permiso: `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

## Eliminar categoría

```text
DELETE /api/seguimientos/categorias/{id}
```

Permiso: `GESTIONAR_CATEGORIAS_SEGUIMIENTO`.

La eliminación se maneja como desactivación lógica para conservar trazabilidad de seguimientos ya creados.

---

# Errores funcionales frecuentes

| Caso | Resultado |
|---|---|
| Fecha de entrega anterior a la actual | Error de negocio. |
| Días de notificación negativos | Error de negocio. |
| Días de notificación sin fecha de entrega | Error de negocio. |
| `notificarEstudiante=true` sin estudiante activo | Error de negocio. |
| Estudiante intentando usar endpoint general por consulta | Acceso denegado; debe usar visibles para estudiante. |
| Editar seguimiento no pendiente | Error de negocio. |
| Completar seguimiento visible a estudiante sin respuesta aprobada | Error de negocio. |
| Completar seguimiento con respuesta pendiente | Error de negocio. |
| Reabrir seguimiento con respuesta aprobada | Error de negocio. |
| Nueva respuesta con respuesta pendiente previa | Error de negocio. |
| Nueva respuesta después de respuesta aprobada | Error de negocio. |
| Rechazar respuesta sin observación | Error de negocio. |
| Decisión `PENDIENTE` | Error de negocio. |
