# API - Procesos

## Propósito

La API de procesos expone las operaciones necesarias para registrar, consultar, actualizar, cambiar estado funcional y desactivar lógicamente los procesos asociados a consultas jurídicas. El proceso no maneja un alcance independiente: su visibilidad y operación se derivan de la consulta a la que pertenece.

El contrato documentado en este archivo corresponde al código fuente actual del módulo `ProcesoController`, `ProcesoService`, `ProcesoCommandService`, `ProcesoQueryService`, `ProcesoValidator`, `ProcesoAccessService`, `ProcesoDTO`, `Proceso` y `EstadoProceso`.

## Base URL

```text
/api/procesos
```

## Seguridad efectiva

Los endpoints están protegidos por permisos de Spring Security y por validaciones internas de alcance en `ProcesoAccessService`.

| Operación | Permiso efectivo | Observación |
|---|---|---|
| Listar procesos | `VER_PROCESOS` | Aunque el controller acepta también `GESTIONAR_PROCESOS`, la validación interna de lectura exige `VER_PROCESOS`. |
| Obtener proceso por id | `VER_PROCESOS` | También valida alcance sobre la consulta asociada. |
| Crear proceso | `GESTIONAR_PROCESOS` | Estudiantes y conciliadores no gestionan procesos. |
| Actualizar proceso | `GESTIONAR_PROCESOS` | Requiere alcance sobre la consulta asociada. |
| Cambiar estado funcional | `GESTIONAR_PROCESOS` | Requiere alcance sobre la consulta asociada. |
| Cambiar marca `activo` | `GESTIONAR_PROCESOS` | Requiere alcance sobre la consulta asociada. |
| Desactivar lógicamente | `GESTIONAR_PROCESOS` | Conserva el proceso con `activo=false`. |

El proceso hereda su alcance de la consulta. Por ello, aun teniendo el permiso general, el backend valida si el usuario puede acceder o gestionar la consulta asociada.

## Alcance por perfil

| Perfil | Lectura | Gestión |
|---|---|---|
| Administrativo autorizado | Según permisos. | Según permisos. |
| Asesor | Puede acceder si la consulta está dentro de su alcance. | Puede gestionar si tiene permiso y la consulta está dentro de su alcance. |
| Monitor | Puede acceder si la consulta está asignada al monitor. | Puede gestionar si tiene permiso y alcance. |
| Estudiante | Puede consultar procesos de sus consultas si tiene permiso de lectura. | No puede crear, editar, cambiar estado ni desactivar procesos. |
| Conciliador | No tiene alcance operativo sobre procesos en esta fase. | No puede gestionar procesos. |

## DTO principal

`ProcesoDTO` es el contrato de entrada y salida.

| Campo | Tipo | Entrada | Regla |
|---|---|---|---|
| `id` | Long | No en creación | En actualización debe coincidir con el id de la ruta si se envía. |
| `numeroRadicado` | String | Opcional según estado | Puede ser nulo en `PENDIENTE`; para estados finales debe existir y tener 23 caracteres. |
| `departamentoId` | Long | Obligatorio | Debe corresponder a un departamento activo. |
| `consultaId` | Long | Obligatorio | La consulta asociada define alcance y debe permitir operación. No se puede cambiar en edición. |
| `especialidadId` | Long | Opcional | Si se informa, debe estar activa y pertenecer al órgano de control indicado. |
| `organoControlId` | Long | Opcional | Si hay especialidad, el órgano de control es obligatorio. |
| `estado` | EstadoProceso | Respuesta / referencia | No se modifica por `POST` ni por `PUT`; se cambia por `PATCH /estado`. |
| `activo` | Boolean | Respuesta / referencia | No se modifica por `POST` ni por `PUT`; se cambia por `PATCH /activo` o `DELETE`. |

## Estados de proceso

`EstadoProceso` contiene:

```text
PENDIENTE
SENTENCIA_FAVORABLE
SENTENCIA_DESFAVORABLE
DESISTIMIENTO
RECHAZO
PRESCRIPCION
```

El método `esFinal()` considera final todo estado distinto de `PENDIENTE`. Esa regla se usa para exigir radicado antes de registrar un resultado final.

## Listar procesos

```http
GET /api/procesos
```

### Permiso efectivo

```text
VER_PROCESOS
```

### Comportamiento

El backend:

1. valida permiso de lectura;
2. consulta procesos activos;
3. excluye procesos asociados a consultas archivadas;
4. filtra cada registro según el alcance del usuario;
5. retorna lista de `ProcesoDTO`.

### Respuesta

```json
[
  {
    "id": 10,
    "numeroRadicado": "12345678901234567890123",
    "departamentoId": 1,
    "consultaId": 15,
    "especialidadId": 4,
    "organoControlId": 2,
    "estado": "SENTENCIA_FAVORABLE",
    "activo": true
  }
]
```

## Obtener proceso por id

```http
GET /api/procesos/{id}
```

### Permiso efectivo

```text
VER_PROCESOS
```

### Parámetros

| Parámetro | Tipo | Ubicación | Descripción |
|---|---|---|---|
| `id` | Long | Path | Identificador del proceso. |

### Comportamiento

El backend valida permiso de lectura, verifica alcance sobre el proceso y retorna el registro activo siempre que no pertenezca a una consulta archivada.

## Crear proceso

```http
POST /api/procesos
```

### Permiso

```text
GESTIONAR_PROCESOS
```

### Cuerpo

```json
{
  "numeroRadicado": null,
  "departamentoId": 1,
  "consultaId": 15,
  "organoControlId": 2,
  "especialidadId": 4
}
```

### Reglas aplicadas

