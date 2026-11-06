# Permisos, roles y alcance

## Modelo general

El sistema separa:

- usuario de acceso (`UsuarioSistema`);
- rol (`Rol`);
- permisos (`Permiso`);
- perfil operativo actual (`TipoPerfilUsuario` y perfil activo asociado);
- reglas de alcance por módulo.

Un usuario autenticado opera con los permisos de su rol y con el alcance propio de su perfil activo.

## Roles y permisos

Los permisos están centralizados en la clase `PermisoNombre`. Esto evita repetir cadenas de texto en controllers y services.

El backend expone endpoints para gestionar:

```text
/api/roles
/api/permisos
/api/usuarios-sistema
```

Los roles pueden tener permisos asociados o removidos mediante endpoints específicos del `RolController`.

## Categorías de permisos implementadas

### Navegación

Permisos usados para mostrar secciones principales del frontend:

- `Acceder inicio`;
- `Acceder recepción`;
- `Acceder tareas`;
- `Acceder nueva consulta`;
- `Acceder consultas jurídicas`;
- `Acceder administración`;
- `Acceder roles`;
- `Acceder estudiantes`;
- `Acceder asesores y monitores`;
- `Acceder personas`;
- `Acceder eliminación`;
- `Acceder conciliaciones`;
- `Acceder procesos`.

### Catálogos y personas

Incluye permisos para consultar catálogos, gestionar catálogos, ver personas, crear personas, editar personas y cambiar estado de personas.

### Consultas

Incluye permisos para ver, crear, editar, cambiar estado, archivar y asignar responsables de consulta.

La reasignación individual usa exclusivamente `ASIGNAR_RESPONSABLES_CONSULTA` en `PATCH /api/consultas/{id}/responsable`. `ConsultaController` aplica el permiso mediante `@PreAuthorize` y `ConsultaAccessService.validarPuedeReasignarResponsable` vuelve a validarlo junto con el alcance sobre la consulta. El perfil estudiante no puede asignar responsables aunque su rol tuviera el permiso.

### Seguimientos

Incluye permisos para ver, crear, editar, eliminar, responder, aprobar respuestas, ver alertas disciplinarias y gestionar categorías de seguimiento.

### Usuarios, roles y permisos

Incluye permisos para ver, crear, editar, cambiar estado y asignar rol a usuarios; también permisos para ver, crear, editar roles y asignar permisos a roles.

### Perfiles

El código define permisos para estudiantes, asesores, monitores, conciliadores y administradores. Algunos módulos distinguen permisos de consulta y permisos de gestión.

### Conciliaciones

Incluye permisos para ver conciliaciones, gestionar conciliaciones, programar reuniones, reprogramar reuniones y concluir conciliaciones.

### Procesos

Incluye permisos para ver procesos y gestionar procesos.

### Reportes

El permiso `Ver reportes` habilita reportes globales de estadísticas. Algunos reportes por perfil usan además `Ver consultas`.

## Autorización en controllers

Los controllers usan `@PreAuthorize`. La autorización se expresa con permisos constantes, por ejemplo:

```text
hasAuthority('Ver consultas')
hasAuthority('Gestionar procesos')
hasAnyAuthority('Ver personas', 'Gestionar personas')
```

La autenticación HTTP es stateless: `JwtAuthenticationFilter` obtiene el token de la cookie `access_token`. Las solicitudes que modifican estado también requieren protección CSRF; el cliente solicita el token en `GET /api/auth/csrf`, conserva las cookies y envía el valor en el nombre de header retornado por ese endpoint.

## Alcance por servicios

El backend no depende únicamente de la visibilidad del frontend. Cada módulo sensible tiene servicios de acceso que validan si el usuario puede operar el recurso específico.

Servicios de acceso observados:

- `ConsultaAccessService`;
- `ProcesoAccessService`;
- `SeguimientoAccessService`;
- `SeguimientoRespuestaAccessService`;
- `ConciliacionAccessService`;
- servicios de acceso de perfiles y personas.

Estos servicios permiten reglas como:

- validar si el usuario puede crear, editar o cambiar estado;
- validar si puede asignar responsables;
- validar si puede ver u operar recursos según su perfil;
- validar si puede gestionar perfiles administrativos, asesores, monitores, estudiantes o conciliadores.

Para reasignaciones, el alcance se calcula antes de modificar la consulta con las relaciones vigentes:

- administrador: alcance global sujeto al permiso;
- estudiante: únicamente consultas donde está asignado, aunque no puede reasignar;
- asesor: consultas donde está asignado o donde el estudiante pertenece a su asesoría;
- monitor: consultas donde está asignado;
- conciliador: sin alcance general sobre consultas.

Después de una reasignación, las lecturas vuelven a calcular el alcance desde los responsables persistidos. Esto evita conservar acceso por una asignación anterior y no concede acceso a consultas no relacionadas.

## Navegación frontend por permisos

El frontend implementa navegación mediante `PermissionSidebar`. La visibilidad de opciones de menú depende de permisos del usuario autenticado.

La visibilidad en frontend mejora la experiencia de usuario, pero la autorización final siempre la valida el backend.

## Cambio de perfil y permisos

El cambio de perfil se gestiona desde usuarios del sistema. El backend usa estrategias para construir el nuevo perfil, desactivar el perfil anterior y resolver el perfil activo vigente.

La operación conserva historial de cambio de perfil mediante `UsuarioCambioPerfilHistorial` y servicios asociados.

## Estado operativo del perfil

La política abarca ADMINISTRATIVO, ASESOR, ESTUDIANTE, MONITOR y CONCILIADOR:

| Regla | Resultado |
|---|---|
| Una cuenta conserva un solo perfil operativo activo. | Los perfiles anteriores se mantienen como registros históricos inactivos. |
| Las operaciones directas de perfil no cambian cuenta, rol, permisos, `tipoPerfilActual` ni historial. | El estado de cuenta y el estado de perfil son independientes. |
| La activación directa de un perfil histórico se rechaza. | Para recuperarlo como perfil vigente se usa el cambio explícito. |
| La desactivación o eliminación lógica directa de un perfil histórico se rechaza. | No afecta la cuenta ni al perfil actual. |
| `UsuarioSistema.activo` se administra por separado. | Habilitar la cuenta no activa un perfil y activar un perfil no habilita la cuenta. |
| Los cambios de tipo usan la operación explícita. | Se preservan validaciones, transacción, rol, perfil actual e historial. |
| El perfil actual no puede desactivarse o eliminarse lógicamente de forma directa. | Para impedir acceso se suspende la cuenta; para cambiar de cargo se usa el cambio explícito. |

Las operaciones incompatibles responden `409 Conflict` con `detalles.codigo = CONFLICTO_OPERACION_PERFIL` y no producen efectos parciales. Si hay varios perfiles activos preexistentes, se rechaza la operación sin corrección automática. Activar directamente el perfil actual solo se permite si no hay otro perfil operativo activo.

Además, asesor, estudiante y monitor no pueden desactivarse si tienen consultas operativas vivas asociadas. Esta regla protege la continuidad operativa de las consultas.

## Estadísticas y reportes

El módulo de estadísticas usa:

- `Ver reportes` para reportes globales y PDF;
- `Ver consultas` para semestres disponibles y estadísticas por perfil.

Esto permite separar reportes administrativos de estadísticas relacionadas con perfiles operativos.
