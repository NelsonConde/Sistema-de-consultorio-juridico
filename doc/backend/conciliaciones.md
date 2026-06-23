# Backend - Módulo de conciliaciones y reuniones

## Propósito

El módulo de conciliaciones administra el flujo conciliatorio que nace desde una consulta jurídica. Permite crear conciliaciones con solicitud PDF, asignar estudiante y conciliador, programar y reprogramar reuniones, registrar historial operativo, notificar destinatarios, finalizar con acta PDF y desactivar registros cuando el flujo administrativo lo requiere.

La implementación se apoya en reglas de negocio, permisos, alcance por perfil, almacenamiento documental y control de estados técnicos.

---

## Fuentes de código principales

| Capa | Archivos relevantes |
|---|---|
| Controller | `ConciliacionController` |
| Fachada | `ConciliacionService` |
| Escritura de conciliación | `ConciliacionCommandService` |
| Lectura de conciliación | `ConciliacionQueryService` |
| Validación | `ConciliacionValidator`, `ReunionConciliacionValidator` |
| Acceso y alcance | `ConciliacionAccessService`, `ConciliacionAlcanceService` |
| Asignación | `ConciliacionAsignacionService` |
| Relaciones | `ConciliacionRelacionService`, `ReunionConciliacionRelacionService` |
| Documentos | `ConciliacionDocumentoService` |
| Reuniones | `ReunionConciliacionCommandService`, `ReunionConciliacionService`, `ReunionConciliacionMapper` |
| Historial | `ReunionConciliacionHistorialService` |
| Notificaciones | `ReunionConciliacionNotificacionService`, `ReunionConciliacionRecordatorioService`, `ReunionConciliacionDestinatarioService`, `ReunionConciliacionEnvioNotificacionService`, `ReunionConciliacionNotificacionEstadoService` |
| Scheduler | `ReunionConciliacionNotificacionScheduler` |
| Entidades | `Conciliacion`, `EstadoConciliacion`, `ReunionConciliacion`, `ReunionConciliacionHistorial`, `ReunionConciliacionNotificacion` |
| Repositorios | `ConciliacionRepository`, `EstadoConciliacionRepository`, `ReunionConciliacionRepository`, `ReunionConciliacionHistorialRepository`, `ReunionConciliacionNotificacionRepository` |

---

## Modelo funcional

La conciliación se relaciona con:

- una consulta origen;
- un estudiante asignado a la conciliación;
- un conciliador asignado;
- un estado funcional administrado en `estado_conciliacion`;
- un usuario solicitante;
- una solicitud PDF;
- un acta PDF cuando finaliza;
- una reunión vigente opcional;
- historial de programación y reprogramación;
- notificaciones de reunión.

La conciliación no duplica personas. El detalle toma consultante, partes y contrapartes desde la consulta origen.

---

## Estados técnicos

| Estado | Descripción |
|---|---|
| `EN_ESPERA` | Conciliación activa sin responsables mínimos completos. |
| `ESPERANDO_REUNION` | Conciliación con estudiante y conciliador, lista para reunión. |
| `REUNION_PROGRAMADA` | Conciliación con reunión vigente. |
| `COMPLETO_CONCILIADO` | Finalizada con acuerdo conciliado. |
| `COMPLETO_NO_CONCILIADO` | Finalizada sin acuerdo. |

`ConciliacionValidator` diferencia estados finalizados y no finalizados mediante códigos técnicos. El backend normaliza códigos recibidos antes de resolver el estado activo.

---

## Creación desde consulta

`ConciliacionCommandService.crearDesdeConsulta` crea la conciliación desde una consulta existente y una solicitud PDF.

Flujo implementado:

1. Valida permiso y alcance para crear conciliación.
2. Obtiene la consulta origen.
3. Valida que la consulta no esté cerrada ni archivada.
4. Valida que no exista una conciliación activa no finalizada para la misma consulta.
5. Selecciona estudiante para conciliación.
6. Selecciona conciliador disponible.
7. Registra el usuario solicitante.
8. Crea la conciliación activa.
9. Calcula estado inicial según responsables.
10. Guarda la conciliación para obtener id.
11. Guarda la solicitud PDF en ruta dependiente del id.
12. Devuelve `ConciliacionResponseDTO`.

