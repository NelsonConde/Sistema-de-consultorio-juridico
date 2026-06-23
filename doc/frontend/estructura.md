# Estructura del frontend

El frontend está organizado como una aplicación Next.js con App Router. Las páginas se ubican en `src/app`, los componentes reutilizables en `src/components`, los hooks en `src/hooks` y las utilidades transversales en `src/lib`.

## Árbol principal

```text
frontend/
├── package.json
├── playwright.config.js
├── public/
│   └── logo.png y recursos estáticos
└── src/
    ├── app/
    │   ├── layout.js
    │   ├── page.js
    │   ├── recuperar-password/page.js
    │   ├── restablecer-password/page.js
    │   └── (dashboard)/
    │       ├── layout.js
    │       ├── inicio/page.js
    │       ├── recepcion/page.js
    │       ├── personas/page.js
    │       ├── nuevaconsulta/page.js
    │       ├── consultasjuridicas/page.js
    │       ├── tareas/page.js
    │       ├── nuevoproceso/page.js
    │       ├── procesos/page.js
    │       ├── conciliaciones/page.js
    │       ├── estudiantes/page.js
    │       ├── asesoresymonitores/page.js
    │       ├── roles/page.js
    │       ├── admin/page.js
    │       ├── estadisticas/page.js
    │       └── eliminacion/page.js
    ├── components/
    │   ├── auth/
    │   ├── navigation/
    │   ├── forms/
    │   ├── ui/
    │   ├── Calendar.js
    │   ├── CalendarModal.jsx
    │   ├── app-sidebar.jsx
    │   └── theme-provider.jsx
    ├── hooks/
    └── lib/
```

## Layout raíz

`src/app/layout.js` define el layout global de la aplicación. En este nivel se configura:

- metadata del sistema;
- fuente global;
- `ThemeProvider`;
- estilos globales;
- estructura base de HTML.

El metadata identifica el sistema como **Consultorio Jurídico** y describe su propósito como sistema de gestión de casos jurídicos del consultorio.

## Rutas públicas

Las rutas públicas no usan el layout del dashboard. Están orientadas al acceso y recuperación de contraseña.

| Archivo | Ruta | Componente principal |
|---|---|---|
| `src/app/page.js` | `/` | `LoginForm` |
| `src/app/recuperar-password/page.js` | `/recuperar-password` | `RecuperarPasswordForm` |
| `src/app/restablecer-password/page.js` | `/restablecer-password` | `RestablecerPasswordForm` |

Estas páginas incorporan selector de tema claro/oscuro/sistema y diseño visual propio de autenticación.

## Grupo de rutas protegidas `(dashboard)`

Las páginas funcionales se agrupan bajo `src/app/(dashboard)`. Este grupo usa `src/app/(dashboard)/layout.js`, que configura:

- `SidebarProvider`;
- `PermissionSidebar`;
- encabezado persistente;
- `CalendarModal`;
- selector de tema;
- `Toaster` de Sonner;
- contenedor principal de contenido.

El layout del dashboard muestra un título y breadcrumb según la ruta. Para rutas no mapeadas usa un fallback general.

## Páginas del dashboard

| Página | Componente renderizado | Propósito |
|---|---|---|
| `/inicio` | `InicioForm` | Panel inicial con información operativa. |
| `/recepcion` | `PersonaForm` | Registro de una persona. |
| `/personas` | `PersonasForm` | Listado y gestión de personas. |
| `/nuevaconsulta` | `NuevaConsultaForm` | Registro de nueva consulta jurídica. |
| `/consultasjuridicas` | `ConsultasJuridicasForm` | Gestión de consultas existentes. |
| `/tareas` | `SeguimientosForm` | Seguimientos, respuestas y tareas. |
| `/nuevoproceso` | `NuevoProcesosForm` | Registro de proceso asociado a una consulta. |
| `/procesos` | `ProcesosForm` | Gestión de procesos. |
| `/conciliaciones` | `ConciliacionesForm` y `ReunionesConciliacionForm` | Gestión de conciliaciones y reuniones. |
| `/estudiantes` | `EstudiantesForm` / `ImportarEstudiantesForm` | Gestión e importación de estudiantes. |
| `/asesoresymonitores` | `AsesoresYMonitoresForm` / `ConciliadorForm` | Gestión de asesores, monitores y conciliadores. |
| `/roles` | `UsuarioSistemaForm` | Creación y gestión de usuarios del sistema. |
| `/admin` | Catálogos, `RolePermissionsForm`, `AuditLogsTable` | Administración de parámetros, permisos y auditoría. |
| `/estadisticas` | `EstadisticasForm` | Consulta de estadísticas y reportes. |
| `/eliminacion` | `EliminacionForm` | Registros desactivados y reactivación. |

