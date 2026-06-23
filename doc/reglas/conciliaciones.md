# Reglas de negocio - Conciliaciones y reuniones

## Regla 1. La conciliación nace desde una consulta

Toda conciliación se crea asociada a una consulta existente. La consulta origen no puede estar cerrada ni archivada.

## Regla 2. Una consulta no debe tener más de una conciliación activa no finalizada

El backend bloquea la creación de una nueva conciliación cuando la consulta ya tiene una conciliación activa en un estado no finalizado.

Estados no finalizados:

- `EN_ESPERA`;
- `ESPERANDO_REUNION`;
- `REUNION_PROGRAMADA`.

## Regla 3. Los estados finalizados son estados de cierre

Estados finalizados:

- `COMPLETO_CONCILIADO`;
- `COMPLETO_NO_CONCILIADO`.

Estos estados solo se asignan mediante el endpoint de finalización con acta.

## Regla 4. `EN_ESPERA` se calcula automáticamente

El estado `EN_ESPERA` no se asigna manualmente. Se calcula cuando falta estudiante o conciliador asignado.

## Regla 5. `ESPERANDO_REUNION` exige responsables mínimos

Para quedar en `ESPERANDO_REUNION`, la conciliación debe tener estudiante y conciliador asignados.

## Regla 6. `REUNION_PROGRAMADA` exige reunión registrada

Para quedar en `REUNION_PROGRAMADA`, la conciliación debe tener estudiante, conciliador y una reunión registrada.

## Regla 7. La creación exige solicitud PDF

Toda conciliación creada desde consulta debe recibir una solicitud PDF válida. El archivo se almacena en:

```text
conciliacion/{id}/solicitud.pdf
```

## Regla 8. La finalización exige acta PDF

La conciliación solo puede finalizar con acta PDF válida. El archivo se almacena en:

```text
conciliacion/{id}/acta.pdf
```

## Regla 9. Los documentos deben ser PDF

El backend valida extensión `.pdf` y, cuando el tipo de contenido viene informado, exige `application/pdf`.

## Regla 10. La asignación automática usa disponibilidad y carga

Al crear conciliación:

1. Se intenta usar el estudiante de la consulta si está activo y habilitado para conciliación.
2. Si no aplica, se selecciona estudiante activo habilitado con menor carga de conciliaciones no finalizadas.
3. El conciliador se selecciona entre conciliadores activos con menor carga de conciliaciones no finalizadas.

## Regla 11. Estudiante asignado debe estar habilitado para conciliación

El estudiante asignado manual o automáticamente debe estar activo y tener `conciliacion=true`.

## Regla 12. Conciliador asignado debe estar activo

El conciliador asignado debe existir y estar activo.

## Regla 13. La asignación manual recalcula estado

Cuando se asigna estudiante o conciliador, el backend recalcula el estado según responsables. Si ya existe reunión registrada, conserva `REUNION_PROGRAMADA`.

## Regla 14. La asignación de conciliador es administrativa

La asignación manual de conciliador está restringida al alcance administrativo.

## Regla 15. La asignación de estudiante permite administrador o conciliador asignado

La asignación manual de estudiante puede realizarla un administrador o el conciliador asignado, siempre que tenga permiso funcional.

## Regla 16. Programar reunión exige conciliación operativa

Para programar reunión se exige:

- conciliación activa no finalizada;
- consulta asociada operativa;
- estudiante asignado;
- conciliador asignado;
- fecha futura;
- sede activa;
- inexistencia de reunión previa.

## Regla 17. La reunión es única por conciliación

`ReunionConciliacion` es una relación uno a uno con `Conciliacion` y usa el mismo id. Una conciliación tiene una reunión vigente. Los cambios posteriores se manejan mediante reprogramación.

## Regla 18. Reprogramar reunión exige cambio real

La reprogramación se rechaza cuando la nueva fecha, sede y observaciones son equivalentes a los valores actuales.

## Regla 19. Reprogramar cancela pendientes anteriores

Al reprogramar se desactivan las notificaciones pendientes anteriores y se crean nuevas notificaciones inmediatas y recordatorios.

## Regla 20. Finalizar cancela notificaciones pendientes

Al finalizar la conciliación, el backend cancela notificaciones pendientes de reunión. Las enviadas permanecen como historial.

## Regla 21. Desactivar cancela notificaciones pendientes

Al desactivar una conciliación activa no finalizada, se cancelan las notificaciones pendientes de reunión.

## Regla 22. Desactivar no equivale a finalizar

La desactivación lógica marca `activo=false`, pero no asigna estado final, no registra acta y no registra `fechaFinalizacion`.

## Regla 23. Notificaciones de reunión usan destinatarios de consulta

Las notificaciones de reunión se envían a destinatarios derivados de la consulta:

- consultante;
- partes;
- contrapartes.

El servicio de destinatarios obtiene estos receptores desde las personas vinculadas a la consulta.

## Regla 24. Los correos se deduplican

Los destinatarios se normalizan y se deduplican por correo electrónico para evitar mensajes repetidos.

## Regla 25. Sin destinatarios se registra alerta administrativa

Si no hay destinatarios con correo, el sistema crea una alerta administrativa asociada a la conciliación.

## Regla 26. Los recordatorios se programan un día antes

El backend crea recordatorios un día antes de la reunión cuando la fecha de recordatorio todavía queda en el futuro.

## Regla 27. El scheduler procesa pendientes

Las notificaciones pendientes se procesan mediante scheduler con cron configurable. El valor por defecto ejecuta el proceso cada hora en el minuto cero:

```text
0 0 * * * *
```

## Regla 28. Las conciliaciones pendientes bloquean cierre de consulta

Una consulta no puede cerrarse si tiene conciliaciones activas en estados:

- `EN_ESPERA`;
- `ESPERANDO_REUNION`;
- `REUNION_PROGRAMADA`.

## Regla 29. Las conciliaciones finalizadas no bloquean cierre de consulta

Los estados `COMPLETO_CONCILIADO` y `COMPLETO_NO_CONCILIADO` no se consideran pendientes para el cierre de la consulta.

## Regla 30. Las operaciones combinan permiso y alcance

Las operaciones del módulo no dependen solo del permiso funcional. El backend también valida alcance según perfil y relación con la conciliación o consulta.
