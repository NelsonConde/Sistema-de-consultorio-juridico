# Entidades principales

Este documento describe las entidades persistentes principales del sistema. La estructura está organizada por dominios funcionales y se basa en las entidades JPA del código fuente actual.

## 1. Seguridad y acceso

### `usuario_sistema`

Entidad:

```text
UsuarioSistema
```

Tabla:

```text
usuario_sistema
```

Propósito:

Representa al usuario autenticable del sistema. Es la identidad de acceso usada por seguridad, roles, permisos, autenticación y resolución del perfil activo.

Campos principales:

| Columna | Uso |
|---|---|
| `id` | Identificador del usuario del sistema. |
| `username` | Usuario de acceso, con longitud máxima de 120 caracteres y valor único. |
| `password_hash` | Hash BCrypt de la contraseña usado para verificación de credenciales. |
| `activo` | Estado lógico del usuario. |
| `tipo_perfil_actual` | Tipo de perfil operativo vigente del usuario. |
| `rol_id` | Rol funcional asignado. |

Relaciones:

```text
usuario_sistema -> rol
usuario_sistema -> perfil real activo según tipo_perfil_actual
usuario_sistema -> usuario_cambio_perfil_historial
usuario_sistema -> audit_logs como actor textual en auditoría
```

Tipos de perfil admitidos por enum:

```text
ASESOR
ESTUDIANTE
MONITOR
ADMINISTRATIVO
CONCILIADOR
```

Criterio operativo:

- el usuario del sistema es la entidad de autenticación;
- el perfil real aporta datos funcionales del rol humano;
- el sistema resuelve el perfil activo mediante estrategias por tipo de perfil;
- al desactivar o reactivar perfiles desde sus servicios de comando, el estado se sincroniza con `UsuarioSistema`.

### `rol`

Entidad:

```text
Rol
```

Tabla:

```text
rol
```

Propósito:

Agrupa permisos funcionales y se asocia a un tipo de perfil. Permite administrar capacidades del sistema sin acoplarlas a usuarios específicos.

Campos principales:

| Columna | Uso |
|---|---|
| `id` | Identificador del rol. |
| `nombre` | Nombre único del rol. |
| `descripcion` | Descripción funcional. |
| `activo` | Disponibilidad lógica del rol. |
| `tipo_perfil` | Tipo de perfil compatible con el rol. |

Relaciones:

```text
rol -> rol_permiso -> permiso
usuario_sistema -> rol
```

### `permiso`

Entidad:

```text
Permiso
```

Tabla:

```text
permiso
```

Propósito:

Representa una acción funcional o permiso de navegación. Los permisos se asignan a roles y son utilizados por backend y frontend.

Campos principales:

| Columna | Uso |
|---|---|
| `id` | Identificador del permiso. |
| `nombre` | Nombre único del permiso. |
| `descripcion` | Descripción del alcance funcional. |
| `activo` | Disponibilidad lógica. |

Relación:

```text
permiso -> rol_permiso -> rol
```

### `rol_permiso`

Tabla de unión definida en la entidad `Rol`.

Propósito:

Permite asociar múltiples permisos a un rol y múltiples roles a un permiso.

Relación:

```text
rol_permiso.rol_id -> rol.id
rol_permiso.permiso_id -> permiso.id
```

### `usuario_cambio_perfil_historial`

Entidad:

```text
UsuarioCambioPerfilHistorial
```

Propósito:

Registra trazabilidad de cambios de perfil del usuario del sistema.

Campos principales:

| Columna | Uso |
|---|---|
| `usuario_sistema_id` | Usuario afectado. |
| `tipo_perfil_anterior` | Tipo de perfil antes del cambio. |
| `perfil_anterior_id` | Id del perfil anterior. |
| `rol_anterior_id` | Rol anterior cuando aplica. |
| `rol_anterior_nombre` | Nombre del rol anterior para conservar trazabilidad. |
| `tipo_perfil_nuevo` | Tipo de perfil asignado. |
| `perfil_nuevo_id` | Id del perfil nuevo. |
| `rol_nuevo_id` | Rol nuevo cuando aplica. |
| `rol_nuevo_nombre` | Nombre del rol nuevo. |
| `cambiado_por_usuario_id` | Usuario que ejecutó el cambio, cuando está disponible. |
| `cambiado_por_username` | Nombre de usuario que ejecutó el cambio. |
| `motivo` | Motivo del cambio, obligatorio. |
| `fecha_cambio` | Fecha del cambio. |

