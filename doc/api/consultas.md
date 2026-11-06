# API - Consultas jurídicas

Este documento describe los endpoints vigentes del módulo de consultas jurídicas expuestos por `ConsultaController` bajo el prefijo `/api/consultas`.

La API de consultas permite buscar, consultar detalle, crear, actualizar, reasignar individualmente responsables, cambiar estado, archivar, listar archivadas y desarchivar consultas jurídicas. Todas las operaciones requieren sesión autenticada y validación de permisos desde backend.

## Base path

```text
/api/consultas
```

## Autenticación

Todos los endpoints requieren una sesión válida. El backend obtiene el JWT desde la cookie `access_token`; el cliente debe enviar las cookies, por ejemplo con `credentials: "include"`.

Las operaciones que modifican estado, incluido el `PATCH` de reasignación, están protegidas además contra CSRF. El cliente obtiene el nombre del header y el token mediante `GET /api/auth/csrf` y envía ese token en el header indicado por la respuesta. La cookie CSRF es `HttpOnly`, por lo que el valor se toma de la respuesta del endpoint y no se lee desde JavaScript.

## Permisos aplicados

| Endpoint | Permisos aceptados |
|---|---|
| `GET /api/consultas` | `VER_CONSULTAS` o `GESTIONAR_CONSULTAS` |
| `GET /api/consultas/{id}` | `VER_CONSULTAS` o `GESTIONAR_CONSULTAS` |
| `POST /api/consultas` | `CREAR_CONSULTAS` o `GESTIONAR_CONSULTAS` |
| `PUT /api/consultas/{id}` | `EDITAR_CONSULTAS` o `GESTIONAR_CONSULTAS` |
| `PATCH /api/consultas/{id}/responsable` | `ASIGNAR_RESPONSABLES_CONSULTA` |
| `PATCH /api/consultas/{id}/estado` | `CAMBIAR_ESTADO_CONSULTAS`, `ARCHIVAR_CONSULTAS` o `GESTIONAR_CONSULTAS` |
| `DELETE /api/consultas/{id}` | `ARCHIVAR_CONSULTAS` |
| `PATCH /api/consultas/{id}/archivar` | `ARCHIVAR_CONSULTAS` |
| `GET /api/consultas/archivadas` | `ARCHIVAR_CONSULTAS` |
| `PATCH /api/consultas/{id}/desarchivar` | `ARCHIVAR_CONSULTAS` |

La asignación o modificación de responsables internos se valida adicionalmente con `ASIGNAR_RESPONSABLES_CONSULTA` cuando el DTO contiene cambios en asesor, estudiante o monitor.

> Precisión de seguridad: aunque el endpoint de cambio de estado acepta `ARCHIVAR_CONSULTAS` en la anotación del controller, la validación interna distingue el caso de archivo. Para estados distintos de `ARCHIVADO`, el backend valida permiso de cambio de estado o gestión de consultas; cuando el estado destino es `ARCHIVADO`, aplica la política específica de archivo.

## Estados permitidos

```text
PENDIENTE
ACTIVO
EN_PROCESO
URGENTE
CERRADO
ARCHIVADO
```

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Estado inicial de una consulta nueva. |
| `ACTIVO` | Consulta habilitada para atención activa. |
| `EN_PROCESO` | Consulta en gestión operativa. |
| `URGENTE` | Consulta priorizada. |
| `CERRADO` | Consulta finalizada funcionalmente. |
| `ARCHIVADO` | Consulta conservada como histórico. |

## DTO `ConsultaDTO`

DTO principal de entrada y salida.

```json
{
  "id": 1,
  "version": 4,
  "fecha": "2026-05-31",
  "descripcion": "Descripción de la consulta",
  "hechos": "Relato de hechos",
  "pretensiones": "Pretensiones de la persona consultante",
  "conceptoJuridico": "Concepto jurídico registrado",
  "tramite": "Trámite o ruta de atención",
  "observaciones": "Observaciones complementarias",
  "tipoViolencia": "Tipo de violencia",
  "estado": "PENDIENTE",
  "resultado": "Resultado de cierre",
  "personaId": 1,
  "partesIds": [2, 3],
  "contrapartesIds": [4, 5],
  "sedeId": 1,
  "areaId": 1,
  "temaId": 1,
  "tipoId": 1,
  "asesorId": 1,
  "monitorId": 1,
  "estudianteId": 1
}
```

### Validaciones principales del DTO

