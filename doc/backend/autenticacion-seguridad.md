# Backend - Autenticación, sesión y seguridad

## Propósito

El módulo de autenticación administra inicio de sesión, consulta del usuario autenticado, cierre de sesión, cambio de contraseña y recuperación de acceso. La protección HTTP se implementa mediante JWT en cookie, filtro de autenticación, configuración stateless y autorización por permisos.

## Componentes principales

| Componente | Responsabilidad implementada |
|---|---|
| `AuthController` | Expone los endpoints `/api/auth`, crea y expira la cookie `access_token`. |
| `AuthService` | Coordina login, lectura de sesión y cambio de contraseña. |
| `LoginValidator` | Normaliza el username y valida usuario, rol, perfil y contraseña para login. |
| `LoginMapper` | Construye `LoginResponseDTO` a partir del usuario y el perfil actual. |
| `JwtService` | Genera y valida JWT firmados con `app.jwt.secret`. |
| `JwtAuthenticationFilter` | Lee la cookie en cada petición y carga la autenticación con permisos activos. |
| `SecurityConfig` | Define endpoints públicos, endpoints autenticados, CORS, sesión stateless y el filtro JWT. |
| `SecurityExceptionHandler` | Produce respuestas JSON para `401` y `403`. |
| `PasswordConfig` | Expone `BCryptPasswordEncoder` como `PasswordEncoder`. |
| `CambioPasswordValidator` | Valida la operación de cambio de contraseña autenticada. |
| `PasswordResetService` | Coordina solicitud y restablecimiento de contraseña. |
| `PasswordResetTokenService` | Genera, persiste, valida e invalida tokens de recuperación. |
| `PasswordResetValidator` | Valida datos, vigencia funcional del usuario y contraseña nueva. |
| `CorsConfig` / `CorsProperties` | Configuran la política CORS aplicada a la API. |

## Endpoints del módulo

| Método | Ruta | Protección configurada | Servicio principal |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | `AuthService.login(...)` |
| `GET` | `/api/auth/me` | Autenticado | `AuthService.me(...)` |
| `POST` | `/api/auth/logout` | Público | Expiración de cookie en controller |
| `PATCH` | `/api/auth/cambiar-password` | Autenticado | `AuthService.cambiarPassword(...)` |
| `POST` | `/api/auth/solicitar-recuperacion` | Público | `PasswordResetService.solicitarRecuperacion(...)` |
| `POST` | `/api/auth/restablecer-password` | Público | `PasswordResetService.restablecerPassword(...)` |

El contrato de request y response de cada operación se encuentra en `doc/api/autenticacion.md`.

## Flujo de inicio de sesión

`AuthController.login(...)` recibe `LoginRequestDTO` y delega la autenticación en `AuthService`.

Secuencia implementada:

1. `LoginValidator.obtenerUsernameNormalizado(...)` obtiene el username normalizado.
2. `LoginValidator.validarPasswordInformada(...)` verifica la contraseña recibida.
3. `UsuarioSistemaRepository.findWithRolAndPermisosByUsernameIgnoreCase(...)` consulta el usuario con rol y permisos.
4. `LoginValidator.validarUsuarioPuedeAutenticarse(...)` exige usuario activo, rol activo y perfil operativo activo.
5. `PasswordEncoder.matches(...)` verifica la contraseña contra `password_hash`.
6. `LoginMapper` construye los datos de respuesta con perfil actual y permisos activos.
7. `JwtService.generarToken(...)` crea un JWT cuyo subject es el username.
8. `AuthController` envía el JWT en la cookie `access_token`.

### DTO de entrada

`LoginRequestDTO` contiene:

| Campo | Validación backend |
|---|---|
| `username` | `@NotBlank`; normalización mediante `normalizarEmail(...)`. |
| `password` | `@NotBlank`; verificación contra BCrypt en el servicio. |

### DTO de salida

`LoginResponseDTO` contiene:

```text
usuarioId
username
rolId
rolNombre
perfilId
tipoPerfil
permisos
```

Los permisos provienen de permisos activos del rol.

## JWT y cookie de acceso

### Token JWT

`JwtService`:

- usa `app.jwt.secret` para generar la clave de firma;
- registra `username` como subject;
- registra fecha de emisión;
- registra fecha de expiración;
- valida firma y expiración al leer el token.

La propiedad:

```text
app.jwt.expiration-ms=3600000
```

configura una vigencia de una hora.

### Cookie

`AuthController` administra la cookie:

```text
access_token
```

| Atributo | Login | Logout |
|---|---|---|
| Valor | JWT generado | Cadena vacía |
| `HttpOnly` | `true` | `true` |
| `Secure` | `app.auth.cookie.secure` | `app.auth.cookie.secure` |
| `SameSite` | `app.auth.cookie.same-site` | `app.auth.cookie.same-site` |
| `Path` | `/` | `/` |
| `Max-Age` | `3600` segundos | `0` segundos |

## Autenticación de peticiones protegidas

`JwtAuthenticationFilter` se ejecuta una vez por petición y se registra antes de `UsernamePasswordAuthenticationFilter`.

Secuencia implementada:

1. localiza `access_token` entre las cookies de la solicitud;
2. obtiene el username mediante `JwtService.obtenerUsername(...)`;
3. consulta el usuario y su rol mediante `UsuarioSistemaRepository`;
4. verifica usuario activo, rol activo y perfil real activo;
5. transforma los permisos activos del rol en `SimpleGrantedAuthority`;
6. registra la autenticación en `SecurityContextHolder`.