El documento se guarda después del primer guardado porque la ruta lógica depende del identificador de conciliación.

---

## Asignación automática

`ConciliacionAsignacionService` selecciona responsables al crear la conciliación.

### Estudiante

El backend intenta usar el estudiante de la consulta cuando:

- existe estudiante asociado a la consulta;
- está activo;
- tiene habilitada la bandera `conciliacion=true`.

Si no se puede usar ese estudiante, selecciona un estudiante activo habilitado para conciliación con menor carga de conciliaciones activas no finalizadas.

### Conciliador

El conciliador se selecciona entre conciliadores activos, priorizando menor carga de conciliaciones activas no finalizadas.

Este comportamiento permite iniciar conciliaciones aun cuando falte algún responsable. En ese caso el estado queda `EN_ESPERA`.

---

## Asignación manual de responsables

### Estudiante

La asignación manual de estudiante valida:

- permiso funcional;
- alcance administrativo o conciliador asignado;
- conciliación activa;
- conciliación no finalizada;
- consulta operativa;
- estudiante activo;
- estudiante habilitado para conciliación.

Después de asignar estudiante, el estado se recalcula. Si ya existe reunión, se conserva `REUNION_PROGRAMADA`; si no existe reunión, el estado depende de responsables mínimos.

### Conciliador

La asignación manual de conciliador está restringida al alcance administrativo. El conciliador debe estar activo. También recalcula el estado con la misma regla de conservación de reunión programada.

---

## Cambio de estado no final

`ConciliacionCommandService.cambiarEstado` permite cambiar estados no finalizados dentro del flujo operativo.

Reglas aplicadas por `ConciliacionValidator`:

- conciliación activa y no finalizada;
- consulta asociada operativa;
- estado destino obligatorio;
- estado destino activo en catálogo;
- no se permite estado final;
- `EN_ESPERA` no se asigna manualmente;
- no se permite repetir estado;
- `ESPERANDO_REUNION` exige estudiante y conciliador;
- `REUNION_PROGRAMADA` exige estudiante, conciliador y reunión registrada.

Los estados finales se gestionan exclusivamente por finalización con acta.

---

## Finalización con acta

`ConciliacionCommandService.finalizar` cierra funcionalmente la conciliación.

Validaciones:

- permiso y alcance para finalizar;
- conciliación activa;
- conciliación no finalizada;
- consulta no cerrada ni archivada;
- estado final permitido;
- estudiante y conciliador asignados;
- acta PDF obligatoria y válida.

Efectos:

- guarda el acta en `conciliacion/{id}/acta.pdf`;
- asigna estado final;
- registra `fechaFinalizacion`;
- cancela notificaciones pendientes de reunión;
- conserva notificaciones enviadas como historial.

---

## Reemplazo de solicitud

`ConciliacionCommandService.reemplazarSolicitud` permite reemplazar la solicitud PDF.

Reglas:

- permiso `GESTIONAR_CONCILIACIONES`;
- alcance administrativo;
- conciliación activa no finalizada;
- consulta operativa;
- archivo PDF válido.

La ruta lógica se mantiene como `conciliacion/{id}/solicitud.pdf`.

---

## Desactivación lógica

`ConciliacionCommandService.desactivar` realiza salida administrativa del registro activo.

Reglas:

- permiso `GESTIONAR_CONCILIACIONES`;
- alcance administrativo;
- conciliación activa;
- conciliación no finalizada;
- consulta operativa.

Efectos:

- marca `activo=false`;
- cancela notificaciones pendientes;
- no asigna estado final;
- no registra acta;
- no registra `fechaFinalizacion`.

La desactivación no equivale a finalización conciliatoria.

---

## Reunión de conciliación

`ReunionConciliacion` representa la reunión vigente de una conciliación.

Características técnicas:

