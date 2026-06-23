# API - Usuarios del sistema, roles, permisos y cambio de perfil

> Documento validado contra el código fuente actualizado del sistema. La documentación describe únicamente comportamiento implementado en backend.


## 1. Propósito

Esta sección documenta los endpoints relacionados con cuentas de acceso, roles, permisos y cambio de perfil. El sistema separa las operaciones de seguridad en tres grupos:

- usuarios del sistema;
- roles;
- permisos.

Además, implementa endpoints específicos para cambiar el perfil activo de un usuario y actualizar su rol asociado.

### Representación de permisos en este contrato

En las tablas de endpoints, identificadores como `VER_USUARIOS` y `GESTIONAR_ROLES` corresponden a constantes de `PermisoNombre` utilizadas por los controllers. El valor textual de la constante es la autoridad evaluada y el dato expuesto en los DTOs.

Ejemplo:

| Constante de código | Valor expuesto |
|---|---|
| `VER_CONSULTAS` | `"Ver consultas"` |
| `EDITAR_CONSULTAS` | `"Editar consultas"` |

Por esta razón, los ejemplos JSON de usuarios, roles y permisos utilizan los nombres textuales retornados por los mappers.

---

## 2. Usuarios del sistema

Ruta base:

```http
/api/usuarios-sistema
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/usuarios-sistema` | Lista usuarios del sistema | `VER_USUARIOS`, `GESTIONAR_USUARIOS` |
| GET | `/api/usuarios-sistema/activos` | Lista usuarios activos | `VER_USUARIOS`, `GESTIONAR_USUARIOS` |
| GET | `/api/usuarios-sistema/{id}` | Obtiene usuario por id | `VER_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/usuarios-sistema/{id}/activo?activo=true|false` | Cambia estado activo | `CAMBIAR_ESTADO_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/usuarios-sistema/{id}/perfil/administrativo` | Cambia perfil a administrativo | `ASIGNAR_ROL_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/usuarios-sistema/{id}/perfil/estudiante` | Cambia perfil a estudiante | `ASIGNAR_ROL_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/usuarios-sistema/{id}/perfil/asesor` | Cambia perfil a asesor | `ASIGNAR_ROL_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/usuarios-sistema/{id}/perfil/monitor` | Cambia perfil a monitor | `ASIGNAR_ROL_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/usuarios-sistema/{id}/perfil/conciliador` | Cambia perfil a conciliador | `ASIGNAR_ROL_USUARIOS`, `GESTIONAR_USUARIOS` |

### UsuarioSistemaDTO

```json
{
  "id": 1,
  "username": "usuario",
  "activo": true,
  "rolId": 2,
  "rolNombre": "ASESOR",
  "perfilId": 10,
  "tipoPerfil": "ASESOR",
  "permisos": ["Editar consultas", "Ver consultas"]
}
```

---

## 3. Cambio de perfil

Los endpoints de cambio de perfil reciben un DTO específico según el perfil destino. Todos usan `PATCH` y son gestionados por `UsuarioCambioPerfilService`.

El flujo implementado es:

1. validar usuario destino;
2. validar que el usuario esté activo;
3. validar que el rol actual esté activo;
4. validar que el perfil destino sea diferente al actual;
5. validar que el rol destino corresponda al tipo de perfil destino;
6. normalizar datos básicos;
7. crear o reactivar el perfil destino mediante Strategy;
8. desactivar el perfil anterior mediante Strategy;
9. actualizar rol y tipo de perfil actual del usuario;
10. registrar historial del cambio.

### Campos transversales del cambio

Los DTOs de cambio de perfil extienden `CambiarPerfilBaseDTO` y comparten datos básicos como:

- nombre;
- tipo de documento, cuando el handler destino lo exige o lo admite;
- documento, obligatorio u opcional según el perfil destino;
- teléfono;
- usuario;
- código;
- sede, obligatoria u opcional según el perfil destino;
- rol destino;
- motivo del cambio.

Los perfiles especializados agregan campos propios, por ejemplo:

- asesor: `areaId`;
- estudiante: `asesorId`;
- conciliador: `tipoConciliador`;
- administrativo: `directora` cuando aplique.

---

## 4. Roles

Ruta base:

```http
/api/roles
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/roles` | Lista roles | `GESTIONAR_ROLES`, `VER_ROLES` |
| GET | `/api/roles/activos` | Lista roles activos | `GESTIONAR_ROLES`, `VER_ROLES` |
| GET | `/api/roles/{id}` | Obtiene rol | `GESTIONAR_ROLES`, `VER_ROLES` |
| POST | `/api/roles` | Crea rol | `GESTIONAR_ROLES`, `CREAR_ROLES` |
| PUT | `/api/roles/{id}` | Actualiza rol | `GESTIONAR_ROLES`, `EDITAR_ROLES` |
| PATCH | `/api/roles/{id}/activo?activo=true|false` | Cambia estado activo | `GESTIONAR_ROLES`, `EDITAR_ROLES` |
| PATCH | `/api/roles/{rolId}/permisos/{permisoId}` | Asigna permiso al rol | `GESTIONAR_ROLES`, `ASIGNAR_PERMISOS_A_ROLES` |
| DELETE | `/api/roles/{rolId}/permisos/{permisoId}` | Quita permiso del rol | `GESTIONAR_ROLES`, `ASIGNAR_PERMISOS_A_ROLES` |

