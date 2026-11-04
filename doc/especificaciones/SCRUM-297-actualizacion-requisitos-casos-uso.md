# SCRUM-297 — Actualización de requisitos y casos de uso de cuenta

Fecha de contraste: 2026-11-02, America/Bogota.

Línea base: `develop` en
`23009d05ca2f37e7195bcc64cb54d546645ec27f`.

Fuentes documentales contrastadas:

- `EP5 Backlog de Producto y las epicas e historias.docx`, para RF37-RF40,
  RF44, RF60-RF63 y PB-30-PB-32;
- `EP3 Casos de Uso.docx`, versión 1.1, para CU-01 y CU-11;
- contratos y componentes versionados en `develop`.

Este documento contiene el texto controlado que corresponde incorporar en los
documentos oficiales. No contiene contraseñas, tokens, cookies, llaves ni datos
de cuentas reales.

## 1. Estado de las capacidades

| Elemento | Capacidad presente en `develop` |
|---|---|
| RF37 | Login con usuario y contraseña, JWT en cookie `HttpOnly` y permisos efectivos. |
| RF38 | Consulta de sesión mediante `GET /api/auth/me`, con validación de cuenta, rol, perfil y `authVersion`. |
| RF39 | Cierre mediante `POST /api/auth/logout`, eliminación de `access_token` e invalidación del CSRF vigente. |
| RF40 | Solicitud y restablecimiento de contraseña con respuesta no enumerativa, token temporal almacenado como hash, consumo único y revocación de JWT anteriores. |
| RF44 | Consulta administrativa, cambio de estado y cambio explícito de perfil mediante operaciones separadas. No existe un endpoint genérico para editar username, contraseña, contacto o identidad de otra cuenta. |
| RF60 | Consulta del perfil propio con identidad, datos institucionales y contacto; el documento se entrega enmascarado. |
| RF61 | Actualización propia limitada a `email` y `telefono`. |
| RF62 | Cambio voluntario de contraseña con contraseña actual, nueva contraseña, BCrypt, rotación de `authVersion`, renovación de la sesión actual e invalidación del CSRF anterior. |
| RF63 | Ruta y formulario público `/activar-cuenta` presentes en frontend. El endpoint backend de activación y la gestión de invitaciones no aparecen en esta línea base. |

## 2. Texto actualizado de requisitos funcionales

### RF37 — Iniciar sesión

El sistema permite autenticar usuarios activos mediante username y contraseña.
El backend valida la cuenta, el rol y el perfil actual, y emite un JWT asociado
a la versión vigente de autenticación. La credencial se transporta en la cookie
`access_token`, configurada como `HttpOnly`, y los permisos efectivos se
obtienen desde el backend.

### RF38 — Consultar la sesión actual

El sistema permite al usuario autenticado consultar su identidad de cuenta,
estado, rol, perfil actual y permisos efectivos mediante `GET /api/auth/me`.
La operación rechaza credenciales ausentes, vencidas o emitidas con una versión
de autenticación revocada.

### RF39 — Cerrar sesión

El sistema permite cerrar la sesión mediante `POST /api/auth/logout`. La
respuesta elimina la cookie `access_token`, establece `Max-Age=0` e invalida el
estado CSRF asociado a la sesión del navegador.

### RF40 — Recuperar y restablecer contraseña

El sistema permite solicitar recuperación mediante correo y responde con el
mismo mensaje tanto si la cuenta existe como si no. Cuando corresponde, genera
un token aleatorio, persiste únicamente su hash SHA-256, invalida tokens activos
anteriores y envía un enlace temporal. El restablecimiento exige token vigente
y no utilizado, contraseña nueva y confirmación. Tras el éxito, el token queda
usado, la contraseña se almacena mediante BCrypt y los JWT emitidos previamente
quedan revocados. El restablecimiento no inicia una sesión automáticamente.

### RF44 — Administrar datos habilitados de una cuenta

El sistema permite a usuarios con permisos administrativos consultar cuentas,
cambiar su estado activo y ejecutar un cambio explícito de perfil institucional
mediante endpoints separados. Estas operaciones no permiten modificar de forma
genérica el username, la contraseña, el contacto ni los atributos de identidad
de otra cuenta.

### RF60 — Consultar el perfil propio

El sistema permite al usuario autenticado consultar su username, nombre, rol,
tipo de perfil, documento enmascarado, correo electrónico, teléfono, sede y
código cuando corresponda a su perfil. La respuesta no contiene contraseña,
hash de contraseña, JWT, CSRF ni documento completo.

### RF61 — Actualizar datos de contacto del perfil propio

