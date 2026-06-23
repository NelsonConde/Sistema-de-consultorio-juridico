# API - Autenticación

Este documento describe el contrato HTTP implementado para autenticación, sesión actual, cierre de sesión y gestión de contraseña.

Base path:

```text
/api/auth
```

## Resumen de endpoints

| Método | Ruta | Acceso configurado | Uso |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Autentica credenciales y crea la cookie de acceso. |
| `GET` | `/api/auth/me` | Autenticado | Retorna el usuario asociado a la sesión actual. |
| `POST` | `/api/auth/logout` | Público | Expira la cookie de acceso. |
| `PATCH` | `/api/auth/cambiar-password` | Autenticado | Actualiza la contraseña del usuario autenticado. |
| `POST` | `/api/auth/solicitar-recuperacion` | Público | Procesa una solicitud de recuperación. |
| `POST` | `/api/auth/restablecer-password` | Público | Actualiza la contraseña mediante un token de recuperación vigente. |

## Cookie de autenticación

Al completar el login, el backend entrega el JWT en una cookie HTTP denominada:

```text
access_token
```

| Atributo | Valor o fuente de configuración |
|---|---|
| `HttpOnly` | `true` |
| `Secure` | Propiedad `app.auth.cookie.secure`, configurada desde `AUTH_COOKIE_SECURE`; valor predeterminado `true`. |
| `SameSite` | Propiedad `app.auth.cookie.same-site`, configurada desde `AUTH_COOKIE_SAME_SITE`; valor predeterminado `None`. |
| `Path` | `/` |
| `Max-Age` en login | `3600` segundos. |
| `Max-Age` en logout | `0` segundos. |

Las solicitudes frontend que requieren sesión incluyen la cookie mediante:

```javascript
credentials: "include"
```

## `POST /api/auth/login`

Autentica al usuario y establece la cookie `access_token`.

### Request

Content-Type:

```text
application/json
```

```json
{
  "username": "usuario@dominio.com",
  "password": "contraseña"
}
```

### Validación y autenticación

| Dato o condición | Comportamiento implementado |
|---|---|
| `username` | Obligatorio mediante `@NotBlank`; `LoginValidator` lo normaliza como correo antes de buscar el usuario. |
| `password` | Obligatorio mediante `@NotBlank` y validación del servicio. |
| Usuario | Debe existir y estar activo. |
| Rol | Debe estar asociado y activo. |
| Perfil actual | Debe resolverse como perfil activo. |
| Contraseña | Se verifica mediante `PasswordEncoder.matches(...)`. |

El formulario web de login valida adicionalmente el formato de correo antes de ejecutar la petición.

### Response `200 OK`

Header de sesión:

```text
Set-Cookie: access_token=<jwt>; HttpOnly; Secure=<configurable>; SameSite=<configurable>; Path=/; Max-Age=3600
```

Body (`LoginResponseDTO`):

```json
{
  "usuarioId": 1,
  "username": "usuario@dominio.com",
  "rolId": 1,
  "rolNombre": "Nombre del rol",
  "perfilId": 1,
  "tipoPerfil": "TIPO_PERFIL",
  "permisos": [
    "Permiso ejemplo"
  ]
}
```

Los permisos de la respuesta corresponden a permisos activos del rol.

### Respuestas de validación o negocio

| Estado | Situación procesada |
|---|---|
| `400 Bad Request` | Datos obligatorios inválidos, credenciales no válidas o estado de usuario, rol o perfil que no permite autenticarse. |

## `GET /api/auth/me`

Retorna la información del usuario autenticado. Spring Security exige una autenticación establecida por el filtro JWT antes de acceder al controller.

### Request

No requiere body. La cookie se envía con:

```javascript
credentials: "include"
```

### Response `200 OK`

Body (`UsuarioSistemaDTO`):

```json
{
  "id": 1,
  "username": "usuario@dominio.com",
  "activo": true,
  "rolId": 1,
  "rolNombre": "Nombre del rol",
  "perfilId": 1,
  "tipoPerfil": "TIPO_PERFIL",
  "permisos": [
    "Permiso ejemplo"
  ]
}
```

Los permisos expuestos son permisos activos del rol y se ordenan por nombre en el mapper de usuario.

### Respuesta de seguridad

| Estado | Situación procesada |
|---|---|
| `401 Unauthorized` | La petición no establece autenticación válida mediante la cookie JWT. |

## `POST /api/auth/logout`

Expira la cookie de autenticación en el navegador.

