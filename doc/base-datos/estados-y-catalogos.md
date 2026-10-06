# Estados y catálogos

Este documento describe estados funcionales, enums y catálogos usados por el sistema. La información fue contrastada con enums, entidades JPA, controladores y servicios del código fuente actual.

## 1. Estado funcional y activo lógico

El sistema separa dos conceptos:

| Concepto | Propósito |
|---|---|
| Estado funcional | Representa el ciclo operativo o resultado del recurso. |
| Activo lógico | Representa disponibilidad o desactivación sin eliminar historial. |

Ejemplos:

- `Consulta.estado` define si la consulta está pendiente, activa, cerrada o archivada;
- `Proceso.estado` define si está pendiente o tiene resultado final;
- `Seguimiento.estado` define si está pendiente, completado o cancelado;
- `SeguimientoRespuesta.estado` define revisión de respuesta;
- `Conciliacion.estado` se relaciona con catálogo `estado_conciliacion`;
- `activo` se usa en catálogos, perfiles, procesos, seguimientos, respuestas y conciliaciones.

## 2. Estados de consulta

Enum:

```text
EstadoConsulta
```

Valores:

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Estado inicial o de espera administrativa. |
| `ACTIVO` | Consulta en atención activa. |
| `EN_PROCESO` | Consulta en gestión operativa. |
| `URGENTE` | Consulta priorizada. |
| `CERRADO` | Consulta cerrada funcionalmente. |
| `ARCHIVADO` | Consulta archivada para histórico. |

Reglas implementadas:

- toda consulta nueva se crea en `PENDIENTE`;
- el estado no se cambia mediante edición general de datos;
- para entrar en estados operativos se validan responsables mínimos cuando aplica;
- `CERRADO` bloquea operaciones operativas;
- `ARCHIVADO` excluye la consulta de operaciones activas;
- una consulta cerrada solo puede archivarse;
- desarchivar devuelve la consulta a `CERRADO`;
- cerrar exige resultado o conclusión final;
- cerrar exige ausencia de procesos, seguimientos, respuestas, notificaciones y conciliaciones pendientes.

## 3. Estados de proceso

Enum:

```text
EstadoProceso
```

Valores:

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Proceso sin resultado final. |
| `SENTENCIA_FAVORABLE` | Resultado final favorable. |
| `SENTENCIA_DESFAVORABLE` | Resultado final desfavorable. |
| `DESISTIMIENTO` | Terminación por desistimiento. |
| `RECHAZO` | Terminación por rechazo. |
| `PRESCRIPCION` | Terminación por prescripción. |

Reglas implementadas:

- el proceso se crea inicialmente en `PENDIENTE`;
- `EstadoProceso.esFinal()` retorna verdadero para todo estado distinto de `PENDIENTE`;
- un proceso pendiente bloquea cierre de consulta;
- `numero_radicado` puede estar vacío mientras el proceso esté `PENDIENTE`;
- un estado final exige radicado válido;
- si el radicado se informa, conserva unicidad y longitud validada por el backend.

## 4. Estados de seguimiento

Enum:

```text
EstadoSeguimiento
```

Valores:

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Seguimiento por atender. |
| `COMPLETADO` | Seguimiento finalizado funcionalmente. |
| `CANCELADO` | Seguimiento cancelado. |

Reglas implementadas:

- un seguimiento nuevo se crea `PENDIENTE`;
- solo seguimientos pendientes son editables;
- seguimientos pendientes bloquean cierre de consulta;
- cambiar a estados no pendientes aplica efectos sobre notificaciones pendientes;
- la eliminación lógica conserva trazabilidad.

## 5. Estados de respuesta de seguimiento

Enum:

```text
EstadoRespuestaSeguimiento
```

Valores:

| Estado | Uso funcional |
|---|---|
| `PENDIENTE` | Respuesta enviada por estudiante pendiente de revisión. |
| `APROBADA` | Respuesta aprobada. |
| `RECHAZADA` | Respuesta rechazada con observación de revisión. |

Reglas implementadas:

- la decisión de revisión solo admite `APROBADA` o `RECHAZADA`;
- una respuesta rechazada exige `observacion_revision`;
- la observación se limita a la longitud definida por el validador;
- respuestas pendientes bloquean cierre de consulta.

## 6. Estados de conciliación

Entidad catálogo:

```text
EstadoConciliacion
```

Tabla:

```text
estado_conciliacion
```

Códigos técnicos usados por backend:

| Código | Uso funcional |
|---|---|
| `EN_ESPERA` | Conciliación en espera de asignación o condiciones iniciales. |
| `ESPERANDO_REUNION` | Conciliación con responsables que requiere programación de reunión. |
| `REUNION_PROGRAMADA` | Conciliación con reunión programada. |
| `COMPLETO_CONCILIADO` | Conciliación finalizada con acuerdo. |
| `COMPLETO_NO_CONCILIADO` | Conciliación finalizada sin acuerdo. |

Características:

- el backend valida estados por `codigo`, no por nombre visible;
- el código se normaliza mediante `EstadoConciliacionCodigo.normalizar`;
- el catálogo incluye `nombre`, `activo` y `orden`;
- los estados finales se aplican mediante flujo de finalización con acta;
- conciliaciones en estados pendientes bloquean cierre de consulta;
- al finalizar o desactivar conciliación se cancelan notificaciones pendientes de reunión.

## 7. Eventos y notificaciones de reunión de conciliación

Enums:

```text
TipoEventoReunionConciliacion
TipoDestinatarioReunionConciliacion
MomentoNotificacionReunionConciliacion
MotivoNotificacionReunionConciliacion
```

Valores observados:

| Enum | Valores |
|---|---|
| `TipoEventoReunionConciliacion` | `PROGRAMACION`, `REPROGRAMACION`. |
| `TipoDestinatarioReunionConciliacion` | `CONSULTANTE`, `PARTE`, `CONTRAPARTE`, `ADMINISTRATIVO`. |
| `MomentoNotificacionReunionConciliacion` | `INMEDIATA`, `RECORDATORIO`. |
| `MotivoNotificacionReunionConciliacion` | `PROGRAMACION`, `REPROGRAMACION`, `ERROR_ENVIO`. |

Uso:

- programación y reprogramación generan historial;
- notificaciones inmediatas se envían al programar o reprogramar;
- recordatorios se programan antes de la reunión;
- alertas administrativas se crean cuando no hay destinatarios con correo o cuando ocurre error de envío.

## 8. Notificaciones de seguimiento

Enums:

```text
TipoNotificacionSeguimiento
MomentoNotificacionSeguimiento
```

Valores:

| Enum | Valores |
|---|---|
| `TipoNotificacionSeguimiento` | `PARTES`, `ESTUDIANTE`, `ALERTA_DISCIPLINARIA`, `AUTOR`. |
| `MomentoNotificacionSeguimiento` | `INMEDIATA`, `RECORDATORIO`. |

Uso:

- `PARTES` notifica persona principal, partes y contrapartes;
- `ESTUDIANTE` requiere consulta con estudiante activo asignado;
- `ALERTA_DISCIPLINARIA` notifica destinatarios administrativos;
- `AUTOR` recuerda a la persona que creó el seguimiento;
- `RECORDATORIO` se calcula con `fecha_entrega - dias_notificacion`.

## 9. Tipos de perfil

Enum:

```text
TipoPerfilUsuario
```

Valores:

```text
ASESOR
ESTUDIANTE
MONITOR
ADMINISTRATIVO
CONCILIADOR
```

Uso:

- se almacena en `UsuarioSistema.tipo_perfil_actual`;
- define el perfil operativo vigente;
- se usa por estrategias de cambio de perfil;
- se usa por estrategias de resolución de perfil activo;
- se relaciona con roles mediante `Rol.tipo_perfil`.

## 10. Tipo de conciliador

Enum:

```text
TipoConciliador
```

Valores:

```text
INTERNO
EXTERNO
```

Uso:

- clasifica el perfil de conciliador;
- se almacena en la entidad `Conciliador`.

## 11. Catálogos jurídicos

| Catálogo | Entidad | Relación principal |
|---|---|---|
| Área | `Area` | Área jurídica de consulta y asesor. |
| Tema | `Tema` | Pertenece a un área. |
| Tipo | `Tipo` | Pertenece a un tema. |
| Órgano de control | `OrganoControl` | Agrupa especialidades. |
| Especialidad | `Especialidad` | Pertenece a órgano de control. |
| Categoría de seguimiento | `CategoriaSeguimiento` | Clasifica seguimientos. |

Reglas implementadas:

- un tema pertenece al área seleccionada;
- un tipo pertenece al tema seleccionado;
- el asesor asignado debe pertenecer al área de consulta;
- el estudiante se valida respecto de su asesor y área;
- una especialidad pertenece a un órgano de control.

## 12. Catálogos personales y geográficos