| Campo | Regla |
|---|---|
| `version` | Versión de concurrencia retornada por el backend; debe reenviarse en operaciones que exigen control optimista. |
| `fecha` | Obligatoria. |
| `descripcion` | Obligatoria; máximo 500 caracteres. |
| `hechos` | Obligatorio. |
| `pretensiones` | Obligatorio. |
| `conceptoJuridico` | Obligatorio. |
| `tramite` | Obligatorio; máximo 100 caracteres. |
| `tipoViolencia` | Opcional; máximo 100 caracteres. |
| `resultado` | Opcional durante operación; obligatorio para cerrar. Máximo 100 caracteres. |
| `personaId` | Obligatorio. |
| `sedeId` | Obligatorio. |
| `areaId` | Obligatorio. |
| `temaId` | Obligatorio. |
| `tipoId` | Opcional. |
| `asesorId`, `monitorId`, `estudianteId` | Opcionales; requieren permiso específico cuando se asignan o cambian. |

## DTO `ConsultaBusquedaDTO`

DTO retornado por el buscador.

```json
{
  "id": 1,
  "consulta": "Descripción de la consulta",
  "fecha": "2026-05-31",
  "nombre": "Nombre persona",
  "apellido": "Apellido persona",
  "cedula": "Número de documento",
  "estado": "PENDIENTE"
}
```