Cuando una petición no establece autenticación y la ruta requiere sesión, `SecurityExceptionHandler` responde `401 Unauthorized` mediante `ErrorResponseDTO`. Cuando existe autenticación sin permiso suficiente, responde `403 Forbidden` mediante el mismo DTO.

## Configuración de seguridad HTTP

`SecurityConfig` aplica:

| Elemento | Configuración implementada |
|---|---|
| Seguridad por método | `@EnableMethodSecurity`, habilitando `@PreAuthorize`. |
| CORS | Usa `CorsConfigurationSource` de `CorsConfig`. |
| CSRF | Deshabilitado. |
| Sesión HTTP de servidor | `SessionCreationPolicy.STATELESS`. |
| Form login | Deshabilitado. |
| HTTP Basic | Deshabilitado. |
| Preflight | `OPTIONS /**` permitido. |
| Swagger/OpenAPI | Rutas `GET` públicas configuradas. |
| API restante | Requiere autenticación, salvo los endpoints públicos definidos. |

### Endpoints públicos de autenticación

```text
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/solicitar-recuperacion
POST /api/auth/restablecer-password
```

### Endpoints autenticados del módulo

```text
GET   /api/auth/me
PATCH /api/auth/cambiar-password
```

## Consulta de sesión actual

`AuthService.me(...)` recibe el token desde la cookie y obtiene el usuario autenticado. El servicio:

1. valida que se reciba token;
2. obtiene el username desde el JWT;
3. consulta el usuario con rol y permisos;
4. valida que el usuario, el rol y el perfil actual se encuentren activos;
5. transforma el usuario a `UsuarioSistemaDTO`.

`UsuarioSistemaMapper` expone permisos activos del rol ordenados por nombre y resuelve el perfil operativo actual.

## Cambio de contraseña

`AuthService.cambiarPassword(...)` opera sobre el usuario autenticado:

1. obtiene el usuario a partir del JWT recibido en la cookie;
2. valida `passwordActual` y `passwordNueva`;
3. verifica la contraseña vigente mediante `PasswordEncoder.matches(...)`;
4. verifica que la nueva contraseña sea diferente;
5. guarda `passwordEncoder.encode(passwordNueva)` en `password_hash`.

`PasswordConfig` implementa el encoder con:

```java
new BCryptPasswordEncoder()
```

Por lo tanto, `password_hash` contiene la codificación BCrypt utilizada para verificación de contraseñas.

## Recuperación de contraseña

### Solicitud de recuperación

`PasswordResetService.solicitarRecuperacion(...)` recibe `SolicitarRecuperacionPasswordDTO`, cuyo campo `username` acepta además el alias JSON `email` y exige formato de correo.

Flujo implementado cuando el usuario, su rol y su perfil se encuentran activos:

1. normaliza el correo;
2. consulta el usuario para recuperación;
3. crea un token de recuperación;
4. construye el HTML mediante `EmailTemplateService`;
5. envía el correo mediante `EmailService`.

El controller responde con un mensaje genérico para cada solicitud válida procesada:

```json
{
  "mensaje": "Si el correo existe, se enviarán instrucciones para recuperar la contraseña"
}
```

### Generación y almacenamiento del token

`PasswordResetTokenService`:

- genera 32 bytes aleatorios mediante `SecureRandom`;
- codifica el token con Base64 URL-safe sin padding;
- genera un hash SHA-256 hexadecimal para persistencia;
- guarda el hash en `PasswordResetToken.tokenHash`;
- marca como usados los tokens activos anteriores del usuario;
- establece fecha de creación y fecha de expiración;
- construye el enlace con `app.frontend.reset-password-url`.

Configuración observada:

```text
app.password-reset.expiration-minutes=15
```

### Restablecimiento

`PasswordResetService.restablecerPassword(...)`:

1. valida token, contraseña nueva y confirmación;
2. busca el registro no usado mediante el hash SHA-256 del token recibido;
3. valida que el token no haya expirado;
4. valida usuario, rol y perfil activos;
5. verifica que la contraseña nueva difiera de la vigente;
6. persiste la nueva codificación BCrypt;
7. marca el token como usado y registra `fechaUso`.

## CORS

`CorsConfig` registra la configuración para `/**` a partir de `CorsProperties`.

Valores predeterminados definidos en código:

| Propiedad | Valor |
|---|---|
| `allowedOriginPatterns` | `http://localhost:3000`, `https://*.vercel.app`, `https://sistema-casos-juridicos.vercel.app` |
| `allowedMethods` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| `allowedHeaders` | `*` |
| `allowCredentials` | `true` |
| `maxAge` | `3600` |

El uso de credenciales permite que el navegador incluya `access_token` en solicitudes realizadas desde orígenes autorizados.

## Relación con otros documentos

| Documento | Contenido relacionado |
|---|---|
| `doc/api/autenticacion.md` | Contratos HTTP del módulo. |
| `doc/02-configuracion-seguridad.md` | Variables y propiedades de entorno. |
| `doc/03-autenticacion-autorizacion.md` | Visión transversal de autenticación y permisos. |
| `doc/frontend/autenticacion-sesion.md` | Pantallas y consumo frontend de sesión. |
| `doc/base-datos/entidades-principales.md` | Entidades `usuario_sistema` y `password_reset_token`. |
