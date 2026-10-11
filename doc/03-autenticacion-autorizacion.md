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
| `CsrfConfig` | Configura el repositorio CSRF basado en cookie. |
| `CsrfController` | Expone el token CSRF y el nombre del header que debe usar el cliente. |
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
8. invalida el token CSRF utilizado para el login;
9. envía el JWT en la cookie `access_token`.

### Cookie y JWT

| Elemento | Comportamiento |
|---|---|
| Cookie | `access_token` |
| Transporte | Cookie `HttpOnly` enviada desde frontend con `credentials: "include"`. |
| Vigencia de cookie en login | `3600` segundos. |
| Subject JWT | `username`. |
| Vigencia JWT | Se calcula desde `app.jwt.expiration-ms`. |
| Firma | Clave configurada mediante `app.jwt.secret`. |

La vigencia de la cookie y la expiración incluida en el JWT son controles independientes. Que el navegador conserve `access_token` no hace válido un JWT cuya fecha `exp` ya venció. `JwtService` verifica firma y claims temporales al ejecutar `parseSignedClaims`; un token expirado, alterado o malformado no genera autenticación.

### Sesión actual y logout

| Endpoint | Comportamiento |
|---|---|
| `GET /api/auth/me` | Retorna `UsuarioSistemaDTO` para la autenticación vigente. |
| `POST /api/auth/logout` | Invalida el CSRF vigente y envía `access_token` con valor vacío y `Max-Age=0`. |

## Filtro JWT y contexto de seguridad

`JwtAuthenticationFilter`:

1. obtiene `access_token` de las cookies;
2. extrae el username desde el JWT;
3. consulta el usuario y el rol;
4. comprueba usuario, rol y perfil real activos;
5. convierte cada permiso activo del rol en `SimpleGrantedAuthority`;
6. registra la autenticación en `SecurityContextHolder`.

Si falta la cookie, el filtro continúa sin crear autenticación. Si el JWT no puede validarse o el usuario, rol o perfil ya no están activos, limpia el contexto y continúa la cadena como petición no autenticada. Por ello, una ruta protegida con CSRF válido responde `401` en estos casos.

`SecurityConfig` ubica este filtro antes de:

```text
UsernamePasswordAuthenticationFilter
```

## Seguridad HTTP

La cadena de seguridad aplica:

| Configuración | Valor implementado |
|---|---|
| CORS | Habilitado mediante `CorsConfig`. |
| CSRF | Habilitado mediante `CookieCsrfTokenRepository`. |
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
GET  /api/auth/csrf
GET  /actuator/health
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

### Protección CSRF

`CsrfConfig` configura `CookieCsrfTokenRepository` con estas características:

- cookie `XSRF-TOKEN` con ruta `/` y atributo `HttpOnly`;
- atributos `Secure` y `SameSite` controlados por `app.security.csrf`;
- header esperado `X-XSRF-TOKEN`;
- validación de que `SameSite=None` solo se use con una cookie `Secure`.

El flujo para una petición que modifica estado es:

1. el cliente solicita `GET /api/auth/csrf`, endpoint público;
2. Spring genera o recupera el token y envía la cookie `XSRF-TOKEN`;
3. `CsrfController` retorna en JSON `headerName` y `token`, porque la cookie es `HttpOnly`;
4. el cliente conserva la cookie mediante `credentials: "include"` y envía el token en el header indicado;
5. Spring compara el token del header con el asociado a la cookie antes de invocar el controller.

El frontend obtiene un CSRF nuevo antes de cada método no seguro. Después de login o logout exitosos limpia el token que mantenía en memoria, ya que el backend invalida el token usado en esas operaciones. La siguiente petición que modifique estado debe obtener otro mediante `GET /api/auth/csrf`.

Declarar un endpoint como `permitAll` evita exigir autenticación, pero no lo excluye de CSRF. Por ejemplo, login y logout siguen necesitando un CSRF válido. Un token CSRF ausente o incorrecto produce `403` antes de llegar al controller y puede preceder a cualquier error de autenticación.

## Respuestas de seguridad

`SecurityExceptionHandler` genera `ErrorResponseDTO`:

| Estado | Condición atendida | Mensaje generado |
|---|---|---|
| `401 Unauthorized` | La ruta requiere autenticación, pero falta un contexto autenticado; incluye JWT ausente, inválido o expirado cuando CSRF es válido. | `Debe iniciar sesión para acceder a este recurso`. |
| `403 Forbidden` | Falta autoridad o la validación CSRF rechaza la petición. | `No tiene permisos para acceder a este recurso`. |

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