El sistema permite al usuario autenticado modificar exclusivamente su correo
electrónico y teléfono. Nombre, username, rol, perfil institucional, documento,
sede y código permanecen como datos de solo lectura en esta operación.

### RF62 — Cambiar contraseña del perfil propio

El sistema permite al usuario autenticado cambiar voluntariamente su contraseña
enviando únicamente `passwordActual` y `passwordNueva`. La confirmación se
valida en frontend y no forma parte del contrato HTTP. La contraseña actual debe
coincidir con el hash vigente; la nueva debe tener entre 8 y 100 caracteres y
ser diferente. Tras el cambio, el backend incrementa `authVersion`, reemplaza la
cookie del navegador con un JWT nuevo, invalida los JWT anteriores y elimina el
estado CSRF previo. La respuesta es `204 No Content`.

### RF63 — Activar cuenta mediante invitación segura

El frontend dispone de la ruta pública `/activar-cuenta`, lee el token desde la
URL, lo conserva únicamente en memoria y elimina el query parameter del
historial. Presenta los estados válido, vencido, usado, revocado, inválido,
error y éxito sin mostrar datos de la cuenta. La integración backend para
generar, reenviar, revocar y consumir invitaciones de activación no está
presente en la línea base indicada.

## 3. Product Backlog

### PB-30 — Consultar y actualizar Mi Perfil

Como usuario autenticado, quiero consultar mi perfil y actualizar mis datos de
contacto para mantener actualizada mi información personal.

Criterios vigentes:

- presenta nombre, username, rol, perfil, documento enmascarado, sede y código
  como información de solo lectura;
- permite modificar únicamente email y teléfono;
- conserva los atributos protegidos fuera del payload de actualización;
- separa Contacto y Seguridad en pestañas visibles;
- no expone contraseñas, hashes, tokens ni documento completo.

Estado documentado: capacidad integrada mediante el endpoint de Mi Perfil y la
interfaz incorporada por el PR #59.

### PB-31 — Cambiar voluntariamente la contraseña

Como usuario autenticado, quiero cambiar voluntariamente mi contraseña desde
Mi Perfil para mantener la seguridad de mi cuenta.

Criterios vigentes:

- solicita contraseña actual, contraseña nueva y confirmación visual;
- envía al backend solo la contraseña actual y la nueva;
- exige entre 8 y 100 caracteres y diferencia frente a la contraseña vigente;
- bloquea envíos duplicados mientras procesa la solicitud;
- renueva la sesión del navegador que realizó el cambio;
- revoca los JWT emitidos antes del cambio e invalida el CSRF anterior;
- no registra valores de contraseña, hashes, JWT ni cuerpos sensibles en la
  auditoría.

Estado documentado: interfaz integrada por el PR #59 y política backend
integrada por el PR #77.

### PB-32 — Activar una cuenta mediante invitación

Como nuevo usuario institucional, quiero activar mi cuenta mediante una
invitación segura para establecer mi contraseña inicial y acceder al sistema.

Estado documentado: el PR #69 integró la ruta pública, el formulario, el manejo
en memoria del token y los estados visuales. La API de activación y las
operaciones administrativas de generación, reenvío y revocación no están
presentes en `develop`.

El contrato funcional de PB-32 excluye el documento de identidad como
contraseña inicial y como campo del payload de activación.

### Procedimiento especificado de reenvío

El procedimiento documental para la capacidad de reenvío es:

1. un usuario administrativo autorizado solicita el reenvío para una cuenta
   pendiente de activación;
2. el backend invalida cualquier invitación activa anterior de esa cuenta;
3. genera un token aleatorio nuevo y persiste únicamente su hash, vigencia,
   estado y relación con la cuenta;
4. construye el enlace público con `FRONTEND_URL` y la ruta
   `/activar-cuenta?token=<valor>`;
5. envía el enlace mediante el proveedor configurado con `BREVO_API_KEY` y
   `MAIL_FROM_EMAIL`;
6. retorna una respuesta que no expone token, documento, contraseña ni datos
   adicionales de la cuenta;
7. registra la operación sin almacenar el token completo.

Este procedimiento forma parte de la especificación de PB-32. La línea base no
contiene el endpoint que lo ejecute.

## 4. CU-01 — Login, autenticación y gestión de sesión

### Estado

Login, consulta de sesión, logout, recuperación y restablecimiento de
contraseña están implementados. La interfaz pública de activación está
integrada; el procesamiento backend de invitaciones no está presente.

### Flujo de autenticación integrado

