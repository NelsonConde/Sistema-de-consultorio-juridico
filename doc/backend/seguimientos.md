# Backend - Módulo de seguimientos, respuestas y notificaciones

## Propósito del módulo

El módulo de seguimientos administra tareas, requerimientos, controles académicos y alertas asociadas a una consulta jurídica. Su implementación vigente integra cuatro capacidades principales:

1. Gestión de seguimientos asociados a consultas.
2. Respuestas enviadas por estudiantes cuando el seguimiento es visible para ellos.
3. Revisión de respuestas por usuarios autorizados.
4. Notificaciones inmediatas y recordatorios programados.

El módulo conserva trazabilidad mediante estados funcionales y eliminación lógica. Las consultas cerradas o archivadas bloquean operaciones activas, y los listados operativos excluyen registros asociados a consultas archivadas.

## Fuentes de código del módulo

| Tipo | Archivos principales |
|---|---|
| Controllers | `SeguimientoController`, `SeguimientoRespuestaController`, `CategoriaSeguimientoController` |
| Servicios fachada | `SeguimientoService`, `SeguimientoRespuestaService`, `CategoriaSeguimientoService`, `SeguimientoNotificacionService` |
| Escritura | `SeguimientoCommandService`, `SeguimientoRespuestaCommandService` |
| Lectura | `SeguimientoQueryService`, `SeguimientoRespuestaQueryService` |
| Estado | `SeguimientoEstadoService` |
| Validadores | `SeguimientoValidator`, `SeguimientoRespuestaValidator`, `CategoriaSeguimientoValidator` |
| Acceso | `SeguimientoAccessService`, `SeguimientoRespuestaAccessService` |
| Notificaciones | `SeguimientoNotificacionService`, `SeguimientoRecordatorioService`, `SeguimientoNotificacionInmediataService`, `SeguimientoEnvioNotificacionService`, `SeguimientoNotificacionEstadoService`, `SeguimientoDestinatarioService` |
| Scheduler | `SeguimientoNotificacionScheduler` |
| DTOs | `SeguimientoRequestDTO`, `SeguimientoResponseDTO`, `SeguimientoRespuestaRequestDTO`, `SeguimientoRespuestaDecisionDTO`, `SeguimientoRespuestaResponseDTO`, `CategoriaSeguimientoDTO` |
| Entidades | `Seguimiento`, `SeguimientoRespuesta`, `SeguimientoNotificacion`, `CategoriaSeguimiento` |
| Estados | `EstadoSeguimiento`, `EstadoRespuestaSeguimiento`, `TipoNotificacionSeguimiento`, `MomentoNotificacionSeguimiento` |
| Repositorios | `SeguimientoRepository`, `SeguimientoRespuestaRepository`, `SeguimientoNotificacionRepository`, `CategoriaSeguimientoRepository` |
| Pruebas | `SeguimientoValidatorTest`, `SeguimientoRespuestaValidatorTest`, `ConsultaEstadoServiceTest` |

---

# Entidad `Seguimiento`

`Seguimiento` representa una tarea o control vinculado a una consulta.

| Campo | Descripción |
|---|---|
| `descripcion` | Texto obligatorio del seguimiento. Máximo 200 caracteres. |
| `fechaEntrega` | Fecha límite. Si es `null`, no se programa recordatorio. |
| `diasNotificacion` | Días antes de `fechaEntrega` para recordatorio. Requiere fecha de entrega. |
| `notificarPartes` | Habilita notificación a persona principal, partes y contrapartes. |
| `notificarEstudiante` | Habilita visibilidad y notificación para estudiante. |
| `alertaDisciplinaria` | Marca alerta disciplinaria y notifica administrativos cuando corresponde. |
| `estado` | Estado funcional: `PENDIENTE`, `COMPLETADO` o `CANCELADO`. |
| `activo` | Marca de eliminación lógica. |
| `categoriaSeguimiento` | Categoría funcional activa. |
| `consulta` | Consulta asociada. |
| `autor` | Usuario del sistema que creó el seguimiento. |
| `fechaCreacion` | Fecha de creación asignada automáticamente. |
| `fechaActualizacion` | Fecha actualizada automáticamente en modificación. |

Los valores booleanos y de estado se normalizan en la entidad para evitar nulos persistidos.

## Estados de seguimiento

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Estado inicial. Permite edición, respuesta y notificaciones activas. |
| `COMPLETADO` | Seguimiento cumplido. Cancela notificaciones pendientes. |
| `CANCELADO` | Seguimiento cancelado. Cancela notificaciones pendientes. |

`activo` no reemplaza al estado. El estado representa el flujo de la tarea; `activo` representa eliminación lógica.

---

# Creación de seguimiento

`SeguimientoCommandService.crear` aplica el siguiente flujo:

1. Valida DTO obligatorio y rechaza `id` en creación.
2. Valida permiso `CREAR_SEGUIMIENTOS`.
3. Bloquea creación para estudiante y conciliador.
4. Valida alcance sobre la consulta.
5. Normaliza descripción.
6. Valida fecha de entrega y días de notificación.
7. Carga categoría activa.
8. Carga consulta y valida que permita operación operativa.
9. Convierte booleanos `null` a `false`.
10. Valida que `notificarEstudiante=true` solo se use si la consulta tiene estudiante activo.
11. Asigna autor desde el usuario autenticado.
12. Crea el seguimiento en `PENDIENTE` y `activo=true`.
13. Guarda el seguimiento.
14. Sincroniza notificaciones después de guardar.

## Regla de estudiante activo

`SeguimientoValidator.validarNotificarEstudianteConConsulta` aplica:

- si `notificarEstudiante=false`, no exige estudiante;
- si `notificarEstudiante=true`, la consulta debe existir;
- si `notificarEstudiante=true`, la consulta debe tener estudiante asignado;
- si `notificarEstudiante=true`, el estudiante asignado debe estar activo.

Esta regla garantiza que una tarea visible para estudiante tenga destinatario real.

---

# Actualización de seguimiento

`SeguimientoCommandService.actualizar` permite modificar datos editables del seguimiento con `PUT`.

Reglas:

- requiere `EDITAR_SEGUIMIENTOS`;
- valida alcance mediante `SeguimientoAccessService`;
- solo se editan seguimientos `PENDIENTE`;
- la consulta asociada no se puede cambiar;
- la consulta debe permitir operación operativa;
- el DTO debe incluir los datos editables del seguimiento, no es un PATCH parcial;
- deben existir cambios reales;
- si se actualizan banderas o fechas, se recalculan efectos de notificación.

Para asesor y monitor, la modificación se limita a seguimientos creados por su propio usuario dentro de su alcance. Administrador puede modificar según permisos. Estudiante y conciliador no modifican seguimientos.

---

# Cambio de estado

`SeguimientoEstadoService` concentra las reglas de transición.

## Completar seguimiento

Para pasar a `COMPLETADO`:

- el seguimiento no puede tener respuestas pendientes;
- si `notificarEstudiante=true`, debe existir una respuesta aprobada;
- si se completa, se cancelan notificaciones pendientes.

## Cancelar seguimiento

Para pasar a `CANCELADO`:

- se valida estado diferente al actual;
- la consulta debe permitir operación operativa;
- se cancelan notificaciones pendientes.

## Reabrir como pendiente

Para volver a `PENDIENTE`:

- no puede existir respuesta aprobada;
- al quedar pendiente, se sincronizan notificaciones.

---

# Eliminación lógica

`DELETE /api/seguimientos/{id}` no elimina físicamente. El backend:

1. valida permiso `ELIMINAR_SEGUIMIENTOS`;
2. valida alcance;
3. valida consulta operativa;
4. cancela notificaciones pendientes;
5. marca el seguimiento como `activo=false`.

Las notificaciones ya enviadas permanecen como historial.

---

# Respuestas de seguimiento

`SeguimientoRespuesta` representa la respuesta de un estudiante a un seguimiento visible para él.

| Campo | Descripción |
|---|---|
| `seguimiento` | Seguimiento respondido. |
| `estudiante` | Estudiante que responde. |
| `contenido` | Texto obligatorio de respuesta. Máximo 1000 caracteres. |
| `estado` | `PENDIENTE`, `APROBADA` o `RECHAZADA`. |
| `fueraPlazo` | Marca si fue enviada o editada después de la fecha de entrega. |
| `observacionRevision` | Comentario de revisión. Máximo 500 caracteres. |
| `revisadoPor` | Usuario del sistema que aprueba o rechaza. |
| `fechaDecision` | Fecha de decisión. |
| `activo` | Marca de eliminación lógica. |

## Creación de respuesta

`SeguimientoRespuestaCommandService.crear` aplica:

- permiso `RESPONDER_SEGUIMIENTOS`;
- solo estudiante responde;
- seguimiento activo;
- seguimiento `PENDIENTE`;
- consulta operativa;
- `notificarEstudiante=true`;
- alcance del estudiante sobre la consulta;
- estudiante activo;
- contenido obligatorio y máximo 1000 caracteres;
- respuesta inicial `PENDIENTE`;
- marca `fueraPlazo=true` si se responde después de `fechaEntrega`.

## Intentos de respuesta

El backend revisa la última respuesta activa del estudiante para el seguimiento:

| Última respuesta | Resultado |
|---|---|
| `PENDIENTE` | No permite nuevo intento. Debe esperar revisión. |
| `APROBADA` | No permite nuevo intento. El seguimiento queda cumplido. |
| `RECHAZADA` | Permite nuevo intento. |

## Edición de respuesta

El estudiante puede editar su propia respuesta si:

- tiene permiso `RESPONDER_SEGUIMIENTOS`;
- la respuesta es suya;
- la respuesta está `PENDIENTE`;
- el seguimiento sigue visible para estudiante;
- el seguimiento está pendiente;
- la consulta permite operación operativa;
- existen cambios reales.

Si se edita fuera del plazo, `fueraPlazo` queda en `true`. Si ya estaba marcada fuera de plazo, se conserva.

---

# Revisión de respuestas

La revisión usa `SeguimientoRespuestaDecisionDTO`.

Reglas:

- requiere `APROBAR_RESPUESTAS_SEGUIMIENTO`;
- estudiante y conciliador no revisan respuestas;
- se valida alcance sobre la consulta;
- solo se revisan respuestas `PENDIENTE`;
- la decisión debe ser `APROBADA` o `RECHAZADA`;
- `RECHAZADA` exige observación;
- la observación no puede superar 500 caracteres;
- `APROBADA` puede incluir observación, pero el backend no la exige.

Cuando una respuesta se aprueba, `SeguimientoRespuestaCommandService` llama a `SeguimientoEstadoService.completarPorRespuestaAprobada`. Esto marca automáticamente el seguimiento como `COMPLETADO` y aplica los efectos de estado, incluyendo cancelación de notificaciones pendientes.

---

# Categorías de seguimiento

Las categorías clasifican tareas. `CategoriaSeguimientoService` permite:

- listar todas las categorías para administración;
- listar solo activas para formularios;
- crear categoría activa;
- actualizar nombre;
- activar o desactivar;
- eliminar lógicamente mediante desactivación.

Reglas:

- nombre obligatorio;
- máximo 50 caracteres;
- nombre único ignorando mayúsculas/minúsculas;
- actualizar nombre no cambia `activo`;
- `DELETE` desactiva la categoría para conservar trazabilidad.

---

# Notificaciones de seguimiento

El módulo crea notificaciones por seguimiento usando `SeguimientoNotificacion`.

## Tipos de notificación

`TipoNotificacionSeguimiento` define destinatarios funcionales:

| Tipo | Destinatarios |
|---|---|
| `PARTES` | Persona principal, partes y contrapartes de la consulta con correo válido. |
| `ESTUDIANTE` | Estudiante activo asignado a la consulta. |
| `ALERTA_DISCIPLINARIA` | Administrativos activos. |
| `AUTOR` | Autor del seguimiento. |

`SeguimientoDestinatarioService` depura correos vacíos, inválidos y repetidos.

## Momentos de notificación

`MomentoNotificacionSeguimiento` diferencia:

| Momento | Uso |
|---|---|
| `INMEDIATA` | Se crea y se intenta enviar al guardar/sincronizar si aplica. |
| `RECORDATORIO` | Se programa según `fechaEntrega - diasNotificacion`. |

## Notificaciones inmediatas

`SeguimientoNotificacionInmediataService` sincroniza notificaciones inmediatas para:

- `PARTES`, si `notificarPartes=true`;
- `ESTUDIANTE`, si `notificarEstudiante=true`;
- `ALERTA_DISCIPLINARIA`, si `alertaDisciplinaria=true`.

El autor no recibe notificación inmediata por defecto.

## Recordatorios

`SeguimientoRecordatorioService` solo programa recordatorios si existen `fechaEntrega` y `diasNotificacion`.

Destinatarios de recordatorio:

- `AUTOR`: siempre recibe recordatorio si hay fecha y días configurados;
- `PARTES`: si `notificarPartes=true`;
- `ESTUDIANTE`: si `notificarEstudiante=true`;
- `ALERTA_DISCIPLINARIA`: si `alertaDisciplinaria=true`.

## Restricción de unicidad

`SeguimientoNotificacion` tiene una restricción única por:

```text
seguimiento_id + tipo_notificacion + momento_notificacion
```

Así se evita duplicar notificaciones equivalentes para el mismo seguimiento.

## Scheduler

`SeguimientoNotificacionScheduler` ejecuta el procesamiento de pendientes con:

```text
app.seguimiento.notificaciones.cron=0 0 8 * * *
```

El servicio procesa notificaciones activas, no enviadas, cuya fecha programada sea menor o igual a la fecha actual.

---

# Alcance por perfil

| Perfil | Alcance real |
|---|---|
| Administrador | Puede ver y modificar según permisos. |
| Asesor | Puede ver según alcance de consulta. Para modificar, debe ser autor del seguimiento, salvo rol administrador. |
| Monitor | Puede ver según alcance de consulta. Para modificar, debe ser autor del seguimiento, salvo rol administrador. |
| Estudiante | Solo ve seguimientos visibles (`notificarEstudiante=true`) y puede responder si tiene permiso. No crea, edita ni elimina seguimientos. |
| Conciliador | No tiene alcance operativo sobre seguimientos en el flujo actual. |

---

# Relación con cierre de consulta

`ConsultaEstadoService` bloquea el cierre de una consulta si existen:

- seguimientos activos en `PENDIENTE`;
- respuestas activas en `PENDIENTE`;
- notificaciones activas no enviadas de seguimientos activos.

Esto evita cerrar una consulta mientras existan tareas, respuestas o comunicaciones pendientes.

---

# Pruebas relacionadas

| Prueba | Cobertura |
|---|---|
| `SeguimientoValidatorTest` | Regla de `notificarEstudiante=true` con estudiante activo. |
| `SeguimientoRespuestaValidatorTest` | Decisión válida, rechazo con observación y límite de observación. |
| `ConsultaEstadoServiceTest` | Bloqueo de cierre por seguimientos, respuestas y notificaciones pendientes. |
