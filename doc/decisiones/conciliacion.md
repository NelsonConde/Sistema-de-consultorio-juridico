# Decisión técnica - Módulo de conciliación

## Contexto

El módulo de conciliación administra un flujo que nace desde una consulta jurídica y permite gestionar responsables, reuniones, documentos PDF, notificaciones y cierre funcional de conciliación.

La decisión principal es mantener la conciliación como módulo propio, pero dependiente del contexto jurídico de la consulta. Esto evita duplicar información de personas, conserva trazabilidad y permite validar cierre de consulta cuando existe actividad conciliatoria pendiente.

---

## Decisiones principales

1. La conciliación nace desde una consulta.
2. La consulta es fuente de consultante, partes y contrapartes.
3. Los estados de conciliación se administran en tabla y se validan por código técnico.
4. La solicitud PDF es obligatoria al crear conciliación.
5. El acta PDF es obligatoria para finalizar.
6. La asignación automática usa disponibilidad y carga no finalizada.
7. La reunión vigente se administra en entidad propia uno a uno.
8. La programación y reprogramación generan historial.
9. La reprogramación reemplaza notificaciones pendientes anteriores.
10. Finalizar o desactivar cancela notificaciones pendientes.
11. `activo` no reemplaza estado funcional.
12. La desactivación lógica no equivale a finalización.

---

## Conciliación desde consulta

La conciliación se crea desde:

```text
POST /api/conciliaciones/consulta/{consultaId}
```

Esta decisión permite:

- conservar contexto jurídico;
- usar consultante, partes y contrapartes desde la consulta;
- validar alcance del usuario desde la consulta;
- impedir conciliaciones sobre consultas cerradas o archivadas;
- bloquear más de una conciliación activa no finalizada por consulta.

---

## Estado de conciliación en tabla

La entidad `EstadoConciliacion` permite separar código técnico y nombre visible.

Códigos técnicos:

```text
EN_ESPERA
ESPERANDO_REUNION
REUNION_PROGRAMADA
COMPLETO_CONCILIADO
COMPLETO_NO_CONCILIADO
```

El backend normaliza los códigos recibidos y valida reglas por código, no por nombre visible.

---

## Separación entre estado y activo

`estado` representa la etapa funcional de la conciliación.

`activo` representa si el registro participa en la operación actual.

Una conciliación finalizada conserva `activo=true` y estado final. Una conciliación desactivada queda `activo=false` sin representar finalización conciliatoria.

---

## Creación y asignación automática

Al crear conciliación, el sistema selecciona estudiante y conciliador cuando hay disponibilidad.

La selección de estudiante usa este orden:

1. estudiante de la consulta, si está activo y habilitado para conciliación;
2. estudiante activo habilitado con menor carga de conciliaciones no finalizadas.

La selección de conciliador usa conciliadores activos ordenados por menor carga de conciliaciones no finalizadas.

Esta decisión permite iniciar el trámite aunque algún responsable no esté disponible. En ese caso, el estado queda `EN_ESPERA`.

---

## Estados calculados por responsables

Después de asignaciones, el estado se recalcula:

- si falta estudiante o conciliador: `EN_ESPERA`;
- si existen ambos y no hay reunión: `ESPERANDO_REUNION`;
- si ya existe reunión: `REUNION_PROGRAMADA`.

Por eso `EN_ESPERA` no se asigna manualmente por endpoint de cambio de estado.

---

## Reunión uno a uno

`ReunionConciliacion` usa el id de la conciliación como id propio mediante relación uno a uno.

Esta decisión significa que:

- una conciliación tiene una única reunión vigente;
- programar crea la reunión;
- reprogramar actualiza esa reunión;
- los cambios se conservan en historial.

---

## Historial de reunión

El historial permite registrar eventos sin crear múltiples reuniones vigentes.

Eventos soportados:

- `PROGRAMACION`;
- `REPROGRAMACION`.

Cada registro conserva valores anteriores y nuevos cuando aplica, sede, observaciones y usuario que realizó la acción.

---

## Documentos PDF

La solicitud y el acta son documentos de soporte.

| Documento | Momento | Ruta lógica |
|---|---|---|
| Solicitud | Creación o reemplazo | `conciliacion/{id}/solicitud.pdf` |
| Acta | Finalización | `conciliacion/{id}/acta.pdf` |

El backend valida extensión `.pdf` y tipo de contenido `application/pdf` cuando el cliente lo informa.

---

## Finalización con acta

La finalización usa:

```text
POST /api/conciliaciones/{id}/finalizar
```

La decisión de usar un endpoint específico evita que estados finales se asignen sin soporte documental.

Al finalizar:

- se guarda el acta;
- se asigna estado final;
- se registra `fechaFinalizacion`;
- se cancelan notificaciones pendientes.

---

## Desactivación lógica

La desactivación usa:

```text
DELETE /api/conciliaciones/{id}
```

Se mantiene como salida administrativa para conciliaciones activas no finalizadas. No equivale a finalización, no genera acta y no registra fecha de finalización.

---

## Notificaciones de reunión

La programación y reprogramación generan notificaciones para destinatarios derivados de la consulta:

- consultante;
- partes;
- contrapartes.

Los correos se normalizan y se deduplican. Cuando no hay destinatarios con correo, se genera alerta administrativa.

Se generan dos momentos:

- inmediata;
- recordatorio un día antes de la reunión cuando la fecha queda en futuro.

El scheduler procesa notificaciones pendientes con cron configurable. El valor por defecto corre cada hora en el minuto cero.

---

## Relación con cierre de consulta

La consulta no puede cerrarse si existen conciliaciones activas no finalizadas.

Estados que bloquean cierre:

- `EN_ESPERA`;
- `ESPERANDO_REUNION`;
- `REUNION_PROGRAMADA`.

Estados finales no bloquean el cierre de consulta.
