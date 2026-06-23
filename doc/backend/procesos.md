# Backend - Módulo de procesos

## Propósito del módulo

El módulo de procesos administra la información procesal asociada a una consulta jurídica. Permite registrar el trámite formal derivado de la consulta, controlar el estado funcional del proceso, conservar el número de radicado cuando existe y mantener trazabilidad mediante eliminación lógica.

En el código fuente actual, el proceso está modelado como una entidad dependiente de una consulta. Por esa razón, el alcance de lectura y gestión no se calcula de forma aislada: se hereda de la consulta asociada y se complementa con permisos específicos del módulo de procesos.

## Fuentes de código validadas

La documentación de este módulo se basa en el código actual de:

| Tipo | Archivos |
|---|---|
| Controller | `ProcesoController` |
| Fachada | `ProcesoService` |
| Escritura | `ProcesoCommandService` |
| Lectura | `ProcesoQueryService` |
| Validación | `ProcesoValidator` |
| Mapeo | `ProcesoMapper` |
| Acceso | `ProcesoAccessService` |
| DTO | `ProcesoDTO` |
| Entidad | `Proceso` |
| Enum | `EstadoProceso` |
| Repositorio | `ProcesoRepository` |
| Catálogos relacionados | `OrganoControlController`, `EspecialidadController`, `OrganoControlService`, `EspecialidadService` |
| Pruebas | `ProcesoValidatorTest` |

## Ubicación en la arquitectura

El módulo sigue la arquitectura por capas del backend:

1. `ProcesoController` expone los endpoints HTTP bajo `/api/procesos`.
2. `ProcesoService` actúa como fachada de casos de uso.
3. `ProcesoCommandService` concentra operaciones de escritura.
4. `ProcesoQueryService` concentra operaciones de lectura.
5. `ProcesoValidator` valida reglas propias del proceso.
6. `ProcesoAccessService` valida permisos y alcance.
7. `ProcesoMapper` transforma entidad y DTO.
8. `ProcesoRepository` maneja persistencia y agregaciones.

Esta separación permite que el controller permanezca delgado y que las reglas de negocio se concentren en servicios especializados.

## Entidad `Proceso`

La entidad `Proceso` se persiste en la tabla `proceso`.

| Campo | Tipo conceptual | Regla |
|---|---|---|
| `id` | Identificador | Generado por base de datos. |
| `numeroRadicado` | String | Único, longitud máxima definida en columna de 23. Puede ser nulo en `PENDIENTE`. |
| `departamento` | Relación obligatoria | Departamento activo requerido en creación y actualización. |
| `consulta` | Relación obligatoria | Consulta jurídica asociada. Define alcance. |
| `organoControl` | Relación opcional | Órgano de control asociado al proceso. |
| `especialidad` | Relación opcional | Especialidad asociada al órgano de control. |
| `estado` | Enum | Estado funcional del proceso. Por defecto `PENDIENTE`. |
| `activo` | Boolean | Marca de eliminación lógica. Por defecto `true`. |

La entidad aplica valores por defecto con `@PrePersist` y `@PreUpdate`: si `estado` llega nulo se asigna `PENDIENTE`; si `activo` llega nulo se asigna `true`.

## DTO `ProcesoDTO`

`ProcesoDTO` es usado para entrada y salida de la API.

| Campo | Regla |
|---|---|
| `id` | No debe enviarse en creación. En actualización, si se envía, debe coincidir con la ruta. |
| `numeroRadicado` | Se normaliza. Puede quedar nulo mientras el estado sea `PENDIENTE`. |
| `departamentoId` | Obligatorio por anotación `@NotNull` y validado en service. |
| `consultaId` | Obligatorio por anotación `@NotNull` y validado en service. |
| `especialidadId` | Opcional. Si se envía, debe existir activa. |
| `organoControlId` | Opcional. Obligatorio si se envía especialidad. |
| `estado` | Expuesto en respuesta. No se cambia desde creación o actualización general. |
| `activo` | Expuesto en respuesta. No se cambia desde creación o actualización general. |

## Estados funcionales

`EstadoProceso` define:

| Estado | Clasificación | Descripción |
|---|---|---|
| `PENDIENTE` | Operativo/inicial | Estado por defecto. Permite proceso sin radicado. |
| `SENTENCIA_FAVORABLE` | Final | Resultado final favorable. |
| `SENTENCIA_DESFAVORABLE` | Final | Resultado final desfavorable. |
| `DESISTIMIENTO` | Final | Terminación por desistimiento. |
| `RECHAZO` | Final | Terminación por rechazo. |
| `PRESCRIPCION` | Final | Terminación por prescripción. |

El método `esFinal()` considera final todo estado diferente de `PENDIENTE`.

## Regla del radicado

`ProcesoValidator.normalizarNumeroRadicadoParaEstado` implementa la regla central del radicado:

- el texto se normaliza;
- texto vacío se trata como `null`;
- si el estado evaluado es `PENDIENTE`, el radicado puede ser nulo;
- si el estado evaluado es final, el radicado es obligatorio;
- si se informa radicado, debe tener exactamente 23 caracteres.

La unicidad se valida en `ProcesoCommandService`:

- creación: `existsByNumeroRadicado`;
- actualización: `existsByNumeroRadicadoAndIdNot`.

## Creación

`ProcesoCommandService.crear` ejecuta el siguiente flujo:

1. valida DTO obligatorio;
2. rechaza `id` en creación;
3. normaliza `numeroRadicado` usando estado `PENDIENTE`;
4. valida permiso y alcance sobre la consulta;
5. valida unicidad del radicado si se informó;
6. carga departamento activo;
7. carga consulta existente y valida operación operativa;
8. carga órgano de control activo si se informó;
9. carga especialidad activa si se informó;
10. valida que la especialidad pertenezca al órgano de control;
11. aplica datos a la entidad;
12. fuerza `estado = PENDIENTE`;
13. fuerza `activo = true`;
14. guarda y retorna DTO.

El estado y la marca activa no dependen del cuerpo enviado en creación.

## Actualización

`ProcesoCommandService.actualizar` ejecuta:

1. valida permiso de actualización y alcance;
2. valida DTO obligatorio;
3. valida que el `id` del DTO no cambie;
4. busca proceso activo;
5. impide cambiar `consultaId`;
6. valida radicado contra el estado actual guardado del proceso;
7. valida unicidad del radicado;
8. carga departamento activo;
9. carga consulta y valida operación operativa;
10. carga órgano y especialidad activos si fueron informados;
11. valida relación especialidad/órgano;
12. valida que existan cambios reales;
13. aplica datos editables;
14. guarda.

La actualización general no cambia `estado` ni `activo`. Es una operación `PUT` de datos editables, no un cambio parcial de estado.

## Cambio de estado funcional

`ProcesoCommandService.cambiarEstadoProceso` se invoca desde:

```text
PATCH /api/procesos/{id}/estado?estado=
```

El flujo real es:

1. valida permiso `GESTIONAR_PROCESOS`;
2. bloquea gestión para estudiantes y conciliadores;
3. valida alcance sobre la consulta asociada;
4. busca proceso activo;
5. valida que la consulta permita operación operativa;
6. valida estado obligatorio;
7. valida que el nuevo estado sea diferente al actual;
8. si el estado destino es final, valida que el radicado guardado exista y tenga 23 caracteres;
9. actualiza estado.

El endpoint de cambio de estado no recibe el radicado en el cuerpo. Si se desea llevar un proceso a estado final, el radicado debe haberse guardado previamente en el proceso.

## Activación, desactivación y eliminación lógica

El proceso separa dos conceptos:

| Concepto | Campo / endpoint | Propósito |
|---|---|---|
| Estado funcional | `estado`, `PATCH /estado` | Resultado procesal. |
| Marca activa | `activo`, `PATCH /activo`, `DELETE` | Eliminación lógica o reactivación. |

`DELETE /api/procesos/{id}` ejecuta la desactivación lógica: busca un proceso activo y establece `activo=false`, conservando el registro persistido.

`PATCH /api/procesos/{id}/activo?activo=` permite cambiar la marca activa de un proceso existente, siempre que el usuario tenga permiso, alcance y la consulta asociada permita operación.

