# Backend - Consultas jurídicas

El módulo de consultas jurídicas administra el registro, búsqueda, actualización, cambio de estado, archivo y desarchivo de las consultas atendidas por el consultorio jurídico. En el código fuente actual, la consulta funciona como entidad central del flujo operativo y se relaciona con personas, catálogos jurídicos, responsables internos, procesos, seguimientos, respuestas, notificaciones y conciliaciones.

Este documento describe la implementación backend vigente del módulo, tomando como referencia los controladores, DTOs, entidades, servicios, validadores, repositorios y pruebas unitarias existentes en el código fuente.

## Paquetes principales

```text
business/controller/consulta
business/dto/consulta
business/model/consulta
business/repository/consulta
business/service/acceso/consulta
business/service/consulta
business/service/consulta/consulta
```

## Componentes principales

| Componente | Responsabilidad |
|---|---|
| `ConsultaController` | Expone los endpoints HTTP del módulo bajo `/api/consultas`. |
| `ConsultaService` | Fachada del módulo; delega lectura a `ConsultaQueryService` y escritura a `ConsultaCommandService`. |
| `ConsultaCommandService` | Orquesta creación, actualización, cambio de estado, archivo y desarchivo. |
| `ConsultaQueryService` | Gestiona búsquedas, listados, consulta por id y listados archivados según alcance del usuario. |
| `ConsultaConstruccionService` | Aplica datos del DTO sobre la entidad y normaliza textos. |
| `ConsultaRelacionService` | Resuelve personas, catálogos y perfiles activos asociados a la consulta. |
| `ConsultaEstadoService` | Valida transiciones de estado, cierre, archivo, desarchivo y pendientes operativos. |
| `ConsultaActividadService` | Determina si la consulta ya tiene procesos, seguimientos o conciliaciones activas asociadas. |
| `ConsultaCambioEstructuralValidator` | Protege datos estructurales cuando la consulta ya tiene actividad asociada. |
| `ConsultaValidator` | Centraliza reglas de negocio de campos, estado, coherencia de dominio y responsables. |
| `ConsultaMapper` | Convierte `Consulta` a `ConsultaDTO` y `ConsultaBusquedaDTO`. |
| `ConsultaAccessService` | Valida permisos y alcance de usuario sobre consultas. |
| `ConsultaRepository` | Expone consultas JPA, búsquedas por alcance, destinatarios de notificación y consultas estadísticas. |

## Entidad principal

La entidad principal es `Consulta`, persistida en la tabla:

```text
consulta
```

Campos principales implementados:

| Campo | Tipo funcional | Uso |
|---|---|---|
| `id` | Identificador | Identificador interno de la consulta. |
| `fecha` | Fecha | Fecha asociada a la consulta. |
| `descripcion` | Texto corto | Descripción resumida de la consulta. |
| `hechos` | Texto amplio | Relato de hechos. |
| `pretensiones` | Texto amplio | Pretensiones de la persona consultante. |
| `conceptoJuridico` | Texto amplio | Concepto jurídico registrado. |
| `tramite` | Texto corto | Trámite o ruta de atención. |
| `observaciones` | Texto amplio | Observaciones complementarias. |
| `tipoViolencia` | Texto corto | Clasificación adicional cuando aplica. |
| `estado` | Enum `EstadoConsulta` | Estado funcional de la consulta. |
| `resultado` | Texto corto | Resultado o conclusión funcional de cierre. |
| `lastUpdatedAt` | Fecha técnica | Fecha de última actualización usada también en consultas estadísticas. |
| `persona` | Relación `ManyToOne` | Persona principal o solicitante. |
| `partes` | Relación `ManyToMany` | Personas relacionadas como partes adicionales. |
| `contrapartes` | Relación `ManyToMany` | Personas relacionadas como contrapartes. |
| `sede` | Relación `ManyToOne` | Sede de recepción o atención. |
| `area` | Relación `ManyToOne` | Área jurídica. |
| `tema` | Relación `ManyToOne` | Tema jurídico asociado al área. |
| `tipo` | Relación `ManyToOne` | Tipo jurídico asociado al tema. |
| `asesor` | Relación `ManyToOne` | Asesor responsable. |
| `monitor` | Relación `ManyToOne` | Monitor responsable. |
| `estudiante` | Relación `ManyToOne` | Estudiante responsable. |