1. El usuario proporciona username y contraseña.
2. El backend valida cuenta, rol, perfil, contraseña y versión de autenticación.
3. El backend emite `access_token` en cookie `HttpOnly`.
4. El frontend consulta `/api/auth/me` y construye la navegación con los
   permisos efectivos.
5. El logout elimina la cookie e invalida el CSRF vigente.

### Recuperación integrada

1. El usuario proporciona su correo.
2. El backend entrega una respuesta genérica que no confirma la existencia de
   la cuenta.
3. Cuando corresponde, genera un token temporal, guarda solo su hash y envía el
   enlace configurado con `FRONTEND_URL`.
4. El usuario establece y confirma la nueva contraseña.
5. El backend consume el token, actualiza el hash de contraseña e incrementa
   `authVersion`.

### Activación visible en frontend

La ruta `/activar-cuenta?token=<valor>` retira el token de la URL tras leerlo,
no lo persiste en almacenamiento del navegador y evita el doble envío. Los
estados se presentan sin enumerar cuentas. El adaptador frontend apunta a
`POST /api/auth/activar-cuenta`; esa ruta no está expuesta por el backend de la
línea base.

## 5. CU-11 — Gestión del perfil propio

### Estado

Consulta de perfil, actualización de contacto y cambio voluntario de contraseña
están integrados.

### Datos visibles y editables

| Dato | Tratamiento |
|---|---|
| Nombre | Solo lectura. |
| Username | Solo lectura. |
| Rol | Solo lectura. |
| Tipo de perfil | Solo lectura. |
| Documento | Solo lectura y enmascarado. |
| Sede | Solo lectura. |
| Código | Solo lectura. |
| Email | Editable. |
| Teléfono | Editable. |
| Contraseñas | Entrada temporal del formulario; nunca se retornan desde la API de perfil. |

### Flujo de contacto

1. El usuario abre Mi Perfil.
2. El frontend consulta `GET /api/mi-perfil`.
3. El usuario modifica email o teléfono.
4. El frontend envía `PATCH /api/mi-perfil/contacto` con esos dos campos.
5. El backend actualiza el perfil resuelto desde la sesión y retorna el perfil
   actualizado.

### Flujo de seguridad

1. El usuario proporciona contraseña actual, nueva contraseña y confirmación.
2. El frontend valida la confirmación y envía solo `passwordActual` y
   `passwordNueva`.
3. El backend verifica la contraseña actual, política y diferencia.
4. El backend guarda el nuevo hash BCrypt, incrementa `authVersion`, renueva la
   cookie del navegador e invalida el CSRF anterior.
5. La respuesta `204 No Content` confirma el cambio sin exponer credenciales.

## 6. Diccionario y esquema

La política de sesión incorporó `usuario_sistema.auth_version` mediante
`backend/app/src/main/resources/db/migration/V20261102_01__add_auth_version_to_usuario_sistema.sql`.

| Campo | Tipo | Regla | Uso |
|---|---|---|---|
| `auth_version` | `BIGINT` | `NOT NULL DEFAULT 0` | Revoca JWT anteriores cuando cambia o se restablece la contraseña. |

No existe en esta línea base una entidad persistente de invitación de
activación que pueda añadirse al diccionario.

## 7. Swagger y OpenAPI

Springdoc genera el contrato desde los controllers versionados. En la línea
base, OpenAPI incluye las operaciones de login, sesión, logout, cambio de
contraseña, solicitud de recuperación, restablecimiento y Mi Perfil. El esquema
no contiene una operación `/api/auth/activar-cuenta` ni endpoints de generación,
reenvío o revocación de invitaciones.

| Método | Ruta presente en controllers |
|---|---|
| `POST` | `/api/auth/login` |
| `GET` | `/api/auth/me` |
| `POST` | `/api/auth/logout` |
| `PATCH` | `/api/auth/cambiar-password` |
| `POST` | `/api/auth/solicitar-recuperacion` |
| `POST` | `/api/auth/restablecer-password` |
| `GET` | `/api/mi-perfil` |
| `PATCH` | `/api/mi-perfil/contacto` |

La documentación interactiva está expuesta por las rutas públicas GET de
Springdoc configuradas en `SecurityConfig`.

## 8. Contratos y enlaces versionados

- API de autenticación: `doc/api/autenticacion.md`.
- Frontend de sesión: `doc/frontend/autenticacion-sesion.md`.
- Activación pública: `doc/frontend/modulos/activacion-cuenta.md`.
- Seguridad y variables: `doc/02-configuracion-seguridad.md`.
- Despliegue y credenciales: `doc/mantenimiento/manual-despliegue-recuperacion.md`.
- Matriz y demo: `doc/evidencias/SCRUM-297-matriz-auth-perfil.md`.