## Listados operativos

`ProcesoQueryService.listar` usa:

```text
findByActivoTrueAndConsulta_EstadoNotOrderByIdDesc(ARCHIVADO)
```

Luego aplica alcance registro por registro con `ProcesoAccessService.puedeAccederAProceso`.

Por tanto, el listado operativo:

- solo incluye procesos activos;
- excluye procesos asociados a consultas archivadas;
- respeta alcance por usuario.

`obtenerPorId` también usa `findByIdAndActivoTrueAndConsulta_EstadoNot`, por lo que el detalle operativo retorna procesos activos asociados a consultas con estado diferente de `ARCHIVADO`.

## Permisos y alcance

El controller declara permisos con `@PreAuthorize`, pero el comportamiento efectivo incluye validaciones internas.

| Acción | Validación real |
|---|---|
| Listar | Requiere `VER_PROCESOS`. |
| Obtener por id | Requiere `VER_PROCESOS` y alcance. |
| Crear | Requiere `GESTIONAR_PROCESOS`, no ser estudiante ni conciliador, y alcance sobre consulta. |
| Actualizar | Requiere `GESTIONAR_PROCESOS`, no ser estudiante ni conciliador, y alcance. |
| Cambiar estado | Requiere `GESTIONAR_PROCESOS`, no ser estudiante ni conciliador, y alcance. |
| Cambiar activo | Requiere `GESTIONAR_PROCESOS`, no ser estudiante ni conciliador, y alcance. |
| Eliminar | Requiere `GESTIONAR_PROCESOS`, no ser estudiante ni conciliador, y alcance. |

El estudiante puede consultar procesos si tiene permiso y alcance, pero no los gestiona. El conciliador no tiene alcance operativo sobre procesos en la implementación actual.

## Catálogos relacionados

El proceso se integra con:

- Departamento.
- Órgano de control.
- Especialidad.

Los órganos de control y especialidades tienen controllers propios:

```text
/api/organos-control
/api/especialidades
```

La especialidad depende de un órgano de control. En procesos, si se informa una especialidad, debe informarse también un órgano de control, y la especialidad debe pertenecer a ese órgano.

## Repositorio y estadísticas

`ProcesoRepository` contiene consultas operativas y agregaciones:

| Método | Uso |
|---|---|
| `findByIdAndActivoTrue` | Buscar proceso activo. |
| `findByIdAndActivoTrueAndConsulta_EstadoNot` | Buscar proceso activo excluyendo consulta archivada. |
| `findByActivoTrueAndConsulta_EstadoNotOrderByIdDesc` | Listado operativo. |
| `existsByNumeroRadicado` | Unicidad en creación. |
| `existsByNumeroRadicadoAndIdNot` | Unicidad en actualización. |
| `existsByConsulta_IdAndActivoTrueAndEstado` | Validación de cierre de consulta con procesos pendientes. |
| `contarProcesosPorEstado` | Agregación global por estado. |
| `contarProcesosPorEstadoYAsesor` | Agregación por estado filtrada por asesor de consulta. |
| `contarProcesosPorEstadoYEstudiante` | Agregación por estado filtrada por estudiante de consulta. |
| `contarProcesosPorEstadoYMonitor` | Agregación por estado filtrada por monitor de consulta. |

Las agregaciones de procesos implementadas en el repository corresponden al conteo global por estado y a los conteos por estado filtrados por asesor, estudiante o monitor de la consulta.

## Relación con cierre de consulta

El proceso influye en el cierre de consulta. `ConsultaEstadoService` bloquea el cierre de una consulta cuando existe un proceso activo en estado `PENDIENTE` asociado a esa consulta.

Los estados finales de proceso permiten que ese bloqueo desaparezca desde la perspectiva de cierre de consulta, siempre que las demás reglas de cierre también se cumplan.

## Pruebas relacionadas

`ProcesoValidatorTest` valida reglas esenciales del radicado:

- radicado nulo permitido en `PENDIENTE`;
- texto vacío tratado como nulo en `PENDIENTE`;
- rechazo de estado final sin radicado;
- rechazo de radicado con longitud distinta de 23;
- aceptación de radicado válido.
