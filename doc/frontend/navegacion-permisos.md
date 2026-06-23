# Navegación y permisos

La navegación del dashboard se construye a partir del usuario autenticado y de sus permisos. El frontend controla qué páginas y acciones muestra; el backend mantiene la autorización definitiva.

## Archivos principales

| Archivo | Responsabilidad |
|---|---|
| `src/components/navigation/PermissionSidebar.jsx` | Carga usuario, filtra páginas y entrega ítems al sidebar. |
| `src/components/app-sidebar.jsx` | Renderiza menú, usuario, versión visual y logout. |
| `src/lib/permission.js` | Constantes de permisos utilizadas por el frontend. |
| `src/lib/authz.js` | Helpers para comparar permisos, roles y perfiles. |

## Carga del usuario

`PermissionSidebar.jsx` llama a:

```http
GET /api/auth/me
```

Si la respuesta es `401`, redirige al login. Si la respuesta es exitosa, filtra los ítems del menú según los permisos del usuario.

## Helpers de autorización visual

`src/lib/authz.js` implementa funciones para normalizar y comparar permisos. La comparación es insensible a mayúsculas y tildes.

| Función | Uso |
|---|---|
| `normalizar(value)` | Convierte texto a mayúsculas y elimina diacríticos. |
| `nombrePermiso(permiso)` | Extrae nombre de permiso desde string u objeto. |
| `obtenerPermisos(user)` | Obtiene el array de permisos del usuario. |
| `obtenerNombresPermisos(user)` | Devuelve nombres de permisos. |
| `tienePermiso(user, permiso)` | Verifica un permiso. |
| `tieneAlgunPermiso(user, permisos)` | Verifica al menos uno. |
| `tieneTodosLosPermisos(user, permisos)` | Verifica todos. |
| `tienePerfil(user, perfil)` | Verifica tipo de perfil. |
| `tieneRol(user, rol)` | Verifica rol. |
| `esAdministrativo`, `esAsesor`, `esEstudiante`, `esMonitor`, `esConciliador` | Helpers por perfil. |

## Permisos de navegación

Los permisos del menú están definidos en `PermissionSidebar.jsx` mediante `SIDEBAR_PAGES`.

| Ítem | Ruta | Permisos requeridos | Modo |
|---|---|---|---|
| Inicio | `/inicio` | `Acceder inicio` | any |
| Recepción | `/recepcion` | `Acceder recepción` | all |
| Personas | `/personas` | `Acceder personas` | all |
| Nueva consulta | `/nuevaconsulta` | `Acceder nueva consulta` | all |
| Consultas jurídicas | `/consultasjuridicas` | `Acceder consultas jurídicas` | all |
| Tareas | `/tareas` | `Acceder tareas` | all |
| Nuevo proceso | `/nuevoproceso` | `Acceder procesos`, `Gestionar procesos` | all |
| Procesos | `/procesos` | `Acceder procesos` | all |
| Conciliaciones | `/conciliaciones` | `Acceder conciliaciones` | all |
| Estudiantes | `/estudiantes` | `Acceder estudiantes` | all |
| Asesores y monitores | `/asesoresymonitores` | `Acceder asesores y monitores` | all |
| Roles | `/roles` | `Acceder roles` | all |
| Estadísticas | `/estadisticas` | `Ver reportes` | any |
| Administración | `/admin` | `Acceder administración` | all |
| Eliminación | `/eliminacion` | `Acceder eliminación` | all |

## Modo `any` y modo `all`

El modo `any` muestra el ítem si el usuario tiene al menos uno de los permisos indicados. El modo `all` exige todos los permisos listados.

Esta lógica se implementa en `puedeVerPagina(page, user)` dentro de `PermissionSidebar.jsx`.

## Permisos centralizados

`src/lib/permission.js` define constantes agrupadas por dominio:

- navegación;
- catálogos;
- personas;
- consultas;
- seguimientos;
- usuarios del sistema;
- roles y permisos;
- estudiantes;
- asesores y monitores;
- administradores;
- perfiles auxiliares;
- conciliaciones;
- conciliadores;
- reportes y estadísticas;
- procesos.

Los valores coinciden con los nombres de permisos devueltos por el backend.

## Permisos de acción

Además del menú, los formularios validan permisos para mostrar u ocultar acciones. Ejemplos:

| Módulo | Acciones controladas |
|---|---|
| Consultas | crear, editar, cambiar estado, archivar, asignar responsables. |
| Seguimientos | crear, editar, eliminar, responder, aprobar respuestas, ver alertas. |
| Procesos | crear, editar, cambiar estado, eliminar lógicamente. |
| Conciliaciones | gestionar, asignar, programar/reprogramar, finalizar, desactivar. |
| Usuarios y roles | crear usuarios, cambiar rol, asignar permisos. |
| Catálogos | gestionar áreas, temas y tipos. |

## Relación con backend

El frontend no reemplaza el control de seguridad del backend. La interfaz filtra navegación y botones para mejorar experiencia de usuario, pero cada endpoint mantiene validaciones de permisos, alcance y reglas de negocio.

## Sidebar principal

`app-sidebar.jsx` recibe los ítems filtrados y renderiza:

- encabezado con logo y versión visual;
- menú principal;
- estado activo según `usePathname`;
- datos del usuario desde `/auth/me`;
- botón de logout.

El cierre de sesión llama a `/auth/logout` y redirige a `/`.
