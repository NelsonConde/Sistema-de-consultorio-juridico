# Arquitectura del sistema

## Vista general

El proyecto se divide en dos aplicaciones principales:

```text
backend/app   API REST, reglas de negocio, seguridad, persistencia y reportes.
frontend      Aplicación web Next.js consumidora de la API.
```

La arquitectura favorece separación de responsabilidades. El backend contiene controllers, DTOs, models, repositories, services, validators, mappers, access services, configuración de seguridad, auditoría, almacenamiento de archivos y schedulers. El frontend contiene rutas, componentes, formularios, navegación, utilidades de API y componentes visuales.

## Arquitectura backend

La estructura principal del backend se organiza así:

```text
co.edu.ufps.legal_cases
  audit
  business
  common
  config
  file_storage
  security
```

### Paquete `business`

Contiene la lógica principal del consultorio jurídico:

```text
business/controller
business/dto
business/model
business/repository
business/scheduler
business/service
```

Los módulos de negocio identificados en código son:

- catálogos;
- personas;
- perfiles;
- consultas;
- procesos;
- seguimientos;
- respuestas de seguimiento;
- conciliaciones;
- reuniones de conciliación;
- estadísticas.

### Paquete `security`

Contiene autenticación, autorización, JWT, roles, permisos, usuarios del sistema, cambio de perfil y resolución del perfil activo.

Elementos relevantes:

- `AuthController`;
- `UsuarioSistemaController`;
- `RolController`;
- `PermisoController`;
- `JwtAuthenticationFilter`;
- `JwtService`;
- `AuthService`;
- `UsuarioCambioPerfilService`;
- estrategias de cambio de perfil;
- estrategias de estado de perfil;
- estrategias de resolución de perfil activo.

### Paquete `audit`

Implementa auditoría probatoria estructurada mediante AOP:

- `Auditable` marca métodos de servicio auditables;
- `AuditAspect` intercepta éxitos, fallos y denegaciones;
- `AuditLogService` coordina persistencia transaccional y eventos que sobreviven al rollback;
- `AuditLogController` expone consulta paginada con permiso dedicado y tamaño limitado.

### Paquete `file_storage`

Implementa carga, descarga, listado y administración de archivos.

### Paquete `common`

Contiene utilidades compartidas, excepciones y el manejador global de errores.

## Patrón de capas del backend

La capa típica por módulo sigue este flujo:

```text
Controller -> Service/Fachada -> CommandService/QueryService -> Validator/Mapper -> Repository -> Entity
```

No todos los módulos tienen exactamente los mismos nombres, pero el patrón general está presente.

### Controllers

Exponen endpoints REST, reciben DTOs, parámetros y archivos, y delegan en servicios. También aplican `@PreAuthorize` para autorización por permisos.

### Services

Agrupan lógica de negocio. En módulos complejos se separan responsabilidades:

- CommandService para operaciones de escritura;
- QueryService para consultas;
- Validator para reglas de negocio;
- Mapper para conversión entidad/DTO;
- AccessService para permisos y alcance.

### Repositories

Usan Spring Data JPA para acceso a base de datos. Algunos repositories incluyen consultas derivadas y consultas orientadas a estadísticas.

### DTOs

Definen contratos de entrada y salida. Los DTOs se validan con Jakarta Validation y con validadores de negocio cuando la regla requiere contexto.

## Estrategias de perfil

El código fuente implementa Strategy en tres zonas de seguridad/perfil:

### Cambio hacia nuevo perfil

Se usan handlers de cambio de perfil:

```text
PerfilCambioHandler
PerfilCambioHandlerRegistry
CambiarAAdministrativoHandler
CambiarAAsesorHandler
CambiarAConciliadorHandler
CambiarAEstudianteHandler
CambiarAMonitorHandler
```

Cada handler conoce cómo construir o actualizar el perfil destino.

### Desactivación del perfil anterior

Se usan handlers de estado:

```text
PerfilEstadoHandler
PerfilEstadoHandlerRegistry
AdministrativoPerfilEstadoHandler
AsesorPerfilEstadoHandler
ConciliadorPerfilEstadoHandler
EstudiantePerfilEstadoHandler
MonitorPerfilEstadoHandler
```

Esto evita que el servicio orquestador tenga lógica condicional por tipo de perfil y permite aplicar reglas específicas, como validar consultas operativas antes de desactivar asesores, estudiantes o monitores.

