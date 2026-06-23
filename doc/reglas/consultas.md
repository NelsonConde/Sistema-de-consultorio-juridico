# Reglas de negocio - Consultas jurídicas

El módulo de consultas jurídicas administra el ciclo principal de atención del consultorio jurídico. Las reglas descritas en este documento corresponden a las validaciones implementadas en el código fuente actual para creación, edición, cambio de estado, cierre, archivo, desarchivo, alcance y protección de trazabilidad.

## Principios del módulo

- Toda consulta nueva inicia en estado `PENDIENTE`.
- El estado funcional se modifica únicamente mediante endpoint específico de cambio de estado.
- La edición general de datos no modifica el estado de la consulta.
- Las consultas cerradas o archivadas no permiten operaciones operativas.
- El archivado conserva la consulta como historial mediante estado `ARCHIVADO`.
- El cierre exige resultado o conclusión final.
- El cierre valida que no existan pendientes operativos asociados.
- El backend filtra las consultas según permisos y alcance del perfil autenticado.
- Si una consulta ya tiene actividad asociada, el backend protege sus datos estructurales.

## Estados de consulta

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Estado inicial de revisión o asignación. |
| `ACTIVO` | Consulta habilitada para atención activa. |
| `EN_PROCESO` | Consulta con gestión operativa en curso. |
| `URGENTE` | Consulta priorizada. |
| `CERRADO` | Consulta finalizada funcionalmente. |
| `ARCHIVADO` | Consulta conservada para consulta histórica. |

## Creación de consulta

Reglas implementadas:

- el usuario debe tener permiso para crear consulta;
- no se permite enviar `id` en creación;
- `fecha`, `descripcion`, `hechos`, `pretensiones`, `conceptoJuridico`, `tramite`, `personaId`, `sedeId`, `areaId` y `temaId` son obligatorios;
- si se envía `estado`, debe ser `PENDIENTE`;
- el backend asigna estado `PENDIENTE` a toda consulta nueva;
- las relaciones principales deben existir y estar activas;
- se valida coherencia de área, tema y tipo;
- se valida coherencia de responsables cuando se asignan;
- se valida que persona principal, partes y contrapartes no se repitan indebidamente.

## Actualización de consulta

Reglas implementadas:

- requiere permiso de edición;
- el usuario debe tener alcance sobre la consulta;
- la consulta no puede estar cerrada ni archivada;
- si el DTO trae `id`, debe coincidir con el identificador de la ruta;
- el estado no puede cambiarse desde edición general;
- si se cambian responsables, se requiere permiso `ASIGNAR_RESPONSABLES_CONSULTA`;
- se resuelven nuevamente las relaciones activas;
- se valida coherencia completa de dominio después de aplicar los cambios;
- si la consulta tiene actividad asociada, se bloquea el cambio de datos estructurales.

## Actividad asociada y datos estructurales

La consulta tiene actividad asociada cuando existe al menos uno de estos elementos activos:

- proceso asociado;
- seguimiento asociado;
- conciliación asociada.

Cuando existe actividad asociada, el backend protege estos datos estructurales:

| Dato estructural | Motivo funcional |
|---|---|
| Persona principal | Mantiene la identidad de la atención jurídica registrada. |
| Partes | Mantiene consistencia con actuaciones posteriores. |
| Contrapartes | Mantiene consistencia con procesos, seguimientos o conciliaciones. |
| Sede | Mantiene la referencia institucional del caso. |
| Área | Mantiene la clasificación jurídica bajo la cual se gestionó el caso. |
| Tema | Mantiene coherencia con el área y la gestión posterior. |
| Tipo | Mantiene coherencia con el tema jurídico. |
| Asesor | Mantiene trazabilidad del responsable académico. |
| Estudiante | Mantiene trazabilidad del responsable estudiantil. |
| Monitor | Mantiene trazabilidad de apoyo o seguimiento. |

Los campos narrativos o complementarios continúan gestionándose mediante la edición general mientras la consulta se mantenga operativa.

## Cambio de estado

Reglas implementadas:

- el estado destino es obligatorio;
- no se permite cambiar al mismo estado actual;
- una consulta archivada no se modifica como consulta operativa;
- una consulta cerrada solo puede archivarse;
- el estudiante no puede cambiar el estado de consulta;
- se valida permiso funcional y alcance;
- los estados de atención requieren responsables mínimos;
- cerrar o archivar valida pendientes operativos.

## Estados que requieren responsables

Para pasar a cualquiera de estos estados:

```text
ACTIVO
EN_PROCESO
URGENTE
```

la consulta debe tener:

- asesor asignado;
- estudiante asignado.

También se valida:

- el asesor pertenece al área de la consulta;
- el estudiante pertenece al asesor asignado cuando ambos existen;
- el asesor del estudiante pertenece al área de la consulta.

## Responsables internos

Responsables posibles:

- asesor;
- monitor;
- estudiante.

Reglas implementadas:

- asignar o cambiar responsables requiere permiso `ASIGNAR_RESPONSABLES_CONSULTA`;
- el estudiante no puede asignar responsables;
- los responsables deben existir y estar activos;
- si se asigna estudiante sin asesor explícito, el backend toma el asesor activo asociado al estudiante;
- si el asesor asociado al estudiante no existe o está inactivo, la operación se rechaza;
- el asesor asignado debe pertenecer al área de la consulta;
- el estudiante asignado debe corresponder al asesor cuando ambos están definidos.

## Persona principal, partes y contrapartes

Reglas implementadas:

- la persona principal es obligatoria;
- la persona principal no puede repetirse como parte adicional;
- la persona principal no puede repetirse como contraparte;
- una persona no puede estar al mismo tiempo como parte y contraparte;
- no se permiten duplicados dentro de partes;
- no se permiten duplicados dentro de contrapartes;
- todas las personas relacionadas deben existir y estar activas.

## Catálogos jurídicos

La consulta se relaciona con:

- sede;
- área;
- tema;
- tipo.

Reglas implementadas:

- sede, área y tema son obligatorios;
- el tipo es opcional;
- sede, área, tema y tipo deben estar activos cuando se asignan;
- el tema debe pertenecer al área;
- el tipo, si se informa, debe pertenecer al tema.

## Cierre de consulta

Una consulta puede pasar a `CERRADO` cuando cumple las reglas de estado y no tiene pendientes operativos asociados.

Reglas implementadas para cierre:

- debe existir resultado o conclusión final en `resultado`;
- no debe tener procesos activos en estado `PENDIENTE`;
- no debe tener seguimientos activos en estado `PENDIENTE`;
- no debe tener respuestas activas de seguimiento en estado `PENDIENTE`;
- no debe tener notificaciones activas de seguimiento sin enviar;
- no debe tener conciliaciones activas en estados no finalizados.

Estados de conciliación que bloquean cierre:

```text
EN_ESPERA
ESPERANDO_REUNION
REUNION_PROGRAMADA
```

## Archivo de consulta

Reglas implementadas:

- una consulta solo se archiva si está `CERRADO`;
- el archivado requiere permiso `ARCHIVAR_CONSULTAS` y política interna de administrador;
- la política de acceso exige rol administrador;
- el sistema vuelve a validar pendientes operativos antes de archivar;
- el endpoint `DELETE /api/consultas/{id}` funciona como archivado lógico;
- `PATCH /api/consultas/{id}/archivar` archiva y retorna DTO;
- el estado `ARCHIVADO` excluye la consulta del buscador operativo general;
- los datos asociados se conservan para historial.

## Desarchivo de consulta

Reglas implementadas:

- solo se desarchivan consultas en estado `ARCHIVADO`;
- desarchivar devuelve la consulta a `CERRADO`;
- desarchivar no reabre operación activa;
- se validan pendientes operativos como defensa de consistencia.

## Alcance de consulta

| Perfil | Alcance implementado |
|---|---|
| Administrador | Accede a todas las consultas según permisos. |
| Estudiante | Accede a consultas donde está asignado como estudiante. |
| Asesor | Accede a consultas donde está asignado como asesor o donde el estudiante pertenece a su asesoría. |
| Monitor | Accede a consultas donde está asignado como monitor. |
| Conciliador | No accede desde el buscador general de consultas. |

## Búsqueda

La búsqueda general excluye consultas archivadas y compara el término contra:

- descripción de consulta;
- nombres de la persona principal;
- apellidos de la persona principal;
- número de documento de la persona principal.

El backend devuelve únicamente las consultas permitidas para el usuario autenticado.

## Reglas para integración frontend

- usar el buscador general y confiar en el alcance aplicado por backend;
- no modificar `estado` desde edición general;
- usar `PATCH /api/consultas/{id}/estado` para cambio de estado;
- guardar `resultado` antes de cerrar una consulta;
- manejar mensajes de pendientes operativos cuando el backend bloquea cierre o archivo;
- usar endpoints de archivo/desarchivo para ciclo histórico;
- enviar responsables solo desde interfaces con permiso de asignación;
- preservar los datos estructurales cuando la consulta ya tiene actividad asociada.

## Pruebas unitarias asociadas

El módulo cuenta con pruebas orientadas a reglas críticas:

| Prueba | Regla validada |
|---|---|
| `ConsultaEstadoServiceTest` | Cierre con resultado, bloqueo por pendientes, archivo, desarchivo y operaciones sobre cerradas o archivadas. |
| `ConsultaCambioEstructuralValidatorTest` | Protección de datos estructurales cuando existe actividad asociada. |
| `ConsultaResponsableOperacionServiceTest` | Bloqueo de desactivación de responsables con consultas operativas vivas. |
