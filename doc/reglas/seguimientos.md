# Reglas de negocio - Seguimientos y respuestas

## 1. Todo seguimiento nuevo inicia pendiente

El backend crea todo seguimiento nuevo con estado `PENDIENTE` y `activo=true`. El estado funcional se gestiona con `EstadoSeguimiento`, mientras que `activo` representa eliminación lógica.

## 2. La descripción es obligatoria

La descripción se normaliza, es obligatoria y no puede superar 200 caracteres.

## 3. Fecha de entrega válida

La fecha de entrega es opcional. Si se informa, no puede ser anterior a la fecha actual.

## 4. Días de notificación válidos

`diasNotificacion` no puede ser negativo. Si se informa, debe existir `fechaEntrega`. Sin fecha de entrega y días de notificación no se programan recordatorios.

## 5. Booleanos nulos se tratan como falsos

En creación y actualización, los campos `notificarPartes`, `notificarEstudiante` y `alertaDisciplinaria` se normalizan a `false` si llegan como `null`.

## 6. Notificación al estudiante exige estudiante activo

Si `notificarEstudiante=true`, la consulta debe tener estudiante asignado y activo. Esta regla garantiza que la tarea visible para estudiante tenga destinatario operativo real.

## 7. Estudiante usa endpoint de visibles

El estudiante no usa el endpoint general de seguimientos por consulta. Para este perfil, los seguimientos se consultan mediante el endpoint de visibles para estudiante y solo se muestran registros con `notificarEstudiante=true`.

## 8. El conciliador no opera seguimientos

El perfil conciliador no tiene alcance operativo sobre seguimientos en el flujo actual. La validación de acceso bloquea creación, modificación, revisión y visualización operativa de seguimientos para este perfil.

## 9. La consulta no cambia en edición

La consulta asociada define el contexto jurídico del seguimiento. En actualización no se permite cambiar `consultaId`.

## 10. Solo seguimientos pendientes son editables

El backend solo permite editar seguimientos en estado `PENDIENTE`.

## 11. La actualización de seguimiento no es parcial

`PUT /api/seguimientos/{id}` espera los datos editables del seguimiento. No se trata como actualización parcial tipo `PATCH`.

## 12. El seguimiento completado o cancelado cancela notificaciones pendientes

Cuando el seguimiento pasa a `COMPLETADO` o `CANCELADO`, el backend cancela notificaciones pendientes. Las notificaciones enviadas permanecen como historial.

## 13. Completar seguimiento con respuesta pendiente está bloqueado

No se puede completar manualmente un seguimiento si tiene respuestas pendientes.

## 14. Completar seguimiento visible para estudiante exige respuesta aprobada

Si `notificarEstudiante=true`, el seguimiento solo puede completarse si existe una respuesta aprobada.

## 15. Reabrir como pendiente está bloqueado si hay respuesta aprobada

No se puede devolver a `PENDIENTE` un seguimiento que ya tiene respuesta aprobada.

## 16. La respuesta se permite solo sobre seguimiento visible y pendiente

El estudiante solo puede responder si el seguimiento está activo, pendiente, visible para estudiante y asociado a una consulta operativa.

## 17. Intentos de respuesta

El backend evalúa la última respuesta activa del estudiante para el seguimiento:

| Última respuesta | Regla |
|---|---|
| `PENDIENTE` | No puede crear nuevo intento. |
| `APROBADA` | No puede crear nuevo intento. |
| `RECHAZADA` | Puede crear nuevo intento. |

## 18. Respuesta fuera de plazo

Si la respuesta se crea o edita después de `fechaEntrega`, se marca `fueraPlazo=true`. Esta marca no bloquea el flujo; deja evidencia para revisión.

## 19. Solo respuestas pendientes son editables

El estudiante puede editar únicamente su propia respuesta mientras esté `PENDIENTE`.

## 20. Revisión de respuestas

Solo usuarios con `APROBAR_RESPUESTAS_SEGUIMIENTO`, que no sean estudiante ni conciliador, pueden revisar respuestas dentro de su alcance.

## 21. Respuesta rechazada exige observación

Una respuesta `RECHAZADA` debe incluir observación de revisión. La observación permite trazabilidad académica y operativa.

## 22. Respuesta aprobada puede tener observación opcional

El backend permite aprobar sin observación. Si se envía observación, se conserva.

## 23. Aprobar respuesta completa el seguimiento

Cuando una respuesta es aprobada, el backend completa automáticamente el seguimiento asociado y aplica los efectos de estado, incluyendo cancelación de notificaciones pendientes.

## 24. Alertas disciplinarias tienen permiso específico

La consulta de alertas disciplinarias requiere `VER_ALERTAS_DISCIPLINARIAS`. Es un listado funcional por permiso específico de seguimientos marcados como alerta disciplinaria y asociados a consultas no archivadas.

## 25. Listados operativos excluyen consultas archivadas

Los listados de seguimientos y respuestas excluyen registros asociados a consultas archivadas. La consulta archivada se considera histórica y no operativa para este módulo.

## 26. Notificaciones inmediatas

El backend crea notificaciones inmediatas para partes, estudiante y alerta disciplinaria según las banderas del seguimiento. El autor no recibe notificación inmediata por defecto.

## 27. Recordatorios

Los recordatorios se programan solo cuando existen `fechaEntrega` y `diasNotificacion`. El autor recibe recordatorio si hay fecha y días configurados. Partes, estudiante y alerta disciplinaria reciben recordatorio según las banderas correspondientes.

## 28. Unicidad de notificación

No puede haber dos notificaciones equivalentes para el mismo seguimiento con igual tipo y momento. La entidad usa restricción única por seguimiento, tipo de notificación y momento.

## 29. Notificaciones pendientes bloquean cierre de consulta

Una consulta no puede cerrarse si existen notificaciones activas no enviadas asociadas a seguimientos activos.

## 30. Respuestas pendientes bloquean cierre de consulta

Una consulta no puede cerrarse si tiene respuestas de seguimiento pendientes.

## 31. Seguimientos pendientes bloquean cierre de consulta

Una consulta no puede cerrarse si tiene seguimientos activos en estado `PENDIENTE`.

## Reglas respaldadas por pruebas

Las pruebas unitarias relacionadas cubren:

- notificación al estudiante con consulta y estudiante activo;
- rechazo de notificación a estudiante sin estudiante asignado;
- rechazo de estudiante inactivo;
- observación obligatoria al rechazar respuesta;
- decisión inválida `PENDIENTE`;
- longitud máxima de observación;
- bloqueo de cierre de consulta por seguimientos, respuestas y notificaciones pendientes.
