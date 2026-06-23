# API - Conciliaciones y reuniones

## Base URL

```text
/api/conciliaciones
```

Controller principal: `ConciliacionController`.

Este módulo expone operaciones para crear conciliaciones desde consultas jurídicas, consultar conciliaciones visibles al usuario autenticado, asignar responsables, cambiar estados no finales, programar y reprogramar reuniones, reemplazar documentos PDF, finalizar con acta y desactivar registros de forma lógica.

La API trabaja con sesión autenticada y cookies. Todas las operaciones requieren permisos funcionales y, adicionalmente, validación de alcance en servicios de acceso.

---

## Permisos usados por la API

| Permiso | Uso principal |
|---|---|
| `VER_CONCILIACIONES` | Listar y consultar conciliaciones. |
| `GESTIONAR_CONCILIACIONES` | Crear conciliaciones, asignar conciliador, reemplazar solicitud, desactivar y participar en operaciones de gestión. |
| `CONCLUIR_CONCILIACIONES` | Participar en cambio de estado y finalización cuando el alcance lo permite. |
| `PROGRAMAR_REUNIONES_CONCILIACION` | Programar la reunión inicial. |
| `REPROGRAMAR_REUNIONES_CONCILIACION` | Reprogramar una reunión existente. |

Los permisos declarados en el controller son la primera capa de seguridad. Después, `ConciliacionAccessService` y `ConciliacionAlcanceService` validan si el usuario autenticado puede operar sobre la consulta o conciliación específica.

---

## Alcance operativo por perfil

| Perfil | Alcance en conciliaciones |
|---|---|
| Administrador | Puede ver y operar conciliaciones activas según permisos funcionales. |
| Asesor | Puede crear y ver conciliaciones de consultas donde es asesor asignado. |
| Monitor | Puede crear y ver conciliaciones de consultas donde es monitor asignado. |
| Estudiante | Puede ver conciliaciones donde es estudiante asignado a la conciliación o estudiante de la consulta. No crea ni gestiona conciliaciones. |
| Conciliador | Puede ver y operar conciliaciones donde está asignado como conciliador, según permiso funcional. |

Algunas operaciones tienen alcance más restringido:

- crear conciliación: administrador, asesor relacionado o monitor relacionado;
- asignar conciliador: alcance administrativo;
- asignar estudiante: administrador o conciliador asignado;
- programar, reprogramar y finalizar: administrador o conciliador asignado;
- reemplazar solicitud y desactivar: alcance administrativo.

---

## Estados técnicos

Los estados se resuelven desde la tabla `estado_conciliacion` y se validan por código técnico:

| Código | Significado operativo |
|---|---|
| `EN_ESPERA` | Conciliación activa sin responsables mínimos completos. |
| `ESPERANDO_REUNION` | Conciliación con estudiante y conciliador, lista para programar reunión. |
| `REUNION_PROGRAMADA` | Conciliación con reunión vigente registrada. |
| `COMPLETO_CONCILIADO` | Conciliación finalizada con acuerdo. |
| `COMPLETO_NO_CONCILIADO` | Conciliación finalizada sin acuerdo. |

`EN_ESPERA` se calcula automáticamente. Los estados finales no se asignan mediante el endpoint general de cambio de estado, sino mediante el endpoint de finalización con acta.

---

## DTOs de respuesta

### `ConciliacionResponseDTO`

DTO usado para listados y respuestas de operaciones de gestión.

| Campo | Descripción |
|---|---|
| `id` | Identificador de la conciliación. |
| `consultaId` | Consulta origen. |
| `estudianteId`, `estudianteNombre` | Estudiante asignado a la conciliación, si existe. |
| `conciliadorId`, `conciliadorNombre` | Conciliador asignado, si existe. |
| `estadoId`, `estadoCodigo`, `estadoNombre` | Estado funcional de conciliación. |
| `fechaConciliacion` | Campo de fecha principal de conciliación incluido en la entidad/DTO. El flujo vigente de reuniones usa `reunion.fechaReunion`. |
| `documentoSolicitudPath` | Ruta lógica del PDF de solicitud. |
| `actaPath` | Ruta lógica del acta PDF cuando la conciliación finaliza. |
| `solicitadoPorId`, `solicitadoPorUsername` | Usuario del sistema que creó la conciliación. |
| `activo` | Marca de actividad lógica. |
| `fechaCreacion`, `fechaActualizacion`, `fechaFinalizacion` | Marcas temporales del registro. |

### `ConciliacionDetalleResponseDTO`

Extiende la información del response general con:

| Campo | Descripción |
|---|---|
| `reunion` | Reunión vigente, si existe. |
| `consultante` | Persona principal de la consulta. |
| `partes` | Partes adicionales de la consulta. |
| `contrapartes` | Contrapartes de la consulta. |

La conciliación no almacena partes propias; toma el contexto de personas desde la consulta origen.