Relaciones:

```text
usuario_cambio_perfil_historial -> usuario_sistema
usuario_cambio_perfil_historial -> rol anterior
usuario_cambio_perfil_historial -> rol nuevo
usuario_cambio_perfil_historial -> usuario que cambió
```

### `password_reset_token`

Entidad:

```text
PasswordResetToken
```

Propósito:

Soporta el flujo de recuperación y restablecimiento de contraseña.

Campos principales:

| Columna | Uso |
|---|---|
| `token_hash` | Hash del token, único. |
| `usuario_sistema_id` | Usuario propietario del token. |
| `fecha_creacion` | Momento de creación. |
| `fecha_expiracion` | Momento máximo de uso. |
| `usado` | Marca de consumo del token. |
| `fecha_uso` | Fecha en que fue usado, si aplica. |

## 2. Perfiles internos

Los perfiles internos almacenan información funcional de los usuarios del consultorio. Cada perfil puede asociarse a un `UsuarioSistema` mediante relación uno a uno.

### `administrativo`

Entidad:

```text
Administrativo
```

Propósito:

Representa personal administrativo del consultorio jurídico.

Campos principales:

| Columna | Uso |
|---|---|
| `usuario_sistema_id` | Usuario de acceso asociado. |
| `nombre` | Nombre completo. |
| `tipo_documento` | Tipo de documento. |
| `documento` | Documento único. |
| `email` | Correo único. |
| `telefono` | Teléfono único. |
| `usuario` | Usuario interno único. |
| `codigo` | Código único. |
| `sede_id` | Sede asociada. |
| `activo` | Estado lógico del perfil. |
| `directora` | Marca funcional de dirección. |

Relaciones:

```text
administrativo -> usuario_sistema
administrativo -> tipo_documento
administrativo -> sede
```

### `asesor`

Entidad:

```text
Asesor
```

Propósito:

Representa asesor jurídico del consultorio. Participa en consultas, estudiantes y alcance de atención.

Campos principales:

| Columna | Uso |
|---|---|
| `usuario_sistema_id` | Usuario de acceso asociado. |
| `nombre`, `documento`, `email`, `telefono`, `usuario`, `codigo` | Datos únicos del perfil. |
| `tipo_documento` | Catálogo de documento. |
| `sede_id` | Sede del asesor. |
| `area_id` | Área jurídica de atención. |
| `activo` | Disponibilidad lógica. |

Relaciones:

```text
asesor -> usuario_sistema
asesor -> tipo_documento
asesor -> sede
asesor -> area
asesor -> estudiante
asesor -> consulta
```

Regla persistente documentada por servicios:

- un asesor con consultas operativas directas o asociadas a sus estudiantes no se desactiva desde los servicios de comando.

### `monitor`

Entidad:

```text
Monitor
```

Propósito:

Representa monitor del consultorio jurídico.

Relaciones:

```text
monitor -> usuario_sistema
monitor -> tipo_documento
monitor -> sede
monitor -> consulta
```

Regla persistente documentada por servicios:

- un monitor con consultas operativas asignadas no se desactiva desde los servicios de comando.

### `estudiante`

Entidad:

```text
Estudiante
```

Propósito:

Representa estudiante vinculado al consultorio y relacionado con asesor, consultas, seguimientos y conciliaciones.

Campos principales:

| Columna | Uso |
|---|---|
| `usuario_sistema_id` | Usuario de acceso asociado. |
| `asesor_id` | Asesor responsable. |
| `conciliacion` | Indica habilitación para conciliación. |
| `activo` | Disponibilidad lógica. |

Relaciones:

