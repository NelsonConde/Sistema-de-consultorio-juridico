# Frontend - Módulo de usuarios, roles, permisos y perfiles

## 1. Propósito del módulo

Este módulo reúne las pantallas frontend relacionadas con creación de usuarios, administración de roles y permisos, cambio de perfil, auditoría administrativa y gestión visual de perfiles internos del consultorio jurídico.

El frontend separa estas funcionalidades en varias rutas y componentes:

```text
/roles
/admin
/estudiantes
/asesoresymonitores
```

## 2. Rutas y componentes

| Ruta | Componente principal | Propósito |
|---|---|---|
| `/roles` | `UsuarioSistemaForm` | Creación de usuarios del sistema y perfiles asociados. |
| `/admin` | `RolePermissionsForm`, `CambiarRolUsuarioForm`, `AuditLogsTable`, catálogos | Administración de permisos, cambio de rol/perfil y auditoría. |
| `/estudiantes` | `EstudiantesForm` | Consulta y cambio de estado de estudiantes visibles por alcance. |
| `/asesoresymonitores` | `AsesoresYMonitoresForm` | Consulta y cambio de estado de asesores y monitores. |

## 3. Archivos relacionados

```text
src/components/forms/AdminUsuarios/UsuarioSistemaForm.jsx
src/components/forms/AdminUsuarios/RolePermissionsForm.jsx
src/components/forms/AdminUsuarios/CambiarRolUsuarioForm.jsx
src/components/forms/AdminUsuarios/AuditLogsTable.jsx
src/components/forms/usuarios/EstudiantesForm.jsx
src/components/forms/usuarios/ImportarEstudiantesForm.jsx
src/components/forms/usuarios/AsesoresYMonitoresForm.jsx
src/components/forms/usuarios/ConciliadorForm.jsx
src/app/(dashboard)/roles/page.js
src/app/(dashboard)/admin/page.js
src/app/(dashboard)/estudiantes/page.js
src/app/(dashboard)/asesoresymonitores/page.js
src/lib/permission.js
src/lib/authz.js
```

## 4. Navegación y permisos operativos

La navegación lateral y los componentes aplican controles diferenciados. `PermissionSidebar` decide qué rutas aparecen en el menú; cada formulario consulta `/api/auth/me` y evalúa los permisos necesarios para sus operaciones visibles.

### Visibilidad en navegación

| Ruta | Permiso de navegación configurado |
|---|---|
| `/roles` | `Acceder roles` |
| `/admin` | `Acceder administración` |
| `/estudiantes` | `Acceder estudiantes` |
| `/asesoresymonitores` | `Acceder asesores y monitores` |

### Validaciones de componentes

| Componente | Condición frontend para operar |
|---|---|
| `UsuarioSistemaForm` | Permite crear perfiles cuando el usuario tiene `Crear usuarios`, el permiso compatible `Gestionar usuarios` o `Gestionar administradores`, según el tipo seleccionado. |
| `CambiarRolUsuarioForm` | Permite operar con `Asignar rol usuarios` o `Gestionar usuarios`. |
| `RolePermissionsForm` | Requiere `Acceder roles` y `Asignar permisos a roles`. |
| `EstudiantesForm` | Requiere `Acceder estudiantes` y `Ver estudiantes`; muestra la desactivación con `Cambiar estado estudiantes`. |
| `AsesoresYMonitoresForm` | Requiere `Acceder asesores y monitores` y `Ver asesores y monitores`; muestra la desactivación con `Gestionar asesores y monitores`. |

La autorización de cada petición se aplica nuevamente en los controllers backend correspondientes.

## 5. Creación de usuarios del sistema

La ruta `/roles` renderiza `UsuarioSistemaForm`. El componente consulta la sesión actual y habilita el formulario cuando el usuario puede crear perfiles o gestionar administrativos.

### Tipos seleccionables y endpoints

El formulario construye la ruta de creación a partir del valor plural del tipo de perfil seleccionado:

| Tipo visible | Valor interno del selector | Endpoint consumido |
|---|---|---|
| Administrativo | `administrativos` | `POST /api/administrativos` |
| Asesor | `asesores` | `POST /api/asesores` |
| Estudiante | `estudiantes` | `POST /api/estudiantes` |
| Monitor | `monitores` | `POST /api/monitores` |
| Conciliador | `conciliadores` | `POST /api/conciliadores` |