### Resolución del perfil activo

Se usan resolvers de perfil activo:

```text
PerfilUsuarioActivoResolver
PerfilUsuarioActivoResolverRegistry
AdministrativoPerfilUsuarioActivoResolver
AsesorPerfilUsuarioActivoResolver
ConciliadorPerfilUsuarioActivoResolver
EstudiantePerfilUsuarioActivoResolver
MonitorPerfilUsuarioActivoResolver
```

Cada resolver busca el perfil activo del usuario en su repositorio correspondiente.

## Exclusividad y concurrencia de perfiles

La exclusividad de un perfil activo por cuenta es una regla de aplicación. En operaciones directas, los command services bloquean `UsuarioSistema` con `PESSIMISTIC_WRITE`, vuelven a cargar el perfil, validan la política central y persisten dentro de la transacción. El cambio explícito y la suspensión o habilitación de la cuenta también usan ese bloqueo para serializar las escrituras sobre la misma fila de `UsuarioSistema`.

Cuando una operación puede afectar la continuidad administrativa, el orden es: validación y bloqueo de continuidad administrativa, bloqueo pesimista de la cuenta, lectura y validación del estado, modificación y persistencia. Los cambios explícitos hacia `ADMINISTRATIVO` participan igualmente del primer bloqueo para evitar invertir el orden frente a una suspensión concurrente.

Los cambios de contraseña autenticados y los restablecimientos también obtienen la entidad que modifican mediante el bloqueo pesimista de `UsuarioSistema`, dentro de su transacción. La identidad se resuelve primero sin conservar una entidad editable; después del bloqueo se revalidan la cuenta y las condiciones de seguridad. En el restablecimiento, además, el token activo se vuelve a consultar tras adquirir el bloqueo para conservar su uso único frente a solicitudes concurrentes.

Esta garantía cubre los flujos de estado de cuenta, cambio de perfil y credenciales descritos aquí, pero no acredita que todos los escritores presentes o futuros de `UsuarioSistema` usen la misma serialización. Cada nuevo flujo que modifique la entidad debe revisar explícitamente su estrategia de concurrencia.

Las cinco entidades de perfil tienen una relación uno a uno y una columna `usuario_sistema_id` única en su tabla respectiva. Esa unicidad local no constituye una restricción única entre las cinco tablas ni sustituye la validación transaccional de la aplicación.

## Arquitectura frontend

El frontend usa Next.js con App Router.

Estructura principal observada:

```text
frontend/src/app
frontend/src/components
frontend/src/hooks
frontend/src/lib
```

### Rutas

Las páginas se ubican en `src/app`. Las rutas del dashboard se agrupan bajo `src/app/(dashboard)`.

### Componentes

Los componentes se organizan en:

- `components/auth` para autenticación;
- `components/navigation` para navegación por permisos;
- `components/forms` para formularios de módulos;
- `components/forms/parts` para controles reutilizables;
- `components/ui` para componentes visuales base.

### Configuración de API

La configuración de URLs está centralizada en `src/lib/config.js`. El cliente HTTP de apoyo está en `src/lib/apiClient.js`, y el manejo de errores HTTP se apoya en `src/lib/api.js`.

## Arquitectura de seguridad

La seguridad del backend usa:

- Spring Security;
- JWT;
- cookie HTTP-only;
- filtro `JwtAuthenticationFilter`;
- autorización por `@PreAuthorize`;
- validadores de permisos y alcance por servicio;
- CORS configurable por propiedades.

Los endpoints públicos son los de login, logout, solicitud/restablecimiento de contraseña y documentación OpenAPI. El resto requiere autenticación, salvo excepciones declaradas en configuración.

## Arquitectura de estadísticas

El módulo de estadísticas se organiza con fachada y servicios especializados:

```text
EstadisticasController
EstadisticasService
EstadisticasQueryService
EstadisticasRangoQueryService
EstadisticasPerfilQueryService
EstadisticasMapperService
EstadisticasPdfService
```

El controller expone estadísticas por semestre, rango y perfil. El servicio PDF genera reportes descargables.

## Schedulers

El código contiene schedulers para procesar notificaciones pendientes de seguimiento y conciliación. La configuración de recordatorios de seguimiento usa la propiedad `app.seguimiento.notificaciones.cron`.
