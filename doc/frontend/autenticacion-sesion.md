# Autenticación y sesión

El frontend implementa las pantallas públicas de acceso y recuperación de contraseña, y consume la sesión autenticada en la navegación interna. La cookie de autenticación es creada por el backend y se envía en solicitudes protegidas mediante `credentials: "include"`.

## Componentes involucrados

| Archivo | Responsabilidad implementada |
|---|---|
| `src/app/page.js` | Renderiza la página pública de login. |
| `src/components/auth/LoginForm.jsx` | Verifica sesión existente y envía credenciales de acceso. |
| `src/app/recuperar-password/page.js` | Renderiza la página pública para solicitar recuperación. |
| `src/components/auth/RecuperarPasswordForm.jsx` | Envía el correo para iniciar la recuperación. |
| `src/app/restablecer-password/page.js` | Lee el token del query parameter y renderiza el formulario correspondiente. |
| `src/components/auth/RestablecerPasswordForm.jsx` | Envía token y nueva contraseña. |
| `src/components/navigation/PermissionSidebar.jsx` | Consulta la sesión y filtra las páginas visibles por permisos. |
| `src/components/app-sidebar.jsx` | Consulta datos visibles del usuario y ejecuta logout. |
| `src/lib/config.js` | Expone `API_URL_BASE` para construir rutas al backend. |
| `src/lib/api.js` | Lee cuerpos JSON, texto o vacío y obtiene mensajes de respuesta. |

## Página de login

La ruta pública:

```text
/
```

renderiza `LoginForm.jsx` y ofrece selección de tema claro, oscuro o sistema.

### Verificación inicial de sesión

Al montarse, `LoginForm` ejecuta:

```http
GET /api/auth/me
```

con:

```javascript
credentials: "include"
```

Si la respuesta es exitosa, redirige a:

```text
/inicio
```

Mientras se ejecuta esta consulta, el componente muestra el estado `Cargando...`.

### Envío de credenciales

El formulario usa `react-hook-form`, valida el correo con `requiredEmailRule()` y requiere contraseña antes de enviar:

```http
POST /api/auth/login
```

Payload:

```json
{
  "username": "usuario@dominio.com",
  "password": "contraseña"
}
```

La petición incluye:

```javascript
credentials: "include"
```

Cuando el login es exitoso, navega a `/inicio`. Cuando la respuesta no es exitosa, lee el cuerpo mediante `readResponseBody(...)` y presenta el mensaje obtenido por `getApiErrorTitle(...)` dentro del formulario.

## Sesión y navegación autenticada

### Menú filtrado por permisos

`PermissionSidebar.jsx` consulta:

```http
GET /api/auth/me
```

con cookies incluidas. Con la respuesta del usuario:

- conserva el usuario en estado local;
- filtra las páginas de `SIDEBAR_PAGES`;
- evalúa permisos requeridos mediante `tieneAlgunPermiso(...)` o `tieneTodosLosPermisos(...)`.

Si `/auth/me` responde `401`, ejecuta:

```javascript
router.replace("/")
```

### Datos visibles y cierre de sesión

`app-sidebar.jsx` consulta `/api/auth/me` para obtener `username`. El nombre mostrado se obtiene desde `user.nombre` cuando está presente o desde la parte inicial de `username`.

El botón de salida ejecuta:

```http
POST /api/auth/logout
```

con:

```javascript
credentials: "include"
```

Después de la petición, navega mediante:

```javascript
router.replace("/")
```

## Solicitud de recuperación

La ruta:

```text
/recuperar-password
```

renderiza `RecuperarPasswordForm.jsx` y ofrece selección de tema.

El formulario:

- valida el correo con `requiredEmailRule()`;
- envía el valor como `username`;
- consume el mensaje retornado por el backend;
- programa la navegación a `/` después de `2500` milisegundos cuando la respuesta es exitosa;
- muestra el mensaje de error dentro del formulario cuando la respuesta falla.

Request:

```http
POST /api/auth/solicitar-recuperacion
```

```json
{
  "username": "usuario@dominio.com"
}
```

## Restablecimiento de contraseña

La ruta:

```text
/restablecer-password?token=<valor>
```

obtiene el token mediante `useSearchParams()`.

| Caso | Comportamiento implementado |
|---|---|
| Query parameter `token` presente | Renderiza `RestablecerPasswordForm` con el token. |
| Query parameter `token` ausente | Muestra el texto `Enlace inválido o incompleto`. |

`RestablecerPasswordForm` valida en interfaz:

- contraseña nueva obligatoria;
- longitud mínima de 8 caracteres;
- confirmación obligatoria;
- coincidencia de contraseña y confirmación.

Después envía:

```http
POST /api/auth/restablecer-password
```

```json
{
  "token": "token-recibido",
  "passwordNueva": "contraseña-nueva",
  "confirmarPassword": "contraseña-nueva"
}
```

Cuando la respuesta es exitosa, muestra:

```text
La contraseña se restableció correctamente
```

y navega a `/` después de `2000` milisegundos. Cuando la respuesta falla, muestra el mensaje procesado mediante `getApiErrorTitle(...)`.

## Transporte de sesión

Los componentes autenticados incluidos en este flujo envían la cookie usando:

```javascript
credentials: "include"
```

El token JWT no se lee desde los componentes frontend; la sesión se consume mediante la cookie gestionada por el backend.

## Relación con autorización

La navegación lateral filtra opciones visibles según los permisos retornados por `/api/auth/me`. La autorización de cada operación protegida se procesa en backend mediante la configuración de seguridad y los permisos de los controllers.
