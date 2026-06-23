# Reglas de negocio - Permisos, roles, usuarios y perfiles

> Documento validado contra el código fuente actualizado del sistema. La documentación describe únicamente comportamiento implementado en backend.


## 1. Principio general

El sistema combina autorización por permisos con alcance funcional por perfil. Esto permite que una cuenta de usuario acceda únicamente a las operaciones autorizadas por su rol y dentro del contexto operativo que le corresponde.

La autorización se expresa mediante:

- anotaciones `@PreAuthorize` en controllers;
- permisos constantes definidos en `PermisoNombre`;
- servicios de acceso por módulo;
- resolución del perfil activo del usuario autenticado.

---

## 2. Rol, permiso y perfil

- El **permiso** representa una capacidad puntual del sistema.
- El **rol** agrupa permisos y puede estar asociado a un tipo de perfil.
- El **usuario del sistema** conserva el rol actual, estado activo y tipo de perfil vigente.
- El **perfil operativo** representa la identidad funcional del usuario dentro del consultorio.

---

## 3. Estado activo

Roles, permisos, usuarios y perfiles manejan estado lógico `activo`.

Reglas implementadas:

- un usuario inactivo no puede ser tratado como usuario operativo;
- un perfil inactivo no se resuelve como perfil activo del usuario;
- la desactivación de perfil sincroniza el usuario del sistema asociado cuando se hace desde gestión directa del perfil;
- el cambio de perfil desactiva el perfil anterior y activa o crea el perfil destino.

---

## 4. Cambio de perfil

El cambio de perfil valida:

1. usuario del sistema existente;
2. usuario activo;
3. rol actual activo;
4. tipo de perfil destino informado;
5. datos de cambio obligatorios;
6. perfil destino diferente al perfil actual;
7. rol destino existente, activo y compatible con el tipo de perfil destino;
8. motivo del cambio obligatorio;
9. duplicados en el perfil destino.

El flujo usa Strategy para:

- crear o actualizar el perfil destino;
- desactivar el perfil anterior;
- resolver el perfil activo del usuario autenticado.

---

## 5. Protección de responsables

No se desactiva asesor, estudiante o monitor cuando existen consultas operativas asociadas.

Estados operativos:

- `PENDIENTE`;
- `ACTIVO`;
- `EN_PROCESO`;
- `URGENTE`.

Esta regla aplica en:

- cambio de estado del perfil;
- eliminación lógica del perfil;
- cambio de perfil cuando el perfil anterior es asesor, estudiante o monitor.

---

## 6. Reglas por controller

Los controllers usan permisos específicos para cada módulo. Ejemplos:

- `GESTIONAR_USUARIOS` para operaciones generales de usuarios;
- `VER_USUARIOS` para consulta de usuarios;
- `CAMBIAR_ESTADO_USUARIOS` para activar o desactivar usuarios del sistema;
- `ASIGNAR_ROL_USUARIOS` para cambio de perfil;
- `GESTIONAR_ROLES`, `CREAR_ROLES`, `EDITAR_ROLES`, `VER_ROLES` para roles;
- `GESTIONAR_PERMISOS`, `ASIGNAR_PERMISOS_A_ROLES` para permisos;
- permisos específicos por perfiles para administrativos, asesores, monitores, estudiantes y conciliadores.

---

## 7. Alcance por servicios de acceso

Los servicios de acceso complementan los permisos con reglas de alcance. La arquitectura usa servicios específicos como:

- `ConsultaAccessService`;
- `ProcesoAccessService`;
- `SeguimientoAccessService`;
- `ConciliacionAccessService`;
- `AdministrativoAccessService`;
- `AsesorMonitorAccessService`;
- `EstudianteAccessService`;
- `ConciliadorAccessService`.

Esta separación permite que los controllers se mantengan declarativos y que la lógica contextual quede en servicios especializados.

---

## 8. Estrategias documentadas

El sistema aplica el patrón Strategy en perfiles mediante:

- `PerfilCambioHandler` para perfil destino;
- `PerfilEstadoHandler` para desactivar perfil anterior;
- `PerfilUsuarioActivoResolver` para resolver perfil vigente.

Cada estrategia corresponde a un tipo de perfil y se registra en su respectivo registry.


---

## 11. Reglas precisas por flujo de perfiles y seguridad

### 11.1 Administrativos

La gestión de administrativos requiere rol administrador y perfil administrativo activo con `directora=true`. El permiso del controller habilita la entrada al endpoint, pero la regla de directora se valida dentro de `AdministrativoAccessService`.

### 11.2 Estudiantes

El sistema diferencia permiso de consulta y alcance funcional. Un administrador ve el conjunto permitido por el endpoint. Un asesor ve únicamente estudiantes asociados a su perfil. Otros perfiles no reciben resultados cuando no tienen alcance funcional.

El endpoint de estudiantes habilitados para conciliación retorna estudiantes activos con `conciliacion=true` y se usa como catálogo de apoyo para conciliaciones, aplicando la visibilidad definida por el servicio de acceso.

### 11.3 Estado de perfil y cuenta de acceso

Cuando se cambia el estado desde el perfil operativo, el backend sincroniza el estado del `UsuarioSistema` asociado. Cuando se cambia directamente el estado de `UsuarioSistema`, el backend modifica la cuenta de acceso y no reactiva o desactiva automáticamente el perfil real.

### 11.4 Cambio de perfil

El cambio de perfil exige usuario activo, perfil anterior activo, rol destino activo y coherente con el tipo de perfil destino, motivo obligatorio y datos requeridos por el handler concreto.

El correo del perfil destino proviene del `username` de `UsuarioSistema`. El perfil anterior se desactiva sin desactivar la cuenta. El perfil destino se crea o se reutiliza si ya pertenece al mismo usuario.

### 11.5 Roles y permisos

Un rol nuevo debe recibir permisos activos. En actualización, enviar `permisoIds` reemplaza la asignación completa de permisos; omitirlo conserva los permisos existentes. Los permisos se administran por estado lógico y no por eliminación física expuesta en API.