| Catálogo | Entidad |
|---|---|
| Tipo de documento | `TipoDocumento` |
| Tipo de persona | `TipoPersona` |
| Condición | `Condicion` |
| Ocupación | `Ocupacion` |
| Empresa | `Empresa` |
| Nacionalidad | `Nacionalidad` |
| Departamento | `Departamento` |
| Municipio | `Municipio` |
| Barrio | `Barrio` |
| Sede | `Sede` |

Uso:

- normalizan formularios de persona y perfiles;
- permiten filtrar datos activos;
- conservan histórico cuando están referenciados por entidades ya registradas.

## 13. Catálogos y activo lógico

Los catálogos usan `activo` para controlar disponibilidad. Un catálogo inactivo puede mantenerse referenciado por información histórica, pero deja de estar disponible para nuevas operaciones según los servicios y endpoints activos.

## 14. Resumen de estados que bloquean cierre de consulta

Una consulta no se cierra si existen recursos pendientes asociados:

| Recurso | Condición documentada por código |
|---|---|
| Proceso | Proceso activo en `PENDIENTE`. |
| Seguimiento | Seguimiento activo en `PENDIENTE`. |
| Respuesta de seguimiento | Respuesta activa en `PENDIENTE`. |
| Notificación de seguimiento | Notificación activa no enviada. |
| Conciliación | Conciliación activa en estado pendiente. |

Además, el cierre exige resultado funcional en `Consulta.resultado`.

## 15. Estados y catálogos de gestión documental

### Estados de ciclo de vida de archivo (`FileAssetStatus`)

Enum:

```text
FileAssetStatus
```

Valores y propósito funcional:

| Estado | Uso funcional |
|---|---|
| `PENDING` | Archivo registrado en staging temporal a la espera de que el cliente complete la subida física al bucket. |
| `ACTIVE` | Archivo verificado y confirmado físicamente en el almacenamiento de objetos. |
| `VIGENTE` | Versión documental activa y válida actualmente para el documento lógico. Solo puede existir una versión en este estado por documento lógico. |
| `HISTORICO` | Versión previa que fue reemplazada por una versión superior ($N+1$). Es de solo lectura e inmutable. |
| `ANULADO` | Documento que fue revocado o dado de baja lógicamente por un usuario autorizado. |
| `FAILED` | Fallo confirmado durante la subida, validación de tamaño o suma de verificación SHA-256. |
| `DELETE_PENDING` | Objeto marcado para limpieza asíncrona o compensación por fallo en la transacción de base de datos. |

### Tipos de recurso documental (`FileResourceType`)

Enum:

```text
FileResourceType
```

Valores admitidos:

| Recurso | Entidad de negocio asociada | Alcance y autorización |
|---|---|---|
| `CONSULTA` | `Consulta` | Validado mediante `ConsultaAccessService.validarPuedeVerConsulta`. |
| `SEGUIMIENTO` | `Seguimiento` | Validado contra la consulta raíz del seguimiento. |
| `RESPUESTA` | `SeguimientoRespuesta` | Validado contra el seguimiento y la consulta raíz asociada. |
| `PROCESO` | `Proceso` | Validado contra la consulta raíz que originó el proceso judicial. |
| `CONCILIACION` | `Conciliacion` | Validado contra la consulta jurídica remitida a conciliación. |

### Origen del documento (`OrigenDocumento`)

Valores admitidos (calculados exclusivamente en el servidor):

| Origen | Descripción |
|---|---|
| `CARGA_USUARIO` | Archivo cargado interactivamente por un usuario autenticado en sesión mediante la interfaz web o API. |
| `SISTEMA` | Documento generado automáticamente por servicios del backend (ej. reportes estadísticos, actas generadas). |
| `MIGRADO` | Documento heredado de versiones anteriores del sistema o migrado desde cargas históricas. |

### Clasificación por tipo documental

Clasificaciones funcionales estandarizadas:

- `CONSULTA_ANEXO`: Documentos probatorios o anexos de la consulta inicial.
- `SEGUIMIENTO_DOCUMENTO`: Evidencias o requerimientos adjuntos a actuaciones de seguimiento.
- `RESPUESTA_DOCUMENTO`: Archivos de soporte de respuestas enviadas por estudiantes.
- `PROCESO_DOCUMENTO`: Piezas procesales, providencias, memoriales o demandas de procesos judiciales.
- `CONCILIACION_SOLICITUD`: Formato o solicitud formal de audiencia de conciliación.
- `CONCILIACION_ACTA`: Acta de acuerdo, no acuerdo o constancia de conciliación.
- `GENERAL`: Archivos generales del caso sin clasificación específica.

