# Reglas de negocio - Procesos

## Propósito

Este documento resume las reglas de negocio implementadas en código para el módulo de procesos. Las reglas se derivan de `ProcesoCommandService`, `ProcesoQueryService`, `ProcesoValidator`, `ProcesoAccessService`, `ProcesoRepository`, `ProcesoDTO`, `Proceso` y `EstadoProceso`.

## Regla 1: el proceso pertenece a una consulta

Todo proceso debe estar asociado a una consulta existente.

La consulta asociada cumple tres funciones:

1. define el alcance del usuario sobre el proceso;
2. permite validar si se pueden hacer operaciones operativas;
3. conecta el proceso con el cierre de la consulta.

No se permite cambiar la consulta asociada durante la edición del proceso.

## Regla 2: creación en estado pendiente

Todo proceso creado desde la API inicia en:

```text
PENDIENTE
```

El backend fuerza ese estado en `ProcesoCommandService.crear`, sin depender de un valor enviado por el cliente.

## Regla 3: creación activa

Todo proceso creado inicia con:

```text
activo = true
```

La marca `activo` es una marca de eliminación lógica y no reemplaza al estado funcional.

## Regla 4: radicado condicional

El número de radicado tiene una regla condicional:

| Estado evaluado | Radicado |
|---|---|
| `PENDIENTE` | Puede ser nulo. |
| Estado final | Es obligatorio. |

Si el radicado se informa, debe tener exactamente 23 caracteres.

## Regla 5: estados finales

Son estados finales todos los estados distintos de `PENDIENTE`:

```text
SENTENCIA_FAVORABLE
SENTENCIA_DESFAVORABLE
DESISTIMIENTO
RECHAZO
PRESCRIPCION
```

Para pasar a cualquiera de esos estados, el proceso debe tener radicado válido previamente guardado.

## Regla 6: unicidad del radicado

Si se informa número de radicado:

- en creación, no puede existir otro proceso con el mismo radicado;
- en actualización, no puede existir otro proceso con el mismo radicado y diferente id.

## Regla 7: actualización general no cambia estado

`PUT /api/procesos/{id}` actualiza datos generales, pero no cambia:

- `estado`;
- `activo`.

El estado funcional se modifica con `PATCH /api/procesos/{id}/estado`. La marca activa se modifica con `PATCH /api/procesos/{id}/activo` o con `DELETE`.

## Regla 8: actualización general no cambia consulta

El proceso no puede moverse de una consulta a otra desde `PUT`. La consulta define el contexto y alcance del proceso.

## Regla 9: especialidad depende de órgano de control

La especialidad es opcional, pero si se informa:

1. debe existir activa;
2. debe seleccionarse órgano de control;
3. debe pertenecer al órgano de control seleccionado.

## Regla 10: operación solo sobre consulta operativa

Crear, actualizar, cambiar estado, cambiar activo o eliminar lógicamente un proceso requiere que la consulta asociada permita operación operativa.

Esto evita operar procesos de consultas cerradas o archivadas.

## Regla 11: listado operativo

El listado operativo de procesos:

- incluye solo procesos activos;
- excluye procesos asociados a consultas archivadas;
- aplica alcance registro por registro.

## Regla 12: detalle operativo

La consulta por id retorna procesos activos asociados a consultas con estado diferente de `ARCHIVADO` en el flujo operativo.

## Regla 13: permisos efectivos

| Acción | Permiso efectivo |
|---|---|
| Listar | `VER_PROCESOS` |
| Obtener detalle | `VER_PROCESOS` |
| Crear | `GESTIONAR_PROCESOS` |
| Actualizar | `GESTIONAR_PROCESOS` |
| Cambiar estado funcional | `GESTIONAR_PROCESOS` |
| Cambiar activo | `GESTIONAR_PROCESOS` |
| Eliminar lógicamente | `GESTIONAR_PROCESOS` |

La lectura efectiva exige `VER_PROCESOS` porque así lo valida `ProcesoAccessService`.

## Regla 14: alcance por perfil

| Perfil | Regla |
|---|---|
| Administrativo autorizado | Opera según permisos y alcance general. |
| Asesor | Accede si la consulta está dentro de su alcance. |
| Monitor | Accede si la consulta está asignada al monitor. |
| Estudiante | Puede consultar procesos de sus consultas si tiene permiso, pero no gestionarlos. |
| Conciliador | No tiene alcance operativo sobre procesos en esta fase. |

## Regla 15: desactivación lógica

`DELETE /api/procesos/{id}` realiza la desactivación lógica del proceso y establece `activo=false`, conservando el registro persistido.

Los procesos inactivos dejan de aparecer en listados operativos, pero permanecen disponibles como historial en base de datos.

## Regla 16: cambio de marca activa

`PATCH /api/procesos/{id}/activo?activo=` permite cambiar la marca activa del proceso. El nuevo valor debe ser diferente al actual y la consulta asociada debe permitir operación.

## Regla 17: incidencia sobre cierre de consulta

Una consulta no puede cerrarse si tiene procesos activos en estado `PENDIENTE`.

Cuando el proceso pasa a un estado final, deja de bloquear el cierre de consulta por la causal de proceso pendiente, sin afectar otras validaciones del cierre.

## Regla 18: estadísticas de proceso

Las agregaciones implementadas en `ProcesoRepository` se basan en:

- conteo por estado;
- conteo por estado filtrado por asesor;
- conteo por estado filtrado por estudiante;
- conteo por estado filtrado por monitor.

Estas son las agregaciones de procesos implementadas en el repository para el módulo y para los reportes que lo consumen.

## Regla 19: frontend y API

La interfaz de procesos acompaña las reglas del backend:

- permite crear proceso sin radicado;
- valida radicado de 23 caracteres si se informa;
- bloquea visualmente el cambio a estado final sin radicado;
- usa `DELETE` para la desactivación lógica visible del proceso.
