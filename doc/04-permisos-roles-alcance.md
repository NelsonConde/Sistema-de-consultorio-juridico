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

## Navegación frontend por permisos

El frontend implementa navegación mediante `PermissionSidebar`. La visibilidad de opciones de menú depende de permisos del usuario autenticado.

La visibilidad en frontend mejora la experiencia de usuario, pero la autorización final siempre la valida el backend.

## Cambio de perfil y permisos

El cambio de perfil se gestiona desde usuarios del sistema. El backend usa estrategias para construir el nuevo perfil, desactivar el perfil anterior y resolver el perfil activo vigente.

La operación conserva historial de cambio de perfil mediante `UsuarioCambioPerfilHistorial` y servicios asociados.

## Estado operativo del perfil

Los perfiles tienen estado activo. El backend sincroniza el estado del perfil con el `UsuarioSistema` cuando un perfil se desactiva o reactiva desde los servicios de perfiles.

Además, asesor, estudiante y monitor no pueden desactivarse si tienen consultas operativas vivas asociadas. Esta regla protege la continuidad operativa de las consultas.

## Estadísticas y reportes

El módulo de estadísticas usa:

- `Ver reportes` para reportes globales y PDF;
- `Ver consultas` para semestres disponibles y estadísticas por perfil.

Esto permite separar reportes administrativos de estadísticas relacionadas con perfiles operativos.