1. `id` no debe enviarse.
2. `departamentoId` es obligatorio y debe estar activo.
3. `consultaId` es obligatorio.
4. La consulta asociada debe permitir operación operativa.
5. El usuario debe tener permiso y alcance para crear proceso en esa consulta.
6. El proceso se crea siempre con `estado = PENDIENTE`.
7. El proceso se crea siempre con `activo = true`.
8. `numeroRadicado` puede ser nulo o vacío en creación porque el estado inicial es `PENDIENTE`.
9. Si se informa radicado, debe tener exactamente 23 caracteres.
10. Si se informa radicado, debe ser único.
11. Si se informa especialidad, debe informarse órgano de control.
12. La especialidad debe pertenecer al órgano de control seleccionado.

### Respuesta

```http
201 Created
```

con `ProcesoDTO`.

## Actualizar proceso

```http
PUT /api/procesos/{id}
```

### Permiso

```text
GESTIONAR_PROCESOS
```

### Cuerpo

El endpoint espera el DTO con los datos editables del proceso. No funciona como actualización parcial.

```json
{
  "id": 10,
  "numeroRadicado": "12345678901234567890123",
  "departamentoId": 1,
  "consultaId": 15,
  "organoControlId": 2,
  "especialidadId": 4
}
```

### Reglas aplicadas

1. El usuario debe tener permiso de gestión y alcance sobre la consulta asociada.
2. El proceso debe estar activo.
3. No se permite cambiar el `id`.
4. No se permite cambiar `consultaId`.
5. La consulta asociada debe permitir operación operativa.
6. El radicado se valida contra el estado funcional actual guardado en base de datos.
7. Si el proceso ya está en un estado final, debe conservar radicado válido.
8. Si se informa radicado, debe tener exactamente 23 caracteres.
9. Si se informa radicado, debe conservar unicidad.
10. El departamento debe estar activo.
11. El órgano de control, si se informa, debe estar activo.
12. La especialidad, si se informa, debe estar activa.
13. La especialidad debe pertenecer al órgano de control seleccionado.
14. Deben existir cambios reales para actualizar.

### Campos no modificados por `PUT`

`PUT /api/procesos/{id}` no modifica:

- `estado`;
- `activo`.

El estado funcional se cambia con `PATCH /api/procesos/{id}/estado`. La marca de eliminación lógica se cambia con `PATCH /api/procesos/{id}/activo` o mediante `DELETE`.

## Cambiar estado funcional

```http
PATCH /api/procesos/{id}/estado?estado=SENTENCIA_FAVORABLE
```

### Permiso

```text
GESTIONAR_PROCESOS
```

### Parámetros

| Parámetro | Tipo | Ubicación | Descripción |
|---|---|---|---|
| `id` | Long | Path | Identificador del proceso. |
| `estado` | EstadoProceso | Query | Nuevo estado funcional. |

### Reglas aplicadas

1. El usuario debe poder gestionar procesos.
2. Estudiantes y conciliadores no pueden cambiar estados de procesos.
3. El proceso debe estar activo.
4. La consulta asociada debe permitir operación operativa.
5. El estado es obligatorio.
6. El nuevo estado no puede ser igual al actual.
7. Si el nuevo estado es final, el proceso debe tener radicado guardado previamente.
8. El radicado guardado debe tener exactamente 23 caracteres.

El endpoint no recibe el radicado. Para pasar a un estado final, el radicado debe haberse guardado antes mediante creación o actualización del proceso.

## Cambiar marca activo

```http
PATCH /api/procesos/{id}/activo?activo=false
```

### Permiso

```text
GESTIONAR_PROCESOS
```

### Reglas aplicadas

1. El proceso se busca por id, esté activo o inactivo.
2. La consulta asociada debe permitir operación operativa.
3. El valor `activo` es obligatorio.
4. El nuevo valor no puede ser igual al actual.
5. No cambia el estado funcional del proceso.

Este endpoint permite cambiar la marca activa del proceso desde API. La pantalla frontend de procesos utiliza `DELETE` para ejecutar la desactivación lógica visible.

## Desactivar lógicamente un proceso

```http
DELETE /api/procesos/{id}
```

### Permiso

```text
GESTIONAR_PROCESOS
```

### Comportamiento

El backend busca el proceso activo, valida permiso y alcance, valida que la consulta permita operación operativa y marca:

```json
{
  "activo": false
}
```

La operación conserva el registro persistido con `activo=false`.

### Respuesta

```http
204 No Content
```

## Validaciones frecuentes

| Situación | Resultado |
|---|---|
| Crear con `id` | Error de negocio. |
| Crear sin departamento | Error de validación o negocio. |
| Crear sin consulta | Error de validación o negocio. |
| Actualizar cambiando `consultaId` | Error de negocio. |
| Proceso final sin radicado | Error de negocio. |
| Radicado con longitud distinta de 23 | Error de negocio. |
| Radicado duplicado | Error de negocio. |
| Especialidad sin órgano de control | Error de negocio. |
| Especialidad que no pertenece al órgano | Error de negocio. |
| Cambiar estado al mismo estado | Error de negocio. |
| Cambiar `activo` al mismo valor | Error de negocio. |
| Gestionar proceso asociado a consulta cerrada o archivada | Error de negocio. |
| Consultar proceso sin alcance | Error de autorización. |

## Relación con catálogos de proceso

El módulo de procesos usa tres catálogos principales:

| Catálogo | Uso |
|---|---|
| Departamento | Obligatorio para crear o actualizar proceso. |
| Órgano de control | Opcional, pero requerido cuando se informa especialidad. |
| Especialidad | Opcional, dependiente de órgano de control. |

Los endpoints de órgano de control y especialidad se documentan también en `api/catalogos.md` porque son catálogos operativos usados por el módulo de procesos.
