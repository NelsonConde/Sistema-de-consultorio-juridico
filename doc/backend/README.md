# Backend

El backend principal del sistema se encuentra en:

```text
backend/app
```

Este backend expone la API REST del sistema de gestión de casos jurídicos y concentra reglas de negocio, seguridad, persistencia, auditoría, estadísticas, notificaciones y manejo de archivos.

## Tecnologías principales

- Java 21.
- Spring Boot.
- Spring MVC.
- Spring Security.
- Spring Data JPA.
- PostgreSQL.
- Jakarta Validation.
- Maven.
- Lombok.
- JWT.
- Cookies de sesión.
- AOP para auditoría.
- Servicio de correo/notificaciones.
- Servicio de almacenamiento de archivos.

## Estructura principal

```text
src/main/java/co/edu/ufps/legal_cases/
  App.java
  audit/
  business/
  common/
  config/
  file_storage/
  security/
```

## Documentos backend por módulo

| Documento | Contenido |
|---|---|
| `autenticacion-seguridad.md` | Seguridad, sesión, usuario actual, autenticación y protección de endpoints. |
| `catalogos.md` | Catálogos generales y jurídicos usados por perfiles, consultas y procesos. |
| `personas.md` | Personas naturales, empresas, activación lógica y relación con consultas. |
| `perfiles.md` | Perfiles internos, usuarios del sistema, Strategy de perfiles y permisos asociados. |
| `consultas.md` | Consultas jurídicas, estados, responsables, archivo, cierre y trazabilidad. |
| `procesos.md` | Procesos asociados, radicado, órganos de control, especialidades y estados. |
| `seguimientos.md` | Seguimientos, respuestas, revisión, notificaciones y scheduler. |
| `conciliaciones.md` | Conciliaciones, responsables, reuniones, documentos, estados y notificaciones. |
| `estadisticas.md` | Estadísticas por semestre, rango, perfil y generación de reportes PDF. |
| `archivos.md` | Carga, descarga, listado y seguridad de rutas de archivos. |
| `auditoria.md` | Auditoría mediante `@Auditable`, `AuditAspect` y consulta de logs. |

## Organización por capas

El backend usa una separación por responsabilidades:

| Capa | Responsabilidad |
|---|---|
| Controller | Exponer endpoints REST y recibir solicitudes HTTP. |
| DTO | Transportar datos entre API y frontend. |
| Service | Coordinar casos de uso y reglas del módulo. |
| CommandService | Operaciones de escritura y cambios de estado. |
| QueryService | Consultas, listados y búsquedas. |
| Validator | Reglas de negocio y consistencia de datos. |
| Mapper | Conversión entre entidades y DTOs. |
| Repository | Persistencia mediante Spring Data JPA. |
| AccessService | Permisos, alcance y reglas de acceso por perfil. |
| Scheduler | Procesamiento periódico de notificaciones cuando aplica. |

## Paquetes transversales

### `audit`

Incluye auditoría de acciones relevantes:

- `Auditable`;
- `AuditAspect`;
- `AuditLog`;
- `AuditLogController`;
- `AuditLogService`;
- `AuditLogRepository`.

### `common`

Incluye utilidades y manejo centralizado de errores:

- `BusinessException`;
- `ErrorResponseDTO`;
- `GlobalExceptionHandler`;
- utilidades de normalización;
- componentes comunes reutilizables.

### `config`

Incluye configuración general:

- seguridad;
- CORS;
- inicialización de datos;
- propiedades de aplicación.

### `file_storage`

Incluye servicios y controladores de almacenamiento de archivos, con validaciones de seguridad para rutas y operaciones de carga/descarga.

### `security`

Incluye autenticación, autorización, usuarios del sistema, roles, permisos, usuario actual, cambio de perfil y resolución del perfil activo.

## Reglas funcionales destacadas

El backend implementa reglas críticas como:

- consultas nuevas inician en `PENDIENTE`;
- el estado de consulta se cambia por endpoint específico;
- una consulta requiere resultado para cerrarse;
- una consulta no se cierra con procesos, seguimientos, respuestas, notificaciones o conciliaciones operativas relacionadas;
- una consulta con actividad asociada conserva sus datos estructurales para trazabilidad;
- un proceso puede estar pendiente sin radicado y requiere radicado para estados finales;
- una respuesta de seguimiento rechazada requiere observación de revisión;
- un seguimiento notificable al estudiante requiere estudiante activo asignado;
- asesor, estudiante y monitor conservan integridad operativa frente a consultas vivas;
- el estado del perfil y el usuario del sistema se sincronizan;
- el cambio y resolución de perfil se implementan mediante Strategy;
- la conciliación finalizada o desactivada cancela notificaciones de reunión programadas.

## Pruebas unitarias

El backend incluye pruebas unitarias orientadas a reglas críticas de negocio, validadores y estrategias de seguridad. La documentación de pruebas se encuentra en:

```text
../06-pruebas.md
```

## Relación con API y reglas

Cada módulo backend se complementa con:

- contrato HTTP en `doc/api/`;
- reglas funcionales en `doc/reglas/`;
- entidades y relaciones en `doc/base-datos/`;
- decisiones técnicas cuando aplica en `doc/decisiones/`.