### `ReunionConciliacionRequestDTO`

```json
{
  "fechaReunion": "2026-06-20T09:00:00",
  "sedeId": 1,
  "observaciones": "Reunión inicial de conciliación."
}
```

| Campo | Regla |
|---|---|
| `fechaReunion` | Obligatoria y futura. |
| `sedeId` | Obligatoria; debe corresponder a sede activa. |
| `observaciones` | Opcional, máximo 300 caracteres. |

### `ReunionConciliacionResponseDTO`

| Campo | Descripción |
|---|---|
| `conciliacionId` | Identificador compartido con la conciliación. |
| `fechaReunion` | Fecha y hora de la reunión vigente. |
| `sedeId`, `sedeNombre` | Sede de la reunión. |
| `observaciones` | Observaciones normalizadas. |
| `fechaCreacion`, `fechaActualizacion` | Marcas temporales de la reunión. |

La reunión es una relación uno a uno con la conciliación y usa el mismo id mediante `@MapsId`.

---

## Endpoints

## Listar conciliaciones

```http
GET /api/conciliaciones
```

Permiso: `VER_CONCILIACIONES`.

Retorna conciliaciones activas visibles según el alcance del usuario autenticado. Los listados operativos excluyen conciliaciones asociadas a consultas archivadas.

### Respuesta

```json
[
  {
    "id": 1,
    "consultaId": 10,
    "estudianteId": 5,
    "estudianteNombre": "Estudiante asignado",
    "conciliadorId": 2,
    "conciliadorNombre": "Conciliador asignado",
    "estadoCodigo": "ESPERANDO_REUNION",
    "estadoNombre": "Esperando reunión",
    "documentoSolicitudPath": "conciliacion/1/solicitud.pdf",
    "actaPath": null,
    "activo": true
  }
]
```

---

## Listar conciliaciones por consulta

```http
GET /api/conciliaciones/consulta/{consultaId}
```

Permiso: `VER_CONCILIACIONES`.

Retorna conciliaciones activas de una consulta, excluyendo consultas archivadas y aplicando alcance por usuario.

---

## Obtener detalle de conciliación

```http
GET /api/conciliaciones/{id}
```

Permiso: `VER_CONCILIACIONES`.

Retorna `ConciliacionDetalleResponseDTO`. El detalle incluye consultante, partes, contrapartes y reunión vigente cuando existe.

---

## Crear conciliación desde consulta

```http
POST /api/conciliaciones/consulta/{consultaId}
Content-Type: multipart/form-data
```

Permiso: `GESTIONAR_CONCILIACIONES`.

Parámetros:

| Nombre | Tipo | Ubicación | Regla |
|---|---|---|---|
| `consultaId` | `Long` | Path | Consulta origen. |
| `solicitud` | `MultipartFile` | Form-data | PDF obligatorio. |

Reglas implementadas:

1. La consulta debe existir.
2. La consulta no puede estar cerrada ni archivada.
3. No puede existir otra conciliación activa no finalizada para la misma consulta.
4. La creación exige permiso funcional y alcance sobre la consulta.
5. El estudiante se selecciona automáticamente:
   - primero se intenta conservar el estudiante de la consulta si está activo y habilitado para conciliación;
   - si no cumple, se selecciona un estudiante activo con `conciliacion=true` y menor carga de conciliaciones no finalizadas.
6. El conciliador se selecciona entre conciliadores activos con menor carga de conciliaciones no finalizadas.
7. Si falta estudiante o conciliador, el estado queda `EN_ESPERA`.
8. Si existen ambos responsables, el estado queda `ESPERANDO_REUNION`.
9. La solicitud se guarda como PDF en ruta lógica `conciliacion/{id}/solicitud.pdf`.

---

## Programar reunión

```http
POST /api/conciliaciones/{id}/reunion
Content-Type: application/json
```

Permiso: `PROGRAMAR_REUNIONES_CONCILIACION`.

Alcance: administrador o conciliador asignado.

Reglas:

- conciliación activa y no finalizada;
- consulta asociada operativa;
- estudiante y conciliador asignados;
- reunión no existente previamente;
- fecha futura;
- sede activa;
- observaciones opcionales de máximo 300 caracteres.

Efectos:

- crea `ReunionConciliacion` como relación uno a uno con la conciliación;
- cambia o asegura estado `REUNION_PROGRAMADA`;
- registra historial de programación;
- crea notificaciones inmediatas y recordatorios.

---

## Reprogramar reunión

```http
PUT /api/conciliaciones/{id}/reunion
Content-Type: application/json
```

Permiso: `REPROGRAMAR_REUNIONES_CONCILIACION`.

Alcance: administrador o conciliador asignado.

Reglas:

- debe existir reunión vigente;
- fecha futura;
- sede activa;
- observaciones máximo 300 caracteres;
- debe existir un cambio real en fecha, sede u observaciones.