La opción **Administrativo** se incorpora al selector cuando el usuario tiene `Gestionar administradores`. Para los demás tipos, el envío se habilita cuando el usuario tiene `Crear usuarios` o el permiso compatible `Gestionar usuarios`.

### Catálogos auxiliares

Cuando el usuario tiene `Ver catálogos` o `Gestionar catálogos`, el formulario intenta cargar:

```text
GET /api/tipos-documento/activos
GET /api/sedes
GET /api/areas
GET /api/asesores/activos
```

Estos datos permiten diligenciar los selectores del formulario según el perfil seleccionado.

### Campos enviados

El formulario base utiliza:

```text
nombre
tipoDocumentoId
documento
email
telefono
usuario
codigo
sedeId
```

El payload se completa según el tipo:

| Perfil seleccionado | Campos adicionales visibles y enviados |
|---|---|
| Administrativo | Checkbox `directora`. |
| Asesor | Selector obligatorio `areaId`. |
| Estudiante | Selector obligatorio `asesorId` y checkbox `conciliacion`. |
| Monitor | No agrega campo específico adicional. |
| Conciliador | Selector obligatorio `tipoConciliador`, con opciones `INTERNO` y `EXTERNO`. |

Antes de enviar, el componente establece `activo=true`, convierte identificadores seleccionados a número y valida que el tipo de documento informado corresponda a las opciones cargadas.

## 6. Importación masiva de estudiantes

Cuando el tipo seleccionado es `estudiantes`, `UsuarioSistemaForm` muestra dos modos:

```text
Crear uno
Cargue masivo
```

La pestaña de cargue masivo renderiza `ImportarEstudiantesForm` cuando el usuario tiene `Crear usuarios` o el permiso compatible `Gestionar usuarios`.

### Archivo y plantilla

`ImportarEstudiantesForm` ofrece la acción visible:

```text
Descargar plantilla
```

La acción genera localmente en el navegador el archivo:

```text
plantilla_importacion_estudiantes.csv
```

con encabezados y una fila de ejemplo.

Para la importación, el selector y la zona de arrastre aceptan archivos cuyo nombre termina en:

```text
.xlsx
.xls
```

### Petición

```text
POST /api/estudiantes/importar
Content-Type: multipart/form-data
```

Campo enviado:

```text
archivo
```

La petición transporta sesión con `credentials: "include"`.

### Resultados presentados

| Respuesta recibida | Presentación implementada |
|---|---|
| `200 OK` con `fallidos=0` | Conserva el resultado y muestra notificación de importación exitosa. |
| `200 OK` con filas exitosas y fallidas | Conserva conteos y errores por fila, y muestra notificación de importación parcial. |
| `200 OK` con todas las filas fallidas | Conserva el detalle y muestra notificación de error. |
| `400 Bad Request` | Lee el texto retornado y lo presenta como error de formato. |
| `403 Forbidden` | Muestra notificación de ausencia de permiso para importar estudiantes. |
| Otra respuesta no exitosa | Procesa el cuerpo recibido y presenta el mensaje de error disponible. |
| Error de red | Muestra notificación de error de conexión. |

## 7. Gestión de roles y permisos

`RolePermissionsForm` se encuentra en `/admin`, dentro de la pestaña `Permisos`. Para operar, consulta la sesión y exige:

```text
Acceder roles
Asignar permisos a roles
```

Endpoints usados:

```text
GET /api/roles/activos
GET /api/permisos/activos
GET /api/permisos
POST /api/permisos
GET /api/roles/{id}
PATCH /api/roles/{rolId}/permisos/{permisoId}
DELETE /api/roles/{rolId}/permisos/{permisoId}
```

El formulario organiza permisos por páginas del sistema. Al marcar una página, calcula los permisos de navegación y operación asociados; al guardar, agrega o remueve las asociaciones correspondientes y conserva permisos no gestionados por el formulario.

Cuando el rol seleccionado corresponde al rol del usuario autenticado, el componente conserva seleccionada la página `/admin` y muestra el mensaje:

```text
No puedes quitar el acceso a Administración de tu propio rol.
```