Las relaciones de partes y contrapartes se almacenan mediante tablas intermedias:

```text
consulta_parte
consulta_contraparte
```

El campo `lastUpdatedAt` se actualiza automáticamente mediante métodos `@PrePersist` y `@PreUpdate`.

## Estados de consulta

La consulta usa el enum `EstadoConsulta`.

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
| `PENDIENTE` | Estado inicial de toda consulta nueva. |
| `ACTIVO` | Consulta habilitada para atención activa. |
| `EN_PROCESO` | Consulta con gestión operativa en curso. |
| `URGENTE` | Consulta priorizada dentro del flujo operativo. |
| `CERRADO` | Consulta finalizada funcionalmente. |
| `ARCHIVADO` | Consulta conservada para consulta histórica. |

## DTOs del módulo

### `ConsultaDTO`

DTO principal de entrada y salida para creación, edición, consulta por id y cambio de estado cuando retorna detalle.

Campos implementados:

| Campo | Validación o uso |
|---|---|
| `id` | No debe enviarse en creación; si se envía en actualización debe coincidir con la ruta. |
| `fecha` | Obligatoria. |
| `descripcion` | Obligatoria; máximo 500 caracteres. |
| `hechos` | Obligatorio. |
| `pretensiones` | Obligatorio. |
| `conceptoJuridico` | Obligatorio. |
| `tramite` | Obligatorio; máximo 100 caracteres. |
| `observaciones` | Opcional. |
| `tipoViolencia` | Opcional; máximo 100 caracteres. |
| `estado` | No se modifica desde edición general. En creación, si se informa, debe ser `PENDIENTE`. |
| `resultado` | Opcional durante la operación; obligatorio para cerrar la consulta. Máximo 100 caracteres. |
| `personaId` | Obligatorio. |
| `partesIds` | Lista opcional de personas adicionales. |
| `contrapartesIds` | Lista opcional de contrapartes. |
| `sedeId` | Obligatorio. |
| `areaId` | Obligatorio. |
| `temaId` | Obligatorio. |
| `tipoId` | Opcional. |
| `asesorId` | Opcional; su asignación depende de permiso específico. |
| `monitorId` | Opcional; su asignación depende de permiso específico. |
| `estudianteId` | Opcional; su asignación depende de permiso específico. |

### `ConsultaBusquedaDTO`

DTO resumido usado por el buscador de consultas.

| Campo | Uso |
|---|---|
| `id` | Identificador de consulta. |
| `consulta` | Descripción de la consulta. |
| `fecha` | Fecha de la consulta. |
| `nombre` | Nombres de la persona principal. |
| `apellido` | Apellidos de la persona principal. |
| `cedula` | Número de documento de la persona principal. |
| `estado` | Estado actual de la consulta. |

## Controlador HTTP

El controlador `ConsultaController` expone endpoints bajo:

```text
/api/consultas
```

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/consultas?search=` | Busca consultas según el alcance del usuario autenticado. |
| `GET` | `/api/consultas/{id}` | Obtiene el detalle de una consulta. |
| `POST` | `/api/consultas` | Crea una nueva consulta. |
| `PUT` | `/api/consultas/{id}` | Actualiza datos generales de una consulta. |
| `PATCH` | `/api/consultas/{id}/estado?estado=` | Cambia el estado funcional de una consulta. |
| `DELETE` | `/api/consultas/{id}` | Conservado por compatibilidad; archiva lógicamente la consulta. |
| `PATCH` | `/api/consultas/{id}/archivar` | Archiva una consulta cerrada. |
| `GET` | `/api/consultas/archivadas` | Lista consultas archivadas. |
| `PATCH` | `/api/consultas/{id}/desarchivar` | Retorna una consulta archivada al estado `CERRADO`. |

## Permisos usados

| Permiso | Uso en el módulo |
|---|---|
| `VER_CONSULTAS` | Buscar y consultar detalle dentro del alcance del usuario. |
| `CREAR_CONSULTAS` | Crear consultas. |
| `EDITAR_CONSULTAS` | Actualizar datos generales de consultas. |
| `CAMBIAR_ESTADO_CONSULTAS` | Cambiar estado funcional. |
| `ARCHIVAR_CONSULTAS` | Archivar, desarchivar y consultar consultas archivadas. |
| `ASIGNAR_RESPONSABLES_CONSULTA` | Asignar o modificar asesor, estudiante o monitor. |
| `GESTIONAR_CONSULTAS` | Permiso amplio aceptado para operaciones generales del módulo. |

## Alcance por perfil

La búsqueda y la consulta por id se filtran desde backend. El frontend no define el alcance final de los datos.

| Perfil | Alcance implementado |
|---|---|
| Administrador | Accede a todas las consultas no archivadas según permisos. |
| Estudiante | Accede a consultas donde está asignado como estudiante. |
| Asesor | Accede a consultas donde está asignado directamente o donde el estudiante asignado pertenece a su asesoría. |
| Monitor | Accede a consultas donde está asignado como monitor. |
| Conciliador | No recibe resultados desde el buscador general de consultas. |

Las consultas en estado `ARCHIVADO` se excluyen del buscador general y se consultan mediante endpoint especializado.

El detalle por id valida permisos y alcance sobre la consulta solicitada. El listado operativo excluye archivadas, mientras que el listado de archivadas se gestiona con endpoint especializado y política de archivo.

## Creación de consulta

El flujo de creación se implementa en `ConsultaCommandService.crear`.

Secuencia principal:

1. Valida permiso para crear consulta.
2. Valida que el DTO no envíe `id`.
3. Valida campos obligatorios.
4. Valida que, si el DTO trae estado, sea `PENDIENTE`.
5. Evalúa si el DTO solicita asignación de responsables.
6. Valida permiso de asignación cuando aplica.
7. Construye la entidad mediante `ConsultaConstruccionService`.
8. Asigna estado inicial `PENDIENTE`.
9. Valida coherencia de dominio.
10. Guarda y retorna `ConsultaDTO`.

Toda consulta nueva queda inicialmente en estado `PENDIENTE`.

## Actualización de consulta

El flujo de actualización se implementa en `ConsultaCommandService.actualizar`.

Reglas aplicadas:

- requiere permiso de edición;
- valida alcance sobre la consulta;
- no permite actualizar consultas cerradas o archivadas;
- valida campos obligatorios;
- si el DTO trae `id`, debe coincidir con el id de la ruta;
- el estado no se cambia desde edición general;
- si cambian responsables, se exige permiso `ASIGNAR_RESPONSABLES_CONSULTA`;
- si la consulta tiene actividad asociada, se protegen los datos estructurales;
- la actualización usa el DTO completo de consulta, no un parche parcial de campos;
- después de aplicar datos se valida la coherencia completa de dominio.

## Protección de datos estructurales

El módulo implementa protección de trazabilidad mediante `ConsultaActividadService` y `ConsultaCambioEstructuralValidator`.

Una consulta se considera con actividad asociada cuando existe al menos uno de estos elementos activos:

- proceso asociado;
- seguimiento asociado;
- conciliación asociada.

Cuando la consulta ya tiene actividad asociada, se protegen los siguientes datos estructurales:

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

Esta protección permite conservar la consistencia entre la consulta y las actuaciones posteriores registradas en el sistema.

Los cambios de asesor, estudiante o monitor se evalúan como parte de la protección estructural cuando el usuario tiene permiso de asignación de responsables. Si el usuario no posee ese permiso, el backend no aplica esos campos desde el DTO.

## Coherencia de dominio

`ConsultaValidator.validarCoherenciaDominio` valida:

- el tema pertenece al área seleccionada;
- el tipo pertenece al tema cuando se informa;
- el asesor pertenece al área de la consulta;
- el estudiante pertenece al asesor asignado cuando ambos existen;
- el asesor del estudiante pertenece al área de la consulta;
- la persona principal no se repite como parte ni contraparte;
- una persona no aparece simultáneamente como parte y contraparte;
- no existen duplicados dentro de partes ni contrapartes.

## Asignación de responsables

La consulta puede relacionarse con asesor, estudiante y monitor.

Reglas implementadas:

- asignar o modificar responsables requiere permiso `ASIGNAR_RESPONSABLES_CONSULTA`;
- el estudiante no puede asignar responsables;
- los responsables deben existir y estar activos;
- si se asigna estudiante sin asesor explícito, el backend toma el asesor activo asociado al estudiante;
- si el asesor asociado al estudiante no existe o está inactivo, la operación se rechaza;
- para pasar a `ACTIVO`, `EN_PROCESO` o `URGENTE`, la consulta debe tener asesor y estudiante asignados.

## Cambio de estado

El cambio de estado se realiza por endpoint específico. La edición general no modifica `estado`.

Reglas implementadas:

- el estado destino es obligatorio;
- no se permite cambiar al mismo estado actual;
- una consulta archivada no se modifica como consulta operativa;
- una consulta cerrada solo puede archivarse;
- el estudiante no puede cambiar estado;
- el cambio requiere permiso funcional y alcance;
- los estados `ACTIVO`, `EN_PROCESO` y `URGENTE` requieren asesor y estudiante;
- el cierre valida resultado y ausencia de pendientes operativos.

## Cierre de consulta

El cierre se valida en `ConsultaEstadoService` cuando el estado destino es `CERRADO`.

Para cerrar una consulta se exige:

- resultado o conclusión final registrada en `resultado`;
- ausencia de procesos activos en estado `PENDIENTE`;
- ausencia de seguimientos activos en estado `PENDIENTE`;
- ausencia de respuestas activas de seguimiento en estado `PENDIENTE`;
- ausencia de notificaciones activas de seguimiento sin enviar;
- ausencia de conciliaciones activas en estados no finalizados.

Los estados de conciliación que bloquean cierre son:

```text
EN_ESPERA
ESPERANDO_REUNION
REUNION_PROGRAMADA
```

## Archivo y desarchivo

El archivado se implementa mediante:

- `DELETE /api/consultas/{id}`;
- `PATCH /api/consultas/{id}/archivar`.

En ambos casos la operación lleva la consulta a `ARCHIVADO` y conserva la información histórica.

Reglas de archivo:

- solo se archivan consultas en estado `CERRADO`;
- el usuario debe tener permiso `ARCHIVAR_CONSULTAS`;
- la política de acceso exige rol administrador;
- el sistema vuelve a validar pendientes operativos como defensa de consistencia.

El desarchivo se implementa con:

```text
PATCH /api/consultas/{id}/desarchivar
```

El desarchivo retorna la consulta a `CERRADO`. No reabre la operación activa.

## Búsqueda y carga de relaciones

La búsqueda principal compara el término con:

- descripción de la consulta;
- nombres de la persona principal;
- apellidos de la persona principal;
- número de documento de la persona principal.

Para consulta por id, `ConsultaRepository` carga partes y contrapartes en consultas separadas mediante `findByIdConPartes` y `findByIdConContrapartes`. Esta separación evita problemas de carga simultánea de múltiples colecciones en Hibernate.

## Uso como fuente de destinatarios de notificación

El repositorio de consultas también expone consultas de apoyo para obtener destinatarios asociados a una consulta. Estas consultas permiten al módulo de seguimientos construir destinatarios de correo a partir de la persona principal, partes, contrapartes y estudiante activo asignado, sin exponer endpoints adicionales desde el módulo de consultas.

## Relación con estadísticas

La entidad `Consulta` contiene el campo `lastUpdatedAt`, actualizado automáticamente en persistencia y actualización. Este campo no hace parte de los datos de entrada de `ConsultaDTO`; se gestiona desde la entidad con callbacks de persistencia y sirve como referencia temporal para reportes. El repositorio de consultas incluye consultas nativas usadas por el módulo de estadísticas para agrupar consultas por semestre, rango de fechas, estado, área, tipo de violencia y datos de personas atendidas.

La documentación detallada del módulo de estadísticas se desarrolla en su bloque específico.

## Pruebas relacionadas

El código fuente incluye pruebas unitarias orientadas a reglas críticas del módulo:

| Prueba | Cobertura principal |
|---|---|
| `ConsultaEstadoServiceTest` | Cierre con resultado, bloqueo por pendientes, archivo, desarchivo y operaciones sobre cerradas o archivadas. |
| `ConsultaCambioEstructuralValidatorTest` | Protección de datos estructurales cuando hay actividad asociada. |
| `ConsultaResponsableOperacionServiceTest` | Validación de responsables con consultas operativas vivas. |