### RolDTO

```json
{
  "id": 1,
  "nombre": "ASESOR",
  "descripcion": "Rol de asesor jurídico",
  "activo": true,
  "permisoIds": [1, 2, 3],
  "permisos": [
    { "id": 1, "nombre": "Ver consultas", "descripcion": "Descripción registrada para el permiso", "activo": true }
  ]
}
```

---

## 5. Permisos

Ruta base:

```http
/api/permisos
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/permisos` | Lista permisos | `GESTIONAR_PERMISOS`, `ASIGNAR_PERMISOS_A_ROLES` |
| GET | `/api/permisos/activos` | Lista permisos activos | `GESTIONAR_PERMISOS`, `ASIGNAR_PERMISOS_A_ROLES` |
| GET | `/api/permisos/{id}` | Obtiene permiso | `GESTIONAR_PERMISOS`, `ASIGNAR_PERMISOS_A_ROLES` |
| POST | `/api/permisos` | Crea permiso | `GESTIONAR_PERMISOS` |
| PUT | `/api/permisos/{id}` | Actualiza permiso | `GESTIONAR_PERMISOS` |
| PATCH | `/api/permisos/{id}/activo?activo=true|false` | Cambia estado activo | `GESTIONAR_PERMISOS` |

### PermisoDTO

```json
{
  "id": 1,
  "nombre": "Ver consultas",
  "descripcion": "Permite consultar casos jurídicos",
  "activo": true
}
```

---

## 6. Reglas implementadas

- Los roles y permisos tienen estado lógico `activo`.
- La asignación de permisos a roles se realiza por endpoint explícito.
- El cambio de perfil requiere rol destino coherente con el tipo de perfil destino.
- El cambio de perfil registra historial con motivo obligatorio.
- El perfil anterior se desactiva usando Strategy.
- El perfil activo se resuelve usando Strategy.
- La cuenta de usuario conserva su identidad de acceso y actualiza el perfil operativo vigente.


---

## 7. Precisiones del cambio de perfil

El cambio de perfil usa DTOs especializados y estrategias por perfil destino. Estos DTOs corresponden a la transición de perfil y se distinguen de los DTOs utilizados para la creación de perfiles operativos.

### 7.1 Correo del perfil destino

En el cambio de perfil, el correo del perfil destino se toma del `username` del `UsuarioSistema`, conservando la identidad de acceso de la cuenta.

### 7.2 Obligatoriedad por perfil destino

La obligatoriedad de algunos campos comunes se valida en el handler concreto de Strategy:

| Perfil destino | Campos específicos y obligatoriedad efectiva |
|---|---|
| Administrativo | `directora` opcional; `documento`, `tipoDocumentoId` y `sedeId` pueden ser opcionales en este flujo. |
| Asesor | `documento`, `tipoDocumentoId`, `sedeId` y `areaId` obligatorios. |
| Estudiante | `documento`, `tipoDocumentoId`, `sedeId` y `asesorId` obligatorios; `conciliacion` opcional. |
| Monitor | `documento`, `tipoDocumentoId` y `sedeId` pueden ser opcionales en este flujo. |
| Conciliador | `documento` y `tipoConciliador` obligatorios; `tipoDocumentoId` y `sedeId` opcionales. |

Los campos comunes obligatorios para el cambio de perfil son `rolId`, `motivo`, `nombre`, `telefono`, `usuario` y `codigo`, de acuerdo con `CambiarPerfilBaseDTO`.

### 7.3 Perfil destino reutilizado

Si el usuario ya tuvo previamente el perfil destino, el handler lo reutiliza y lo reactiva. La reutilización se aplica exclusivamente a un perfil asociado al mismo `UsuarioSistema`.

### 7.4 Perfil anterior y UsuarioSistema

Durante el cambio de perfil, el sistema desactiva el perfil anterior mediante `PerfilEstadoHandler` y conserva activo el `UsuarioSistema` con el perfil destino y el nuevo rol asociado.

---

## 8. Precisiones de roles y permisos

### 8.1 Creación y actualización de roles

En la creación de rol, el backend requiere `permisoIds` informado con al menos un elemento. Los permisos indicados deben existir y estar activos.

En la actualización de rol, al informar `permisoIds` el backend reemplaza la lista completa de permisos por la lista recibida; al omitir el campo, conserva los permisos existentes del rol.

La asignación individual mediante `PATCH /api/roles/{rolId}/permisos/{permisoId}` también exige que el permiso exista y esté activo. La remoción individual usa `DELETE /api/roles/{rolId}/permisos/{permisoId}`.

### 8.2 Administración del estado de permisos

El estado de un permiso se administra mediante `PATCH /api/permisos/{id}/activo?activo=true|false`.

### 8.3 UsuarioSistema y creación de cuentas

La cuenta de acceso se crea al registrar un perfil operativo desde su endpoint correspondiente.

`PATCH /api/usuarios-sistema/{id}/activo?activo=true|false` administra el estado de la cuenta de acceso. El estado del perfil operativo se administra mediante las operaciones del módulo de perfil correspondiente.