Efectos:

- actualiza la reunión existente;
- conserva o asegura estado `REUNION_PROGRAMADA`;
- registra historial de reprogramación;
- cancela notificaciones pendientes anteriores;
- crea nuevas notificaciones y recordatorios.

---

## Asignar estudiante

```http
PATCH /api/conciliaciones/{id}/estudiante?estudianteId={estudianteId}
```

Permisos: `GESTIONAR_CONCILIACIONES` o `CONCLUIR_CONCILIACIONES`.

Alcance: administrador o conciliador asignado.

Reglas:

- conciliación activa y no finalizada;
- consulta asociada operativa;
- estudiante existente, activo y habilitado para conciliación;
- el estado se recalcula según responsables;
- si ya existe reunión, se conserva `REUNION_PROGRAMADA`.

---

## Asignar conciliador

```http
PATCH /api/conciliaciones/{id}/conciliador?conciliadorId={conciliadorId}
```

Permiso: `GESTIONAR_CONCILIACIONES`.

Alcance: administrativo.

Reglas:

- conciliación activa y no finalizada;
- consulta asociada operativa;
- conciliador existente y activo;
- el estado se recalcula según responsables;
- si ya existe reunión, se conserva `REUNION_PROGRAMADA`.

---

## Cambiar estado no final

```http
PATCH /api/conciliaciones/{id}/estado?estado={codigoEstado}
```

Permisos: `GESTIONAR_CONCILIACIONES` o `CONCLUIR_CONCILIACIONES`.

Reglas:

- el código se normaliza antes de buscar el estado;
- no permite estado nulo o vacío;
- no permite cambiar a un estado final;
- no permite fijar manualmente `EN_ESPERA`;
- no permite repetir el estado actual;
- `ESPERANDO_REUNION` exige estudiante y conciliador;
- `REUNION_PROGRAMADA` exige estudiante, conciliador y reunión registrada.

Los estados finales se gestionan por el endpoint de finalización con acta.

---

## Finalizar conciliación

```http
POST /api/conciliaciones/{id}/finalizar
Content-Type: multipart/form-data
```

Permisos: `GESTIONAR_CONCILIACIONES` o `CONCLUIR_CONCILIACIONES`.

Alcance: administrador o conciliador asignado.

Parámetros:

| Nombre | Tipo | Ubicación | Regla |
|---|---|---|---|
| `estado` | `String` | Request param | Debe ser `COMPLETO_CONCILIADO` o `COMPLETO_NO_CONCILIADO`. |
| `acta` | `MultipartFile` | Form-data | PDF obligatorio. |

Reglas:

- conciliación activa y no finalizada;
- consulta asociada no cerrada ni archivada;
- estudiante y conciliador asignados;
- acta PDF válida;
- estado final permitido.

Efectos:

- guarda acta en `conciliacion/{id}/acta.pdf`;
- asigna estado final;
- registra `fechaFinalizacion`;
- cancela notificaciones pendientes de reunión.

---

## Reemplazar solicitud

```http
POST /api/conciliaciones/{id}/solicitud
Content-Type: multipart/form-data
```

Permiso: `GESTIONAR_CONCILIACIONES`.

Alcance: administrativo.

Reglas:

- conciliación activa y no finalizada;
- consulta asociada operativa;
- archivo PDF obligatorio;
- se reemplaza la ruta lógica `conciliacion/{id}/solicitud.pdf`.

---

## Desactivar conciliación

```http
DELETE /api/conciliaciones/{id}
```

Permiso: `GESTIONAR_CONCILIACIONES`.

Alcance: administrativo.

Reglas:

- conciliación activa;
- conciliación no finalizada;
- consulta asociada operativa.

Efectos:

- marca `activo=false`;
- no cambia a estado final;
- no registra acta;
- no registra `fechaFinalizacion`;
- cancela notificaciones pendientes.

---

## Validación de documentos PDF

`ConciliacionDocumentoService` valida documentos de solicitud y acta:

- solicitud obligatoria al crear o reemplazar solicitud;
- acta obligatoria al finalizar;
- extensión `.pdf`;
- si el `contentType` viene informado, debe ser `application/pdf`;
- rutas lógicas:
  - `conciliacion/{id}/solicitud.pdf`;
  - `conciliacion/{id}/acta.pdf`.

---

## Notificaciones de reunión

Al programar o reprogramar reunión, el backend construye destinatarios desde la consulta:

- consultante;
- partes;
- contrapartes.

Los correos se normalizan y se deduplican. Si no hay destinatarios con correo, el sistema genera una alerta administrativa. La programación y reprogramación crean notificaciones inmediatas y recordatorios un día antes de la reunión cuando esa fecha aún está en el futuro.

El scheduler procesa pendientes con cron por defecto:

```text
0 0 * * * *
```

Esto corresponde al minuto cero de cada hora en la configuración por defecto.