```text
estudiante -> usuario_sistema
estudiante -> tipo_documento
estudiante -> sede
estudiante -> asesor
estudiante -> consulta
estudiante -> seguimiento_respuesta
estudiante -> conciliacion
```

Reglas persistentes documentadas por servicios:

- un estudiante con consultas operativas asignadas no se desactiva desde los servicios de comando;
- `conciliacion` habilita participación en asignación de conciliaciones cuando el flujo lo requiere.

### `conciliador`

Entidad:

```text
Conciliador
```

Propósito:

Representa conciliador interno o externo que puede ser asignado a conciliaciones.

Campos principales:

| Columna | Uso |
|---|---|
| `tipo_conciliador` | Enum `INTERNO` o `EXTERNO`. |
| `activo` | Disponibilidad lógica. |

Relaciones:

```text
conciliador -> usuario_sistema
conciliador -> tipo_documento
conciliador -> sede
conciliador -> conciliacion
```

## 3. Catálogos generales

### `area`

Entidad:

```text
Area
```

Propósito:

Catálogo de áreas jurídicas. Se relaciona con temas, asesores y consultas.

Campos:

| Columna | Uso |
|---|---|
| `nombre` | Nombre único del área. |
| `activo` | Disponibilidad lógica. |

Relaciones:

```text
area -> tema
area -> asesor
area -> consulta
```

### `tema`

Entidad:

```text
Tema
```

Propósito:

Catálogo de temas asociados a un área.

Relación:

```text
tema -> area
tema -> tipo
tema -> consulta
```

Regla aplicada por servicios:

- el tema seleccionado para una consulta debe pertenecer al área de la consulta.

### `tipo`

Entidad:

```text
Tipo
```

Propósito:

Catálogo de tipos asociados a un tema.

Relación:

```text
tipo -> tema
tipo -> consulta
```

Regla aplicada por servicios:

- el tipo seleccionado debe pertenecer al tema de la consulta.

### `sede`

Entidad:

```text
Sede
```

Propósito:

Catálogo de sedes del consultorio y sedes de reunión.

Relaciones:

```text
sede -> perfiles
sede -> consulta
sede -> reunion_conciliacion
sede -> reunion_conciliacion_historial
```

### Catálogos geográficos

Entidades:

```text
Departamento
Municipio
Barrio
Nacionalidad
```

Relaciones:

```text
departamento -> municipio
municipio -> barrio
municipio -> persona
barrio -> persona
nacionalidad -> persona
```

### Catálogos de persona

Entidades:

```text
TipoPersona
Condicion
Ocupacion
Empresa
TipoDocumento
```

Uso:

- tipifican datos personales;
- normalizan formularios;
- permiten mantener consistencia en personas y perfiles.

## 4. Personas

### `persona`

Entidad:

```text
Persona
```

Propósito:

Representa personas atendidas o relacionadas con una consulta jurídica.

Grupos de información observados en la entidad:

| Grupo | Contenido |
|---|---|
| Identificación | tipo de persona, documento, nombres, apellidos, tipo de documento. |
| Contacto | correo, teléfono, dirección y ubicación. |
| Caracterización | nacionalidad, estado civil, escolaridad, grupo étnico, condición actual, lectura/escritura, discapacidad y PCD. |
| Vivienda | estrato, tipo de vivienda, zona, tenencia, servicios. |
| Laboral | ocupación, empresa, salario, cargo, teléfono y dirección de empresa. |
| Acudiente | datos de acudiente cuando aplica. |
| Vinculación | cómo se enteró y relación con universidad. |
| Control | `activo`. |

Relaciones:

```text
persona -> tipo_persona
persona -> tipo_documento
persona -> nacionalidad
persona -> condicion
persona -> municipio
persona -> barrio
persona -> ocupacion
persona -> empresa
persona -> consulta como persona principal
persona -> consulta_parte
persona -> consulta_contraparte
```

## 5. Consultas

### `consulta`

Entidad:

```text
Consulta
```

Propósito:

Representa el caso o atención jurídica principal del sistema.

Campos principales:

| Columna | Uso |
|---|---|
| `fecha` | Fecha de la consulta. |
| `descripcion` | Descripción breve. |
| `hechos` | Hechos del caso. |
| `pretensiones` | Pretensiones de la consulta. |
| `concepto_juridico` | Concepto jurídico registrado. |
| `tramite` | Trámite definido. |
| `observaciones` | Observaciones complementarias. |
| `tipo_violencia` | Clasificación opcional relacionada. |
| `estado` | Estado funcional de la consulta. |
| `resultado` | Conclusión o resultado funcional de cierre. |
| `last_updated_at` | Fecha de última actualización usada también por reportes. |
| `persona_id` | Persona principal. |
| `sede_id` | Sede asociada. |
| `area_id` | Área jurídica. |
| `tema_id` | Tema jurídico. |
| `tipo_id` | Tipo jurídico. |
| `asesor_id` | Asesor asignado. |
| `monitor_id` | Monitor asignado. |
| `estudiante_id` | Estudiante asignado. |

Relaciones:

```text
consulta -> persona principal
consulta -> consulta_parte -> persona
consulta -> consulta_contraparte -> persona
consulta -> sede
consulta -> area
consulta -> tema
consulta -> tipo
consulta -> asesor
consulta -> monitor
consulta -> estudiante
consulta -> proceso
consulta -> seguimiento
consulta -> conciliacion
```

Tablas de relación:

```text
consulta_parte
consulta_contraparte
```

Reglas implementadas por servicios:

- las consultas nuevas se crean en estado `PENDIENTE`;
- el estado no se cambia mediante edición general;
- `CERRADO` y `ARCHIVADO` bloquean operaciones operativas;
- una consulta requiere `resultado` para cerrar;
- una consulta no se cierra con procesos pendientes, seguimientos pendientes, respuestas pendientes, notificaciones pendientes o conciliaciones pendientes;
- una consulta con procesos, seguimientos o conciliaciones activas no permite modificar datos estructurales;
- campos narrativos y complementarios se mantienen editables mientras la consulta permita operación.

## 6. Seguimientos

### `categoria_seguimiento`

Entidad:

```text
CategoriaSeguimiento
```

Propósito:

Catálogo de categorías para seguimiento.

Campos:

| Columna | Uso |
|---|---|
| `nombre` | Nombre único. |
| `activo` | Disponibilidad lógica. |

### `seguimiento`

Entidad:

```text
Seguimiento
```

Propósito:

Representa una tarea, requerimiento o actuación de seguimiento asociada a una consulta.

Campos principales:

| Columna | Uso |
|---|---|
| `descripcion` | Descripción del seguimiento. |
| `fecha_entrega` | Fecha límite o de entrega. |
| `dias_notificacion` | Días previos para recordatorio. |
| `notificar_partes` | Indica notificación a persona principal, partes y contrapartes. |
| `notificar_estudiante` | Indica visibilidad/notificación al estudiante asignado. |
| `alerta_disciplinaria` | Indica alerta administrativa/disciplinaria. |
| `estado` | Estado funcional. |
| `activo` | Desactivación lógica. |
| `categoria_seguimiento` | Categoría asociada. |
| `consulta` | Consulta asociada. |
| `autor` | Usuario que creó el seguimiento. |
| `fecha_creacion` | Fecha de creación. |
| `fecha_actualizacion` | Fecha de actualización. |

Relaciones:

```text
seguimiento -> consulta
seguimiento -> categoria_seguimiento
seguimiento -> usuario_sistema como autor
seguimiento -> seguimiento_respuesta
seguimiento -> seguimiento_notificacion
```

Reglas implementadas por servicios:

- un seguimiento nuevo inicia `PENDIENTE`;
- solo seguimientos pendientes son editables;
- `notificar_estudiante=true` requiere consulta con estudiante activo asignado;
- seguimientos pendientes bloquean cierre de consulta;
- al cancelar o completar se aplican efectos sobre notificaciones pendientes.

### `seguimiento_respuesta`

Entidad:

```text
SeguimientoRespuesta
```

Propósito:

Respuesta del estudiante a un seguimiento visible.

Campos principales:

| Columna | Uso |
|---|---|
| `seguimiento_id` | Seguimiento respondido. |
| `estudiante_id` | Estudiante autor de la respuesta. |
| `contenido` | Contenido de la respuesta. |
| `estado` | Estado de revisión. |
| `fuera_plazo` | Indica si se respondió fuera del plazo. |
| `observacion_revision` | Observación del revisor, obligatoria al rechazar. |
| `revisado_por_id` | Usuario que revisa. |
| `fecha_creacion` | Fecha de creación. |
| `fecha_actualizacion` | Fecha de actualización. |
| `fecha_decision` | Fecha de aprobación o rechazo. |
| `activo` | Estado lógico. |

Relaciones:

```text
seguimiento_respuesta -> seguimiento
seguimiento_respuesta -> estudiante
seguimiento_respuesta -> usuario_sistema como revisor
```

### `seguimiento_notificacion`

Entidad:

```text
SeguimientoNotificacion
```

Propósito:

Registra notificaciones inmediatas, recordatorios y alertas asociadas a seguimientos.

Campos principales:

| Columna | Uso |
|---|---|
| `seguimiento_id` | Seguimiento relacionado. |
| `tipo_notificacion` | `PARTES`, `ESTUDIANTE`, `ALERTA_DISCIPLINARIA` o `AUTOR`. |
| `momento_notificacion` | `INMEDIATA` o `RECORDATORIO`. |
| `fecha_programada` | Fecha programada. |
| `fecha_envio` | Fecha real de envío. |
| `enviada` | Indica si se envió. |
| `intentos` | Número de intentos. |
| `error` | Error de envío. |
| `activa` | Vigencia de la notificación pendiente. |
| `fecha_cancelacion` | Fecha de cancelación. |

Restricción única observada en entidad:

```text
seguimiento_id + tipo_notificacion + momento_notificacion
```

## 7. Procesos

### `proceso`

Entidad:

```text
Proceso
```

Propósito:

Representa un proceso jurídico asociado a una consulta.

Campos principales:

| Columna | Uso |
|---|---|
| `numero_radicado` | Radicado único cuando se informa. Puede estar vacío mientras el proceso está `PENDIENTE`; es obligatorio para estados finales. |
| `departamento_id` | Departamento del proceso. |
| `consulta_id` | Consulta asociada. |
| `organo_control_id` | Órgano de control. |
| `especialidad_id` | Especialidad asociada al órgano de control. |
| `estado` | Estado funcional del proceso. |
| `activo` | Estado lógico. |

Relaciones:

```text
proceso -> consulta
proceso -> departamento
proceso -> organo_control
proceso -> especialidad
```

Reglas implementadas por servicios:

- un proceso nuevo inicia `PENDIENTE`;
- `numero_radicado` puede ser nulo o vacío mientras el proceso esté pendiente;
- si se informa radicado, debe cumplir la longitud definida por el backend y conservar unicidad;
- un estado final exige radicado válido;
- un proceso pendiente bloquea cierre de consulta.

### `organo_control`

Entidad:

```text
OrganoControl
```

Propósito:

Catálogo de órganos de control disponibles para procesos.

Relación:

```text
organo_control -> especialidad
organo_control -> proceso
```

### `especialidad`

Entidad:

```text
Especialidad
```

Propósito:

Catálogo de especialidades asociadas a órganos de control.

Relación:

```text
especialidad -> organo_control
especialidad -> proceso
```

Regla implementada:

- una especialidad pertenece a un órgano de control específico.

## 8. Conciliación

### `conciliacion`

Entidad:

```text
Conciliacion
```

Propósito:

Representa trámite de conciliación asociado a una consulta jurídica.

Campos principales:

| Columna | Uso |
|---|---|
| `consulta_id` | Consulta origen. |
| `estudiante_id` | Estudiante asignado. |
| `conciliador_id` | Conciliador asignado. |
| `estado_id` | Estado de conciliación. |
| `fecha_conciliacion` | Campo heredado de fecha de conciliación. |
| `documento_solicitud_path` | Ruta documental de solicitud. |
| `acta_path` | Ruta documental de acta de finalización. |
| `solicitado_por_id` | Usuario que solicitó la conciliación. |
| `fecha_creacion` | Fecha de creación. |
| `fecha_actualizacion` | Fecha de actualización. |
| `fecha_finalizacion` | Fecha funcional de finalización. |
| `activo` | Estado lógico. |

Relaciones:

```text
conciliacion -> consulta
conciliacion -> estudiante
conciliacion -> conciliador
conciliacion -> estado_conciliacion
conciliacion -> usuario_sistema como solicitante
conciliacion -> reunion_conciliacion
conciliacion -> reunion_conciliacion_historial
conciliacion -> reunion_conciliacion_notificacion
```

Reglas implementadas por servicios:

- la conciliación nace desde una consulta;
- no se crea sobre consulta cerrada o archivada;
- la solicitud PDF se almacena al crear;
- los estados finales se aplican mediante endpoint de finalización con acta;
- al finalizar o desactivar conciliación se cancelan notificaciones pendientes de reunión;
- conciliaciones pendientes bloquean cierre de consulta.

### `estado_conciliacion`

Entidad:

```text
EstadoConciliacion
```

Propósito:

Catálogo funcional de estados de conciliación.

Campos:

| Columna | Uso |
|---|---|
| `codigo` | Código técnico único. |
| `nombre` | Nombre visible. |
| `activo` | Disponibilidad lógica. |
| `orden` | Orden de presentación. |

Códigos técnicos usados por backend:

```text
EN_ESPERA
ESPERANDO_REUNION
REUNION_PROGRAMADA
COMPLETO_CONCILIADO
COMPLETO_NO_CONCILIADO
```

### `reunion_conciliacion`

Entidad:

```text
ReunionConciliacion
```

Propósito:

Representa la reunión vigente asociada a una conciliación.

Campos principales:

| Columna | Uso |
|---|---|
| `conciliacion_id` | Identificador y relación hacia conciliación. |
| `fecha_reunion` | Fecha y hora vigente. |
| `sede_id` | Sede de reunión. |
| `observaciones` | Observaciones opcionales. |
| `fecha_creacion` | Fecha de creación. |
| `fecha_actualizacion` | Fecha de actualización. |

Relación:

```text
reunion_conciliacion -> conciliacion
reunion_conciliacion -> sede
```

### `reunion_conciliacion_historial`

Entidad:

```text
ReunionConciliacionHistorial
```

Propósito:

Registra programación y reprogramación de reuniones.

Campos principales:

| Columna | Uso |
|---|---|
| `conciliacion_id` | Conciliación afectada. |
| `tipo_evento` | `PROGRAMACION` o `REPROGRAMACION`. |
| `fecha_reunion_anterior` | Fecha anterior cuando aplica. |
| `fecha_reunion_nueva` | Nueva fecha. |
| `sede_anterior_id` | Sede anterior. |
| `sede_nueva_id` | Nueva sede. |
| `realizado_por_id` | Usuario que realizó el cambio. |
| `fecha_evento` | Fecha del evento. |

### `reunion_conciliacion_notificacion`

Entidad:

```text
ReunionConciliacionNotificacion
```

Propósito:

Registra notificaciones inmediatas, recordatorios y alertas administrativas de reuniones de conciliación.

Campos principales:

| Columna | Uso |
|---|---|
| `conciliacion_id` | Conciliación relacionada. |
| `tipo_destinatario` | `CONSULTANTE`, `PARTE`, `CONTRAPARTE` o `ADMINISTRATIVO`. |
| `motivo` | `PROGRAMACION`, `REPROGRAMACION` o `ERROR_ENVIO`. |
| `momento_notificacion` | `INMEDIATA` o `RECORDATORIO`. |
| `destinatario_email` | Correo destino. |
| `destinatario_nombre` | Nombre destino. |
| `fecha_programada` | Fecha programada. |
| `fecha_envio` | Fecha de envío. |
| `enviada` | Estado de envío. |
| `intentos` | Número de intentos. |
| `error` | Error cuando aplica. |
| `activa` | Vigencia de la notificación. |
| `fecha_cancelacion` | Fecha de cancelación. |

