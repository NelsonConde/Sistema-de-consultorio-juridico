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
| `src/app/activar-cuenta/page.js` | Renderiza la ruta pública de activación con política `no-referrer`. |
| `src/components/auth/ActivarCuentaForm.jsx` | Lee el token, lo retira de la URL y controla el formulario y sus estados. |
| `src/components/forms/perfil/CambiarPasswordForm.jsx` | Valida confirmación y ejecuta el cambio voluntario de contraseña. |
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

## Cambio voluntario de contraseña

La pestaña Seguridad de `/mi-perfil` solicita contraseña actual, nueva
contraseña y confirmación. La confirmación se valida en el navegador y el
payload contiene exclusivamente:

```http
PATCH /api/auth/cambiar-password
```

```json
{
  "passwordActual": "contraseña-actual",
  "passwordNueva": "contraseña-nueva"
}
```

`apiClient` obtiene el CSRF antes de ejecutar el `PATCH`. Una respuesta exitosa
es `204 No Content`. El backend incrementa la versión de autenticación, renueva
la cookie `access_token` del navegador que realizó el cambio e invalida el CSRF
anterior. Los JWT emitidos antes del cambio dejan de ser válidos.

Los valores de los tres campos se limpian después del éxito, al abandonar la
pestaña Seguridad y al desmontar el formulario. No se almacenan en URL,
`localStorage` ni `sessionStorage`.

## Activación pública de cuenta

La ruta visible es:

```text
/activar-cuenta?token=<valor>
```

El componente lee el token una vez, lo conserva en memoria y reemplaza la URL
por `/activar-cuenta`. La confirmación se valida localmente y el adaptador
frontend envía solo `token` y `passwordNueva` a
`POST /api/auth/activar-cuenta` con CSRF.

La interfaz muestra estados válido, vencido, usado, revocado, inválido, error y
éxito sin presentar datos de la cuenta. En la línea base documentada, la ruta y
el formulario frontend están integrados y el backend no expone el endpoint de
activación.

El detalle del componente y sus controles se encuentra en
`doc/frontend/modulos/activacion-cuenta.md`.

## Transporte de sesión

Los componentes autenticados incluidos en este flujo envían la cookie usando:

```javascript
credentials: "include"
```

El token JWT no se lee desde los componentes frontend; la sesión se consume mediante la cookie gestionada por el backend.

El cambio voluntario exitoso reemplaza la cookie JWT del navegador actual. El
restablecimiento mediante enlace revoca credenciales anteriores, pero no crea
una sesión nueva; el usuario vuelve al login.

## Relación con autorización

La navegación lateral filtra opciones visibles según los permisos retornados por `/api/auth/me`. La autorización de cada operación protegida se procesa en backend mediante la configuración de seguridad y los permisos de los controllers.