- relación uno a uno con `Conciliacion`;
- usa `conciliacion_id` como identificador;
- una conciliación solo tiene una reunión vigente;
- la reprogramación actualiza la misma reunión;
- el historial conserva eventos de programación y reprogramación.

### Programación

Al programar se valida:

- permiso `PROGRAMAR_REUNIONES_CONCILIACION`;
- alcance administrador o conciliador asignado;
- conciliación activa y no finalizada;
- consulta operativa;
- estudiante y conciliador asignados;
- fecha futura;
- sede activa;
- inexistencia de reunión previa.

Efectos:

- crea reunión;
- asegura estado `REUNION_PROGRAMADA`;
- registra historial `PROGRAMACION`;
- crea notificaciones inmediatas y recordatorios.

### Reprogramación

Al reprogramar se valida:

- permiso `REPROGRAMAR_REUNIONES_CONCILIACION`;
- alcance administrador o conciliador asignado;
- reunión existente;
- fecha futura;
- sede activa;
- cambio real en fecha, sede u observaciones.

Efectos:

- actualiza reunión vigente;
- asegura estado `REUNION_PROGRAMADA`;
- registra historial `REPROGRAMACION`;
- cancela notificaciones pendientes anteriores;
- crea nuevas notificaciones y recordatorios.

---

## Historial de reunión

`ReunionConciliacionHistorialService` registra:

- programación inicial;
- reprogramación.

El historial conserva:

- tipo de evento;
- fecha anterior y nueva;
- sede anterior y nueva;
- observaciones anteriores y nuevas;
- usuario que ejecutó el evento;
- fecha del evento.

La programación y la reprogramación registran su historial en backend mediante `ReunionConciliacionHistorialService`.

---

## Notificaciones de reunión

`ReunionConciliacionNotificacionService` administra notificaciones asociadas a programación y reprogramación.

### Destinatarios

Los destinatarios se obtienen desde la consulta:

- consultante;
- partes;
- contrapartes.

Los correos se normalizan y se deduplican por email. Los tipos técnicos de destinatario son:

- `CONSULTANTE`;
- `PARTE`;
- `CONTRAPARTE`;
- `ADMINISTRATIVO`.

`ADMINISTRATIVO` se usa para alertas administrativas cuando no hay destinatarios notificables o cuando se registran errores operativos.

### Momentos

| Momento | Uso |
|---|---|
| `INMEDIATA` | Al programar o reprogramar. |
| `RECORDATORIO` | Un día antes de la reunión, si esa fecha aún está en el futuro. |

### Cancelación

Se cancelan notificaciones pendientes cuando:

- se reprograma la reunión;
- se finaliza la conciliación;
- se desactiva la conciliación.

Las notificaciones enviadas permanecen como historial.

### Scheduler

`ReunionConciliacionNotificacionScheduler` procesa pendientes con cron configurable. El valor por defecto es:

```text
0 0 * * * *
```

Este valor ejecuta el procesamiento en el minuto cero de cada hora.

---

## Documentos PDF

`ConciliacionDocumentoService` valida y guarda:

| Documento | Momento | Ruta lógica |
|---|---|---|
| Solicitud | Creación o reemplazo | `conciliacion/{id}/solicitud.pdf` |
| Acta | Finalización | `conciliacion/{id}/acta.pdf` |

Validaciones:

- archivo obligatorio;
- extensión `.pdf`;
- `contentType=application/pdf` cuando viene informado.

---

## Relación con cierre de consulta

La consulta no puede cerrarse si tiene conciliaciones activas en estados no finalizados:

- `EN_ESPERA`;
- `ESPERANDO_REUNION`;
- `REUNION_PROGRAMADA`.

Las conciliaciones finalizadas no bloquean el cierre de consulta. Las conciliaciones inactivas tampoco bloquean porque las validaciones consideran conciliaciones activas.

---

## Estadísticas relacionadas

`ConciliacionRepository` aporta agregaciones por periodo y estado. También contiene conteos de carga no finalizada usados para seleccionar estudiante y conciliador durante la asignación automática.
