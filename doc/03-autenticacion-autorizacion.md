# Autenticación y autorización

## Alcance

La autenticación se implementa con JWT almacenado en cookie HTTP-only y la autorización combina permisos de Spring Security con servicios de alcance funcional por módulo.

## Autenticación

### Componentes principales

| Componente | Función implementada |
|---|---|
| `AuthController` | Expone login, sesión, logout y operaciones de contraseña. |
| `AuthService` | Autentica, consulta el usuario actual y cambia contraseña. |
| `JwtService` | Genera y valida el token firmado. |
| `JwtAuthenticationFilter` | Construye la autenticación en cada petición protegida. |
| `SecurityConfig` | Configura rutas, filtros, sesión stateless y seguridad por método. |
| `SecurityExceptionHandler` | Genera respuestas `401` y `403` en JSON. |

### Flujo de login

Endpoint:

```text
POST /api/auth/login
```

El flujo implementado:

1. recibe `username` y `password`;
2. normaliza `username`;
3. consulta el usuario con rol y permisos;
4. valida usuario activo, rol activo y perfil actual activo;
5. valida la contraseña con `PasswordEncoder`;
6. construye la respuesta del usuario y sus permisos activos;
7. genera JWT;
8. envía el token en la cookie `access_token`.

### Cookie y JWT

| Elemento | Comportamiento |
|---|---|
| Cookie | `access_token` |
| Transporte | Cookie `HttpOnly` enviada desde frontend con `credentials: "include"`. |
| Vigencia de cookie en login | `3600` segundos. |
| Subject JWT | `username`. |
| Vigencia JWT configurada | `3600000` milisegundos. |
| Firma | Clave configurada mediante `app.jwt.secret`. |

### Sesión actual y logout

| Endpoint | Comportamiento |
|---|---|
| `GET /api/auth/me` | Retorna `UsuarioSistemaDTO` para la autenticación vigente. |
| `POST /api/auth/logout` | Envía `access_token` con valor vacío y `Max-Age=0`. |

## Filtro JWT y contexto de seguridad

`JwtAuthenticationFilter`:

1. obtiene `access_token` de las cookies;
2. extrae el username desde el JWT;
3. consulta el usuario y el rol;
4. comprueba usuario, rol y perfil real activos;
5. convierte cada permiso activo del rol en `SimpleGrantedAuthority`;
6. registra la autenticación en `SecurityContextHolder`.

`SecurityConfig` ubica este filtro antes de:

```text
UsernamePasswordAuthenticationFilter
```

## Seguridad HTTP

La cadena de seguridad aplica:

| Configuración | Valor implementado |
|---|---|
| CORS | Habilitado mediante `CorsConfig`. |
| CSRF | Deshabilitado. |
| Sesión | `SessionCreationPolicy.STATELESS`. |
| Form login | Deshabilitado. |
| HTTP Basic | Deshabilitado. |
| Métodos protegidos | Habilitados mediante `@EnableMethodSecurity`. |
| Preflight CORS | `OPTIONS /**` permitido. |

### Endpoints públicos

```text
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/solicitar-recuperacion
POST /api/auth/restablecer-password
GET  /v3/api-docs/**
GET  /swagger-ui/**
GET  /swagger-ui.html
```

### Endpoints autenticados de sesión

```text
GET   /api/auth/me
PATCH /api/auth/cambiar-password
```

Las demás rutas no declaradas como públicas requieren autenticación.

## Respuestas de seguridad

`SecurityExceptionHandler` genera `ErrorResponseDTO`:

| Estado | Condición atendida | Mensaje generado |
|---|---|---|
| `401 Unauthorized` | La petición requiere autenticación y no tiene un contexto autenticado. | `Debe iniciar sesión para acceder a este recurso`. |
| `403 Forbidden` | La autenticación no cuenta con autoridad suficiente. | `No tiene permisos para acceder a este recurso`. |

## Contraseñas

`PasswordConfig` expone un `BCryptPasswordEncoder`. Los flujos de login, cambio y restablecimiento operan con:

| Operación | Implementación |
|---|---|
| Verificación | `passwordEncoder.matches(...)` contra `password_hash`. |
| Cambio autenticado | `passwordEncoder.encode(passwordNueva)` antes de persistir. |
| Restablecimiento | `passwordEncoder.encode(passwordNueva)` antes de persistir. |

## Recuperación de contraseña

### Solicitud

```text
POST /api/auth/solicitar-recuperacion
```

El DTO recibe correo en `username` y acepta `email` como alias. Aplica `@NotBlank` y `@Email`.

El servicio procesa la recuperación cuando usuario, rol y perfil se encuentran activos y el controller retorna un mensaje genérico para cada solicitud válida.

### Token

`PasswordResetTokenService`:

- genera un token aleatorio con `SecureRandom`;
- persiste su hash SHA-256;
- marca como usados los tokens activos anteriores;
- calcula expiración con `app.password-reset.expiration-minutes`;
- construye el enlace mediante `app.frontend.reset-password-url`.

### Restablecimiento

```text
POST /api/auth/restablecer-password
```

El backend valida token no usado y vigente, coincidencia de las contraseñas, estado activo del usuario/rol/perfil y diferencia frente a la contraseña vigente. Tras completar la operación, guarda el hash BCrypt nuevo y marca el token como usado.

## Autorización por permisos

Los controllers y servicios usan `@PreAuthorize` con nombres de permisos almacenados en el modelo de acceso. Los permisos activos del rol son las authorities de la sesión y también se exponen en las respuestas de autenticación.

Ámbitos documentados en el backend:

- navegación;
- consultas;
- seguimientos y respuestas;
- procesos;
- conciliaciones y reuniones;
- perfiles, usuarios, roles y permisos;
- reportes y administración.

## Servicios de alcance funcional

Además de los permisos, los módulos implementan servicios de acceso que validan la relación del perfil autenticado con recursos concretos. Este patrón se usa en operaciones de consultas, procesos, seguimientos, conciliaciones y perfiles.

## Resolución de perfil actual

El perfil operativo activo del usuario se resuelve mediante:

```text
PerfilUsuarioActivoResolver
PerfilUsuarioActivoResolverRegistry
```

Implementaciones registradas:

```text
AdministrativoPerfilUsuarioActivoResolver
AsesorPerfilUsuarioActivoResolver
ConciliadorPerfilUsuarioActivoResolver
EstudiantePerfilUsuarioActivoResolver
MonitorPerfilUsuarioActivoResolver
```

La autenticación y recuperación de contraseña consultan esta resolución para validar el perfil real asociado al usuario.

## Frontend

El frontend consume el módulo mediante:

- `LoginForm` para login y comprobación inicial de sesión;
- `RecuperarPasswordForm` para solicitar recuperación;
- `RestablecerPasswordForm` para aplicar el token;
- `PermissionSidebar` para cargar permisos de `/auth/me` y filtrar navegación;
- `AppSidebar` para datos visibles de usuario y logout;
- `credentials: "include"` en peticiones que transportan sesión.
