# Frontend

Esta carpeta documenta el frontend del sistema de gestión de casos jurídicos.

El frontend está implementado con Next.js, React y componentes reutilizables para formularios, navegación, autenticación, consumo de API y módulos operativos del sistema.

## Estructura documentada

| Documento | Contenido |
|---|---|
| `estructura.md` | Organización del proyecto frontend, rutas públicas y rutas del dashboard. |
| `configuracion-api.md` | Variables de entorno, URL base de API y URL de archivos. |
| `autenticacion-sesion.md` | Login, recuperación, restablecimiento, sesión y cierre de sesión. |
| `navegacion-permisos.md` | Sidebar, permisos visibles y navegación protegida. |
| `servicios-api.md` | Utilidades de consumo de API, `apiClient`, helpers y formularios. |
| `formularios-validaciones.md` | Componentes reutilizables de formulario y validaciones de entrada. |
| `manejo-errores.md` | Manejo de respuestas del backend, mensajes y toasts. |

## Módulos documentados

Los módulos visibles del frontend se documentan en:

```text
frontend/modulos/
```

| Documento | Módulo |
|---|---|
| `consultas.md` | Nueva consulta, recepción y administración de consultas jurídicas. |
| `personas.md` | Registro, búsqueda y gestión de personas. |
| `procesos.md` | Creación y administración de procesos asociados a consultas. |
| `seguimientos.md` | Gestión de tareas, seguimientos y respuestas. |
| `conciliaciones.md` | Gestión de conciliaciones y documentos asociados. |
| `reuniones-conciliacion.md` | Programación y reprogramación de reuniones. |
| `estadisticas.md` | Visualización y descarga de reportes estadísticos. |
| `usuarios-roles.md` | Usuarios del sistema, roles, permisos y auditoría visual. |
| `catalogos.md` | Administración de áreas, temas y tipos. |
| `eliminacion.md` | Reactivación y gestión lógica de registros. |

## Rutas principales

El frontend usa App Router de Next.js. Entre las rutas implementadas se encuentran:

- `/` para login;
- `/recuperar-password`;
- `/restablecer-password`;
- `/inicio`;
- `/admin`;
- `/roles`;
- `/asesoresymonitores`;
- `/estudiantes`;
- `/personas`;
- `/recepcion`;
- `/nuevaconsulta`;
- `/consultasjuridicas`;
- `/nuevoproceso`;
- `/procesos`;
- `/tareas`;
- `/conciliaciones`;
- `/estadisticas`;
- `/eliminacion`.

## Integración con backend

El frontend consume el backend mediante configuración centralizada:

- `API_URL_BASE` para endpoints REST;
- `FILE_STORAGE_API_URL_BASE` para archivos;
- `credentials: "include"` para mantener sesión por cookie;
- `apiClient` y utilidades compartidas para solicitudes;
- manejo de errores con mensajes del backend cuando están disponibles.

## Navegación por permisos

El menú lateral se construye con base en permisos visibles para el usuario autenticado. La navegación por permisos mejora la experiencia del usuario, mientras que el backend conserva la validación definitiva de autorización y alcance.

## Relación con documentación backend/API

Cada módulo frontend se complementa con:

- documentos de API en `doc/api/`;
- reglas de negocio en `doc/reglas/`;
- documentación de servicios backend en `doc/backend/`.