## 9. Auditoría

### `audit_logs`

Entidad:

```text
AuditLog
```

Propósito:

Registra acciones auditables ejecutadas por servicios anotados con `@Auditable`.

Campos principales:

| Columna | Uso |
|---|---|
| `id` | Identificador. |
| `created_date` | Fecha de creación del registro de auditoría. |
| `username` | Usuario asociado a la acción. |
| `action` | Acción auditada. |
| `entity_name` | Entidad lógica auditada. |
| `entity_id` | Identificador afectado cuando está disponible. |
| `details` | Detalle adicional. |

Características:

- la entidad usa listener de auditoría de Spring Data;
- los campos principales no se actualizan después de registrados;
- la consulta se expone mediante módulo de auditoría.

## 10. Estadísticas

El módulo de estadísticas no define una tabla principal propia para reportes. Sus resultados se calculan desde entidades operativas existentes.

Fuentes principales:

```text
consulta
proceso
seguimiento
conciliacion
perfiles internos
catálogos relacionados
```

Servicios relacionados:

```text
EstadisticasQueryService
EstadisticasRangoQueryService
EstadisticasPerfilQueryService
EstadisticasMapperService
EstadisticasPdfService
```

Criterio documental:

- las estadísticas derivan de datos persistidos en módulos operativos;
- `Consulta.lastUpdatedAt` participa en consultas de rango y reportes;
- los PDF estadísticos se generan como salida del servicio, no como entidad persistente principal documentada aquí.

## 11. Archivos

El almacenamiento de archivos se maneja mediante servicio de archivos. La base de datos conserva rutas en entidades cuando el archivo forma parte del flujo de negocio.

Ejemplos:

| Entidad | Campo documental |
|---|---|
| `Conciliacion` | `documento_solicitud_path`. |
| `Conciliacion` | `acta_path`. |

El módulo de archivos también permite carga, descarga, listado y validación de rutas desde el backend.

## 12. Tablas con desactivación lógica

Usan `activo` o equivalente lógico:

- catálogos;
- personas;
- perfiles;
- roles;
- permisos;
- procesos;
- seguimientos;
- respuestas de seguimiento;
- notificaciones de seguimiento;
- conciliaciones;
- notificaciones de reunión;
- usuarios del sistema.

## 13. Tablas con estado funcional

Usan campo `estado` o relación a estado:

- `consulta.estado`;
- `proceso.estado`;
- `seguimiento.estado`;
- `seguimiento_respuesta.estado`;
- `conciliacion.estado_id`.

## 14. Relaciones transversales principales

```text
usuario_sistema -> rol -> rol_permiso -> permiso
usuario_sistema -> perfil actual según tipo_perfil_actual
perfil interno -> usuario_sistema
asesor -> estudiante
consulta -> persona principal
consulta -> partes y contrapartes
consulta -> sede, área, tema y tipo
consulta -> asesor, monitor y estudiante
consulta -> proceso
consulta -> seguimiento
consulta -> conciliacion
seguimiento -> respuesta
seguimiento -> notificacion
conciliacion -> estado_conciliacion
conciliacion -> reunion vigente
conciliacion -> historial de reunión
conciliacion -> notificacion de reunión
audit_logs registra acciones de servicios auditables
```

## 15. Consideraciones de integridad

La integridad se protege mediante:

- relaciones JPA;
- campos obligatorios en entidades;
- validación de catálogos activos;
- validación de jerarquía área-tema-tipo;
- validación de responsables asignados;
- bloqueo de operaciones sobre consultas cerradas o archivadas;
- bloqueo de cierre de consulta con pendientes;
- bloqueo de cambio estructural de consulta con actividad asociada;
- validación condicional de radicado en procesos;
- sincronización entre perfil y usuario del sistema;
- estrategias para cambio y resolución de perfil activo;
- auditoría de acciones relevantes.
