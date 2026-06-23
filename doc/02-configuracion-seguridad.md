# Configuración y seguridad de entorno

## Propósito

Este documento describe las propiedades y variables utilizadas por la aplicación para conexión, autenticación, correo, CORS, archivos y consumo frontend de la API.

## Backend

La configuración principal del backend se encuentra en:

```text
backend/app/src/main/resources/application.properties
```

### Variables y propiedades principales

| Variable de entorno | Propiedad de aplicación resultante | Uso implementado | Valor predeterminado |
|---|---|---|---|
| `PORT` | `server.port` | Puerto HTTP del backend. | `8080` |
| `DB_URL` | `spring.datasource.url` | URL JDBC de PostgreSQL. | Requerida por el entorno. |
| `DB_USERNAME` | `spring.datasource.username` | Usuario de conexión PostgreSQL. | Requerida por el entorno. |
| `DB_PASSWORD` | `spring.datasource.password` | Contraseña de conexión PostgreSQL. | Requerida por el entorno. |
| `DB_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` | Estrategia de esquema Hibernate. | `update` |
| `DB_SHOW_SQL` | `spring.jpa.show-sql` | Salida de SQL. | `true` |
| `JWT_SECRET` | `app.jwt.secret` | Clave usada para firmar y validar JWT. | Requerida por el entorno. |
| `AUTH_COOKIE_SECURE` | `app.auth.cookie.secure` | Atributo `Secure` de `access_token`. | `true` |
| `AUTH_COOKIE_SAME_SITE` | `app.auth.cookie.same-site` | Atributo `SameSite` de `access_token`. | `None` |
| `BREVO_API_KEY` | `brevo.api-key` | Autenticación del proveedor de correo. | Requerida por el entorno. |
| `MAIL_FROM_EMAIL` | `app.mail.from-email` | Remitente del sistema. | Requerida por el entorno. |
| `FRONTEND_URL` | `app.frontend.reset-password-url` | Base del enlace de restablecimiento. | `http://localhost:3000` |
| `UPLOAD_DIR` | `file.upload-dir` | Directorio base de archivos. | `uploads` |

Propiedades fijas observadas:

| Propiedad | Valor configurado | Uso |
|---|---|---|
| `app.jwt.expiration-ms` | `3600000` | Vigencia del JWT: una hora. |
| `app.password-reset.expiration-minutes` | `15` | Vigencia del token de recuperación. |
| `app.mail.from-name` | `Consultorio Juridico` | Nombre remitente del correo. |
| `app.mail.app-name` | `Consultorio Juridico` | Nombre de aplicación utilizado en correo. |
| `app.seguimiento.notificaciones.cron` | `0 0 8 * * *` | Ejecución diaria de recordatorios de seguimientos. |

## Base de datos

El backend configura PostgreSQL mediante Spring Data JPA con:

- driver `org.postgresql.Driver`;
- dialecto `org.hibernate.dialect.PostgreSQLDialect`;
- esquema predeterminado `DB_consultorioJuridico`;
- creación de namespaces habilitada para Hibernate.

## JWT y cookie de autenticación

### JWT

`JwtService` consume:

```text
app.jwt.secret
app.jwt.expiration-ms
```

El token conserva el `username` como subject y una expiración de una hora según la configuración actual.

### Cookie

`AuthController` entrega el JWT en:

```text
access_token
```

| Atributo | Configuración implementada |
|---|---|
| `HttpOnly` | `true` |
| `Secure` | `${AUTH_COOKIE_SECURE:true}` |
| `SameSite` | `${AUTH_COOKIE_SAME_SITE:None}` |
| `Path` | `/` |
| Duración al autenticar | `3600` segundos |
| Cierre de sesión | `Max-Age=0` |

La configuración del archivo indica que una comunicación frontend/backend cross-site usa `SameSite=None` y `Secure=true`, mientras el desarrollo HTTP local puede configurar `AUTH_COOKIE_SECURE=false` y `AUTH_COOKIE_SAME_SITE=Lax`.

## Recuperación de contraseña y correo

El enlace de recuperación se construye a partir de:

```text
app.frontend.reset-password-url=${FRONTEND_URL:http://localhost:3000}/restablecer-password
```

`PasswordResetTokenService` agrega el token como parámetro de consulta y aplica la vigencia configurada en:

```text
app.password-reset.expiration-minutes=15
```

El envío de correo utiliza:

```text
brevo.api-key=${BREVO_API_KEY}
app.mail.from-email=${MAIL_FROM_EMAIL}
app.mail.from-name=Consultorio Juridico
app.mail.app-name=Consultorio Juridico
```

Los flujos que consumen el servicio de correo incluyen recuperación de contraseña y notificaciones funcionales de otros módulos.

## CORS

`CorsConfig` registra la política de CORS para todas las rutas mediante las propiedades `app.cors.*` cargadas en `CorsProperties`.

Valores predeterminados definidos por `CorsProperties`:

| Propiedad | Valor predeterminado |
|---|---|
| `app.cors.allowed-origin-patterns` | `http://localhost:3000`, `https://*.vercel.app`, `https://sistema-casos-juridicos.vercel.app` |
| `app.cors.allowed-methods` | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| `app.cors.allowed-headers` | `*` |
| `app.cors.allow-credentials` | `true` |
| `app.cors.max-age` | `3600` |

`SecurityConfig` habilita CORS y permite solicitudes `OPTIONS /**` para preflight.

## Archivos

El almacenamiento de archivos consume:

```text
file.upload-dir=${UPLOAD_DIR:uploads}
```

El valor predeterminado ubica los archivos bajo el directorio `uploads`.

## Ejecución con Docker Compose

El archivo:

```text
docker-compose.yml
```

define los servicios:

| Servicio | Puerto expuesto | Configuración visible |
|---|---|---|
| `backend-app` | `8080:8080` | Construcción desde `./backend/app`. |
| `frontend` | `3000:3000` | `NEXT_PUBLIC_API_URL=http://localhost:8080` como argumento y variable de entorno. |

El backend resuelve sus variables requeridas desde el entorno con el que se ejecute el servicio `backend-app`.

## Frontend

La resolución de URLs se implementa en:

```text
frontend/src/lib/config.js
```

| Variable | Prioridad y uso |
|---|---|
| `NEXT_PUBLIC_API_URL_BASE` | Primera opción para la URL base del backend. |
| `NEXT_PUBLIC_API_URL` | Alternativa para la URL base del backend. |
| `NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE` | Base preferida para operaciones de archivos. |

`normalizarApiUrl(...)`:

- usa `http://localhost:8080/api` cuando no se proporciona una URL;
- agrega `https://` a un valor sin esquema;
- remueve la barra final;
- agrega `/api` cuando la ruta no lo incluye.

`src/lib/apiClient.js` envía:

```javascript
credentials: "include"
```

en sus solicitudes para transportar la cookie de sesión.

## Manejo documental de datos sensibles

La documentación identifica las variables y su función técnica. Los valores de contraseñas, llaves, tokens de acceso, tokens de recuperación y claves del proveedor de correo se suministran mediante el entorno correspondiente.
