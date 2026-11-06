# SCRUM-297 — Matriz historia, PR, prueba y evidencia

Fecha: 2026-11-02, America/Bogota.

Línea base: `develop`
`23009d05ca2f37e7195bcc64cb54d546645ec27f`.

## Matriz de trazabilidad

| Historia / requisito | Capacidad versionada | PR o línea base | Prueba versionada | Evidencia |
|---|---|---|---|---|
| RF37 / CU-01 | Login, JWT en cookie y permisos efectivos. | `develop` | Pruebas de autenticación y seguridad del backend. | `doc/api/autenticacion.md` y `doc/backend/autenticacion-seguridad.md`. |
| RF38 / CU-01 | Consulta de sesión y validación de `authVersion`. | PR #77 | `PasswordSessionPolicyPostgreSqlTest` y pruebas del filtro JWT. | PR #77: 15 pruebas focalizadas y 31 regresiones ejecutables aprobadas. |
| RF39 / CU-01 | Logout elimina JWT e invalida CSRF. | `develop` | `AuthControllerCsrfTest`. | Contrato `POST /api/auth/logout`. |
| RF40 / CU-01 | Recuperación no enumerativa, token temporal y restablecimiento con revocación de sesiones. | PR #77 para `authVersion`; flujo base en `develop`. | Pruebas de `PasswordResetService` y política de sesión. | `doc/api/autenticacion.md`. |
| RF44 | Consulta administrativa, cambio de estado y cambio explícito de perfil. | `develop` | Pruebas de invariantes y operaciones de perfil. | `UsuarioSistemaController` y `doc/api/usuarios-roles-permisos.md`. |
| PB-30 / RF60-RF61 / CU-11 | Resumen de identidad y edición exclusiva de email y teléfono. | PR #59, integrado. | `frontend/e2e/mi-perfil.spec.js`, 13 escenarios descubiertos en la validación del PR. | Build y lint registrados en PR #59; `MiPerfilControllerTest` y `MiPerfilServiceTest`. |
| PB-31 / RF62 / CU-11 | Cambio voluntario, payload mínimo, BCrypt y rotación de sesión. | PR #59 y PR #77, integrados; PR #79 abierto para QA frontend focalizada. | `PasswordSessionPolicyPostgreSqlTest`, `frontend/e2e/mi-perfil.spec.js` y `frontend/e2e/password-change-qa.spec.js`. | PR #77 registra 15 pruebas focalizadas aprobadas; PR #79 contiene seis casos frontend. |
| PB-32 / RF63 / CU-01 | Ruta pública, token solo en memoria, estados visuales y doble envío bloqueado. | PR #69, integrado en frontend. | `frontend/e2e/account-activation.spec.js`, nueve casos descubiertos. | Build, lint y prerender de `/activar-cuenta` registrados en PR #69. |
| RF63 backend | Activación, generación, reenvío y revocación de invitaciones. | No existe endpoint ni entidad de invitación en la línea base. | No existe prueba backend de activación en la línea base. | `AuthController` no expone `/api/auth/activar-cuenta`. |

## Correspondencia de datos y secretos

| Dato | Entrada | Salida o persistencia documentada |
|---|---|---|
| Documento | Identidad institucional | Se expone enmascarado en Mi Perfil; no se documenta como contraseña. |
| Contraseña actual | Cambio voluntario | Se recibe para verificación y no se retorna. |
| Contraseña nueva | Cambio o restablecimiento | Se persiste únicamente como hash BCrypt. |
| Confirmación | Frontend | Se valida localmente en cambio voluntario; no se envía en ese endpoint. |
| JWT | Cookie de sesión | Cookie `HttpOnly`; no se presenta en cuerpos JSON ni evidencias. |
| Token de recuperación | Enlace temporal | Se persiste como hash SHA-256 y se consume una sola vez. |
| Token de activación | URL pública frontend | Se conserva en memoria y se retira de la URL; el backend de activación no está presente. |
| CSRF | Cookie y header | Se obtiene antes de operaciones no seguras y se invalida tras login, logout o cambio de contraseña exitoso. |

## Guion de demostración

### 1. Login y sesión

1. Abrir `/` e iniciar sesión con una cuenta sintética activa.
2. Confirmar la navegación a `/inicio`.
3. Consultar `GET /api/auth/me` en Network.
4. Verificar que la respuesta contiene identidad, rol, perfil y permisos, sin
   contraseña, hash ni JWT.
5. Ejecutar logout y observar `POST /api/auth/logout`.

### 2. Perfil propio y contacto

1. Abrir `/mi-perfil`.
2. Mostrar nombre, username, rol, perfil, documento enmascarado, sede y código
   como datos de solo lectura.
3. Modificar email y teléfono desde Contacto.
4. Verificar que `PATCH /api/mi-perfil/contacto` contiene únicamente `email` y
   `telefono`.
5. Confirmar que los atributos protegidos conservan sus valores.

### 3. Cambio voluntario de contraseña

1. Abrir la pestaña Seguridad.
2. Ingresar contraseña actual, nueva contraseña y confirmación sintéticas.
3. Verificar que `PATCH /api/auth/cambiar-password` contiene únicamente
   `passwordActual` y `passwordNueva`.
4. Confirmar la respuesta `204 No Content`, la renovación de `access_token` y
   la eliminación del CSRF previo.
5. Consultar nuevamente `/api/auth/me` desde la sesión renovada.

### 4. Recuperación y restablecimiento

1. Abrir `/recuperar-password` y enviar un correo sintético.
2. Verificar el mensaje genérico de la respuesta.
3. Abrir un enlace temporal de prueba en `/restablecer-password`.
4. Definir una contraseña nueva y confirmar el restablecimiento.
5. Iniciar sesión de forma explícita con la nueva contraseña.

### 5. Activación pública disponible en frontend

1. Abrir `/activar-cuenta?token=token-sintetico`.
2. Verificar que el query parameter se elimina del historial visible.
3. Comprobar los campos de nueva contraseña y confirmación.
4. Mostrar con respuestas sintéticas los estados vencido, usado, revocado,
   inválido y éxito.
5. Verificar que el token no aparece en almacenamiento, logs ni redirecciones.

## Archivos documentales relacionados

- `doc/especificaciones/SCRUM-297-actualizacion-requisitos-casos-uso.md`;
- `doc/api/autenticacion.md`;
- `doc/frontend/autenticacion-sesion.md`;
- `doc/frontend/modulos/activacion-cuenta.md`;
- `doc/mantenimiento/manual-despliegue-recuperacion.md`.