## Componentes de autenticación

La carpeta `src/components/auth` contiene:

| Componente | Función |
|---|---|
| `LoginForm.jsx` | Envía credenciales a `/auth/login` y redirige a `/inicio` al autenticar. |
| `RecuperarPasswordForm.jsx` | Envía solicitud de recuperación a `/auth/solicitar-recuperacion`. |
| `RestablecerPasswordForm.jsx` | Envía token y nueva contraseña a `/auth/restablecer-password`. |

## Componentes de navegación

La navegación se compone de:

| Archivo | Función |
|---|---|
| `PermissionSidebar.jsx` | Consulta `/auth/me`, filtra páginas por permisos y entrega los ítems a la barra lateral. |
| `app-sidebar.jsx` | Renderiza menú lateral, estado activo, datos del usuario y cierre de sesión. |

## Componentes de formulario

La carpeta `src/components/forms` agrupa formularios por dominio:

| Carpeta | Módulos |
|---|---|
| `AdminUsuarios` | Usuarios, roles, permisos, cambio de rol y auditoría. |
| `catalogos` | Áreas, temas y tipos. |
| `conciliacion` | Conciliaciones y reuniones. |
| `consulta` | Consultas jurídicas y seguimientos. |
| `estadisticas` | Estadísticas y reportes. |
| `inicio` | Panel inicial. |
| `parts` | Componentes reutilizables de formulario. |
| `persona` | Persona individual y listado de personas. |
| `procesos` | Nuevo proceso y gestión de procesos. |
| `usuarios` | Estudiantes, asesores, monitores, conciliadores e importación. |

## Componentes reutilizables de UI

La carpeta `src/components/ui` contiene componentes de interfaz reutilizables: botones, tarjetas, diálogos, inputs, selects, tabs, tooltips, sidebar, badges, paginación y confirmación de acciones.

El componente `ConfirmActionDialog.jsx` se usa para operaciones sensibles que requieren confirmación visual del usuario antes de ejecutar la acción.

## Hooks y utilidades

| Archivo | Propósito |
|---|---|
| `src/hooks/useApiForm.js` | Hook para enviar formularios con toasts, estado de envío y manejo de 401. |
| `src/hooks/use-mobile.js` | Hook para comportamiento responsive. |
| `src/lib/config.js` | URL base de API y archivos. |
| `src/lib/api.js` | Lectura y normalización de respuestas y errores. |
| `src/lib/apiClient.js` | Cliente HTTP centralizado. |
| `src/lib/authz.js` | Helpers de permisos, roles y perfil. |
| `src/lib/permission.js` | Constantes de permisos. |
| `src/lib/form-validation.js` | Reglas reutilizables de validación. |
| `src/lib/list-utils.js` | Utilidades para listados. |
| `src/lib/utils.js` | Utilidad de clases CSS `cn`. |

## Convenciones de organización

- Las páginas de `src/app` deben ser delgadas y delegar la lógica al componente de formulario correspondiente.
- Los componentes de formulario encapsulan carga de datos, validaciones y operaciones de API.
- Las constantes de permisos se centralizan en `src/lib/permission.js`.
- Las reglas de autorización visual se centralizan en `src/lib/authz.js`.
- Las URLs del backend se centralizan en `src/lib/config.js`.
- Los formularios deben reutilizar componentes de `forms/parts` cuando el patrón sea común.