## 8. Cambio de rol y perfil

`CambiarRolUsuarioForm` se encuentra en `/admin` y permite operar cuando el usuario tiene:

```text
Asignar rol usuarios
o
Gestionar usuarios
```

Carga inicial:

```text
GET /api/usuarios-sistema/activos
GET /api/roles/activos
GET /api/tipos-documento/activos
GET /api/sedes
GET /api/asesores/activos
GET /api/areas
```

El selector trabaja con usuarios activos y excluye de los destinos el perfil actual del usuario seleccionado. La opción de destino administrativo solo se muestra con `Gestionar administradores`.

### Consulta de datos actuales

Para consultar la información del perfil vigente utiliza rutas plurales:

```text
GET /api/administrativos/{perfilId}
GET /api/asesores/{perfilId}
GET /api/estudiantes/{perfilId}
GET /api/monitores/{perfilId}
GET /api/conciliadores/{perfilId}
```

### Cambio de perfil

Para cambiar el perfil utiliza rutas singulares bajo `/api/usuarios-sistema`:

```text
PATCH /api/usuarios-sistema/{usuarioId}/perfil/administrativo
PATCH /api/usuarios-sistema/{usuarioId}/perfil/asesor
PATCH /api/usuarios-sistema/{usuarioId}/perfil/estudiante
PATCH /api/usuarios-sistema/{usuarioId}/perfil/monitor
PATCH /api/usuarios-sistema/{usuarioId}/perfil/conciliador
```

Cuando el destino seleccionado es administrativo, el componente valida nuevamente el permiso `Gestionar administradores` antes de enviar la operación.

## 9. Auditoría administrativa

`AuditLogsTable` se muestra en `/admin`, pestaña `Auditoría`.

Endpoint usado:

```text
GET /api/audit?page={page}&size={size}&username={username}&sortBy={sortBy}&sortDir={sortDir}
```

La tabla consume la paginación del backend con índice base cero. Desde la interfaz se maneja página visual base uno y se envía `page - 1` al backend. Permite búsqueda por `username`, ordenamiento por `timestamp` o `username`, y visualización de detalles en diálogo modal.

## 10. Vista de estudiantes

`EstudiantesForm` se muestra en la ruta:

```text
/estudiantes
```

El componente consulta `/api/auth/me` y permite ingresar cuando el usuario tiene simultáneamente:

```text
Acceder estudiantes
Ver estudiantes
```

Luego consulta estudiantes según el perfil autenticado:

```text
GET /api/estudiantes/activos
GET /api/estudiantes/activos/asesor/{perfilId}
```

El segundo endpoint se utiliza cuando el usuario autenticado corresponde al perfil `ASESOR` y tiene `perfilId`.

La acción visible de desactivación requiere:

```text
Cambiar estado estudiantes
```

y utiliza:

```text
PATCH /api/estudiantes/{id}/activo?activo=false
```

## 11. Vista de asesores y monitores

`AsesoresYMonitoresForm` se muestra en:

```text
/asesoresymonitores
```

El componente consulta `/api/auth/me` y permite ingresar cuando el usuario tiene simultáneamente:

```text
Acceder asesores y monitores
Ver asesores y monitores
```

Carga:

```text
GET /api/asesores/activos
GET /api/monitores/activos
```

La acción visible de desactivación requiere:

```text
Gestionar asesores y monitores
```

y utiliza el endpoint correspondiente al tipo:

```text
PATCH /api/asesores/{id}/activo?activo=false
PATCH /api/monitores/{id}/activo?activo=false
```

## 12. Relación con backend

El frontend aplica controles visuales de permisos y formularios, pero las reglas centrales se aplican en backend. Entre ellas:

- creación de usuario asociado al perfil;
- sincronización entre perfil y usuario del sistema;
- cambio de perfil mediante Strategy;
- bloqueo de desactivación de responsables con consultas operativas;
- validación de roles y permisos.

## 13. Alcance de la documentación

Este documento describe la implementación frontend actual de usuarios, roles, permisos y perfiles. La especificación backend se documenta en:

```text
doc/backend/perfiles.md
doc/api/perfiles.md
doc/api/usuarios-roles-permisos.md
doc/reglas/permisos.md
```