## Resumen de endpoints

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/consultas?search=` | Busca consultas no archivadas según alcance del usuario. |
| `GET` | `/api/consultas/{id}` | Obtiene el detalle de una consulta. |
| `POST` | `/api/consultas` | Crea una nueva consulta. |
| `PUT` | `/api/consultas/{id}` | Actualiza datos generales de una consulta. |
| `PATCH` | `/api/consultas/{id}/responsable` | Reasigna exactamente un estudiante, asesor o monitor. |
| `PATCH` | `/api/consultas/{id}/estado?estado=` | Cambia el estado funcional de la consulta. |
| `DELETE` | `/api/consultas/{id}` | Archiva lógicamente la consulta por compatibilidad del endpoint. |
| `PATCH` | `/api/consultas/{id}/archivar` | Archiva una consulta cerrada. |
| `GET` | `/api/consultas/archivadas` | Lista consultas archivadas. |
| `PATCH` | `/api/consultas/{id}/desarchivar` | Retorna una consulta archivada al estado `CERRADO`. |

---

# GET `/api/consultas`

Busca consultas jurídicas aplicando filtros de alcance desde backend.

## Query params

| Parámetro | Tipo | Obligatorio | Valor por defecto | Uso |
|---|---|---|---|---|
| `search` | String | No | `""` | Término de búsqueda. |

El término se compara contra:

- descripción de la consulta;
- nombres de la persona principal;
- apellidos de la persona principal;
- número de documento de la persona principal.

## Alcance del resultado

| Perfil | Resultado retornado |
|---|---|
| Administrador | Todas las consultas no archivadas. |
| Estudiante | Consultas donde el estudiante está asignado. |
| Asesor | Consultas donde el asesor está asignado o donde el estudiante asignado pertenece a su asesoría. |
| Monitor | Consultas donde el monitor está asignado. |
| Conciliador | Lista vacía desde el buscador general de consultas. |

## Response `200 OK`

```json
[
  {
    "id": 1,
    "consulta": "Descripción de la consulta",
    "fecha": "2026-05-31",
    "nombre": "Nombre persona",
    "apellido": "Apellido persona",
    "cedula": "Número de documento",
    "estado": "PENDIENTE"
  }
]
```

---

# GET `/api/consultas/{id}`

Obtiene el detalle de una consulta jurídica.

## Parámetros

| Parámetro | Tipo | Ubicación |
|---|---|---|
| `id` | Long | Path |

## Reglas aplicadas

- requiere permiso de consulta;
- valida alcance del usuario sobre la consulta;
- carga partes y contrapartes asociadas;
- retorna `ConsultaDTO`.

## Response `200 OK`

```json
{
  "id": 1,
  "version": 4,
  "fecha": "2026-05-31",
  "descripcion": "Descripción de la consulta",
  "hechos": "Relato de hechos",
  "pretensiones": "Pretensiones",
  "conceptoJuridico": "Concepto jurídico",
  "tramite": "Trámite",
  "observaciones": "Observaciones",
  "tipoViolencia": "Tipo de violencia",
  "estado": "PENDIENTE",
  "resultado": null,
  "personaId": 1,
  "partesIds": [2, 3],
  "contrapartesIds": [4, 5],
  "sedeId": 1,
  "areaId": 1,
  "temaId": 1,
  "tipoId": 1,
  "asesorId": 1,
  "monitorId": 1,
  "estudianteId": 1
}
```

---

# POST `/api/consultas`

Crea una consulta jurídica.

## Request body

Usa `ConsultaDTO`. La operación se implementa como `PUT`, por lo que el backend espera el DTO completo de consulta y no una actualización parcial de campos.

## Reglas aplicadas

- el DTO no debe enviar `id`;
- la consulta inicia en estado `PENDIENTE`;
- si se envía `estado`, debe ser `PENDIENTE`;
- persona, sede, área y tema son obligatorios;
- las relaciones deben existir y estar activas;
- se valida coherencia área-tema-tipo;
- se valida coherencia de asesor, estudiante y área cuando se asignan responsables;
- se valida que persona principal, partes y contrapartes no se repitan indebidamente;
- si se envía estudiante sin asesor explícito, el backend asigna el asesor activo asociado al estudiante.

## Response `201 Created`

Retorna `ConsultaDTO`.

---

# PUT `/api/consultas/{id}`

Actualiza datos generales de una consulta.

## Parámetros

| Parámetro | Tipo | Ubicación |
|---|---|---|
| `id` | Long | Path |

## Request body

Usa `ConsultaDTO`. La operación se implementa como `PUT`, por lo que el backend espera el DTO completo de consulta y no una actualización parcial de campos.

## Reglas aplicadas

- requiere permiso de edición y alcance sobre la consulta;
- no permite editar consultas cerradas o archivadas;
- si el DTO trae `id`, debe coincidir con el `id` de la ruta;
- el estado no se modifica desde este endpoint;
- si se modifican responsables, se exige permiso `ASIGNAR_RESPONSABLES_CONSULTA`;
- si la consulta tiene procesos, seguimientos o conciliaciones activas asociadas, se protegen sus datos estructurales;
- los campos de responsables solo se evalúan y aplican cuando el usuario tiene permiso de asignación de responsables;
- se valida coherencia de dominio después de aplicar los datos.

## Datos estructurales protegidos con actividad asociada

Cuando la consulta tiene actividad asociada, el backend protege:

- persona principal;
- partes;
- contrapartes;
- sede;
- área;
- tema;
- tipo;
- asesor;
- estudiante;
- monitor.

## Response `200 OK`

Retorna `ConsultaDTO` actualizado.

---

# PATCH `/api/consultas/{id}/responsable`

Reasigna exactamente un responsable de la consulta. Esta operación no modifica simultáneamente otros responsables ni reutiliza la actualización general de la consulta.

## Parámetros

| Parámetro | Tipo | Ubicación | Uso |
|---|---|---|---|
| `id` | Long | Path | Identificador de la consulta. |

## Request body

Usa `ConsultaReasignacionDTO`:

```json
{
  "tipoResponsable": "MONITOR",
  "nuevoResponsableId": 27,
  "motivo": "Reasignación por distribución de carga",
  "version": 4
}
```

| Campo | Regla |
|---|---|
| `tipoResponsable` | Obligatorio. Valores permitidos: `ESTUDIANTE`, `ASESOR` o `MONITOR`. |
| `nuevoResponsableId` | Obligatorio y positivo. Su tabla de referencia depende de `tipoResponsable`. |
| `motivo` | Obligatorio y no vacío. El backend elimina espacios extremos y compacta espacios internos consecutivos antes de persistirlo. |
| `version` | Obligatoria y no negativa. Debe coincidir con la versión vigente de la consulta. |

## Reglas aplicadas

- requiere autenticación, permiso `ASIGNAR_RESPONSABLES_CONSULTA` y alcance efectivo sobre la consulta;
- un estudiante no puede ejecutar asignaciones de responsables;
- permite la operación en `PENDIENTE`, `ACTIVO`, `EN_PROCESO` y `URGENTE`, y la rechaza en `CERRADO` o `ARCHIVADO`;
- el nuevo responsable debe existir, estar activo y cumplir las reglas vigentes de área, asesor y estudiante;
- no permite asignar nuevamente al responsable actual;
- modifica solo el rol indicado y conserva los demás responsables, personas, datos jurídicos y estado;
- registra consulta, tipo de responsable, identificadores anterior y nuevo, motivo, actor e instante del evento;
- actualiza la consulta y escribe el historial dentro de la misma transacción. Un error en cualquiera de las dos escrituras revierte ambas;
- el control `@Version` evita sobrescrituras cuando otra operación ya modificó la consulta.

Los identificadores `responsableAnteriorId` y `responsableNuevoId` del historial son polimórficos: corresponden a `estudiante`, `asesor` o `monitor` según `tipoResponsable`. No representan una relación con una tabla única de responsables.

## Response `200 OK`

Retorna el `ConsultaDTO` completo con el nuevo responsable y la versión incrementada:

```json
{
  "id": 10,
  "version": 5,
  "fecha": "2026-05-31",
  "descripcion": "Descripción de la consulta",
  "hechos": "Relato de hechos",
  "pretensiones": "Pretensiones",
  "conceptoJuridico": "Concepto jurídico",
  "tramite": "Trámite",
  "observaciones": "Observaciones",
  "tipoViolencia": null,
  "estado": "ACTIVO",
  "resultado": null,
  "personaId": 1,
  "partesIds": [2, 3],
  "contrapartesIds": [4],
  "sedeId": 1,
  "areaId": 1,
  "temaId": 1,
  "tipoId": 1,
  "asesorId": 8,
  "monitorId": 27,
  "estudianteId": 15
}
```

No existe actualmente un endpoint para consultar el historial de reasignaciones.

## Respuestas de error relevantes

| Estado HTTP | Situación |
|---|---|
| `400 Bad Request` | Cuerpo inválido, campos obligatorios ausentes, consulta cerrada o archivada, responsable inexistente, inactivo, incompatible o igual al actual. |
| `401 Unauthorized` | No se envió una autenticación válida. |
| `403 Forbidden` | El actor no posee el permiso requerido o no tiene alcance sobre la consulta. |
| `409 Conflict` | La `version` enviada ya no coincide con la versión persistida o se detecta el conflicto durante el `flush` transaccional. El cliente debe recargar la consulta antes de reintentar. |

---

# PATCH `/api/consultas/{id}/estado?estado=`

Cambia el estado funcional de una consulta.

## Parámetros

| Parámetro | Tipo | Ubicación | Uso |
|---|---|---|---|
| `id` | Long | Path | Identificador de consulta. |
| `estado` | `EstadoConsulta` | Query param | Estado destino. |

Ejemplo:

```text
PATCH /api/consultas/10/estado?estado=CERRADO
```

## Reglas aplicadas

- el estado destino es obligatorio;
- no se permite cambiar al mismo estado actual;
- una consulta archivada no se modifica como consulta operativa;
- una consulta cerrada solo puede archivarse;
- el estudiante no puede cambiar estado;
- para pasar a `ACTIVO`, `EN_PROCESO` o `URGENTE`, la consulta debe tener asesor y estudiante;
- para cerrar, la consulta debe tener resultado y no tener pendientes operativos.

## Cierre de consulta

Para cambiar a `CERRADO`, el backend valida:

- `resultado` con contenido;
- ausencia de procesos activos `PENDIENTE`;
- ausencia de seguimientos activos `PENDIENTE`;
- ausencia de respuestas activas de seguimiento `PENDIENTE`;
- ausencia de notificaciones activas de seguimiento sin enviar;
- ausencia de conciliaciones activas no finalizadas.

Los estados de conciliación que bloquean el cierre de la consulta son `EN_ESPERA`, `ESPERANDO_REUNION` y `REUNION_PROGRAMADA`.

## Response `200 OK`

Retorna `ConsultaDTO`.

---

# DELETE `/api/consultas/{id}`

Conservado por compatibilidad. Internamente funciona como archivado lógico de consulta.

## Reglas aplicadas

- requiere permiso `ARCHIVAR_CONSULTAS`;
- la consulta debe estar `CERRADO`;
- se validan pendientes operativos como defensa de consistencia;
- la consulta pasa a `ARCHIVADO`.

## Response `204 No Content`

---

# PATCH `/api/consultas/{id}/archivar`

Archiva una consulta cerrada y retorna el DTO resultante.

## Reglas aplicadas

- requiere permiso `ARCHIVAR_CONSULTAS`;
- la política de acceso exige rol administrador;
- la consulta debe estar `CERRADO`;
- la consulta no debe tener pendientes operativos;
- la consulta pasa a `ARCHIVADO`.

## Response `200 OK`

Retorna `ConsultaDTO`.

---

# GET `/api/consultas/archivadas`

Lista consultas archivadas.

## Reglas aplicadas

- requiere permiso `ARCHIVAR_CONSULTAS`;
- la política de acceso exige rol administrador;
- retorna consultas en estado `ARCHIVADO`.

## Response `200 OK`

Retorna lista de `ConsultaBusquedaDTO`.

---

# PATCH `/api/consultas/{id}/desarchivar`

Retorna una consulta archivada al estado `CERRADO`.

## Reglas aplicadas

- requiere permiso `ARCHIVAR_CONSULTAS`;
- la política de acceso exige rol administrador;
- la consulta debe estar `ARCHIVADO`;
- se validan pendientes operativos como defensa de consistencia;
- la consulta queda en estado `CERRADO`.

## Response `200 OK`

Retorna `ConsultaDTO`.

## Errores comunes del módulo

| Estado HTTP esperado | Situación |
|---|---|
| `400 Bad Request` | Datos obligatorios faltantes, estado inválido, cambio de estado no permitido o regla de negocio incumplida. |
| `401 Unauthorized` | Sesión no autenticada. |
| `403 Forbidden` | Usuario sin permiso o sin alcance sobre la consulta. |
| `404 Not Found` | Consulta o relación asociada no encontrada cuando el manejo global lo traduzca a ese estado. |
| `409 Conflict` | Versión desactualizada o conflicto de concurrencia optimista. |