### Response `200 OK`

El endpoint retorna respuesta sin body y envía:

```text
Set-Cookie: access_token=; HttpOnly; Secure=<configurable>; SameSite=<configurable>; Path=/; Max-Age=0
```

## `PATCH /api/auth/cambiar-password`

Actualiza la contraseña del usuario autenticado.

### Request

```json
{
  "passwordActual": "contraseña-actual",
  "passwordNueva": "contraseña-nueva"
}
```

### Validaciones y reglas

| Campo o regla | Comportamiento implementado |
|---|---|
| `passwordActual` | Obligatoria y debe coincidir con el hash almacenado. |
| `passwordNueva` | Obligatoria; longitud entre 8 y 100 caracteres. |
| Diferencia de contraseña | La nueva contraseña no puede coincidir con la actual. |
| Persistencia | La nueva contraseña se codifica mediante BCrypt antes de guardarse en `password_hash`. |

### Response `204 No Content`

La respuesta se entrega sin cuerpo.

### Respuestas

| Estado | Situación procesada |
|---|---|
| `401 Unauthorized` | No existe autenticación válida para ejecutar la operación. |
| `400 Bad Request` | Datos inválidos, contraseña actual incorrecta o contraseña nueva igual a la actual. |

## `POST /api/auth/solicitar-recuperacion`

Inicia el flujo de recuperación asociado a un correo.

### Request

El DTO recibe `username` y acepta `email` como alias JSON:

```json
{
  "username": "usuario@dominio.com"
}
```

o:

```json
{
  "email": "usuario@dominio.com"
}
```

### Validación y procesamiento

| Dato o condición | Comportamiento implementado |
|---|---|
| `username` / `email` | Obligatorio y con formato de correo mediante `@Email`. |
| Correo recibido | Se normaliza antes de consultar el usuario. |
| Usuario, rol y perfil | Cuando los tres se encuentran activos, se genera el token y se envía el correo. |
| Respuesta HTTP | Conserva el mismo mensaje de confirmación para toda solicitud válida procesada. |

Cuando corresponde enviar la recuperación:

1. se marcan como usados los tokens activos anteriores del usuario;
2. se genera un token aleatorio seguro;
3. se persiste únicamente su hash SHA-256;
4. se construye el enlace con `app.frontend.reset-password-url`;
5. se envía el correo HTML de recuperación.

### Response `200 OK`

```json
{
  "mensaje": "Si el correo existe, se enviarán instrucciones para recuperar la contraseña"
}
```

### Respuesta de validación

| Estado | Situación procesada |
|---|---|
| `400 Bad Request` | Correo ausente o con formato inválido. |

## `POST /api/auth/restablecer-password`

Actualiza la contraseña utilizando el token recibido en el enlace de recuperación.

### Request

```json
{
  "token": "token-recibido",
  "passwordNueva": "contraseña-nueva",
  "confirmarPassword": "contraseña-nueva"
}
```

### Validaciones y reglas

| Campo o regla | Comportamiento implementado |
|---|---|
| `token` | Obligatorio; se transforma a hash SHA-256 para localizar un token no usado. |
| `passwordNueva` | Obligatoria; longitud entre 8 y 100 caracteres. |
| `confirmarPassword` | Obligatoria y debe coincidir con la contraseña nueva. |
| Vigencia del token | Se valida su fecha de expiración; la configuración actual define `15` minutos. |
| Estado del usuario | Usuario, rol y perfil asociado deben encontrarse activos. |
| Contraseña nueva | Debe diferir de la contraseña vigente y se codifica mediante BCrypt. |
| Consumo del token | Al restablecer exitosamente, el token se marca como usado y registra `fechaUso`. |

### Response `200 OK`

```json
{
  "mensaje": "La contraseña se restableció correctamente"
}
```

### Respuestas de negocio o validación

| Estado | Situación procesada |
|---|---|
| `400 Bad Request` | Token inválido, usado o expirado; datos de contraseña inválidos; confirmación diferente; contraseña nueva igual a la vigente. |

## Relación con seguridad y frontend

- `SecurityConfig` declara los cuatro endpoints `POST` públicos y protege `/me` y `/cambiar-password`.
- `JwtAuthenticationFilter` lee la cookie y establece la autenticación con permisos activos del rol.
- Los contratos de error JSON generados por seguridad se detallan en `doc/05-estandar-api-errores.md`.
- El flujo visible de pantallas se documenta en `doc/frontend/autenticacion-sesion.md`.
