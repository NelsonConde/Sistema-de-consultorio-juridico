# API - Perfiles

> Documento validado contra el código fuente actualizado del sistema. La documentación describe únicamente comportamiento implementado en backend.


## 1. Propósito

La API de perfiles permite administrar los perfiles operativos del sistema: administrativos, asesores, monitores, estudiantes y conciliadores. Cada grupo de endpoints opera sobre un tipo de perfil concreto y mantiene separación entre lectura, creación, edición, cambio de estado y eliminación lógica.

---

## 2. Administrativos

Ruta base:

```http
/api/administrativos
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/administrativos` | Lista administrativos | `VER_ADMINISTRADORES`, `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/administrativos/activos` | Lista administrativos activos | `VER_PERFILES_AUXILIARES`, `VER_ADMINISTRADORES`, `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/administrativos/directoras` | Lista administrativos marcados como directora | `VER_ADMINISTRADORES`, `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/administrativos/{id}` | Obtiene un administrativo por id | `VER_ADMINISTRADORES`, `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| POST | `/api/administrativos` | Crea administrativo | `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| PUT | `/api/administrativos/{id}` | Actualiza datos del perfil | `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/administrativos/{id}/activo?activo=true|false` | Cambia estado activo | `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/administrativos/{id}/directora?directora=true|false` | Cambia marca de directora | `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |
| DELETE | `/api/administrativos/{id}` | Desactiva lógicamente | `GESTIONAR_ADMINISTRADORES`, `GESTIONAR_USUARIOS` |

### Cuerpo base

```json
{
  "nombre": "Nombre completo",
  "tipoDocumentoId": 1,
  "documento": "1090000000",
  "email": "usuario@correo.com",
  "telefono": "3000000000",
  "usuario": "usuario",
  "codigo": "ADM001",
  "sedeId": 1,
  "directora": false
}
```

---

## 3. Asesores

Ruta base:

```http
/api/asesores
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/asesores` | Lista asesores | `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/asesores/activos` | Lista asesores activos | `VER_PERFILES_AUXILIARES`, `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/asesores/{id}` | Obtiene asesor por id | `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| POST | `/api/asesores` | Crea asesor | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| PUT | `/api/asesores/{id}` | Actualiza asesor | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/asesores/{id}/activo?activo=true|false` | Cambia estado activo | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| DELETE | `/api/asesores/{id}` | Desactiva lógicamente | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |

### Cuerpo base

```json
{
  "nombre": "Nombre completo",
  "tipoDocumentoId": 1,
  "documento": "1090000000",
  "email": "asesor@correo.com",
  "telefono": "3000000000",
  "usuario": "asesor",
  "codigo": "ASE001",
  "sedeId": 1,
  "areaId": 1
}
```

---

## 4. Monitores

Ruta base:

```http
/api/monitores
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/monitores` | Lista monitores | `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/monitores/activos` | Lista monitores activos | `VER_PERFILES_AUXILIARES`, `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/monitores/{id}` | Obtiene monitor | `VER_ASESORES_MONITORES`, `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| POST | `/api/monitores` | Crea monitor | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| PUT | `/api/monitores/{id}` | Actualiza monitor | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/monitores/{id}/activo?activo=true|false` | Cambia estado activo | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |
| DELETE | `/api/monitores/{id}` | Desactiva lógicamente | `GESTIONAR_ASESORES_MONITORES`, `GESTIONAR_USUARIOS` |

---

## 5. Estudiantes

Ruta base:

```http
/api/estudiantes
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/estudiantes` | Lista estudiantes | `VER_ESTUDIANTES`, `VER_PERFILES_AUXILIARES`, `GESTIONAR_USUARIOS` |
| GET | `/api/estudiantes/activos` | Lista estudiantes activos | `VER_ESTUDIANTES`, `VER_PERFILES_AUXILIARES`, `GESTIONAR_USUARIOS` |
| GET | `/api/estudiantes/conciliacion` | Lista estudiantes habilitados para conciliación | `VER_ESTUDIANTES`, `VER_PERFILES_AUXILIARES`, `GESTIONAR_USUARIOS` |
| GET | `/api/estudiantes/activos/asesor/{asesorId}` | Lista estudiantes activos por asesor | `VER_ESTUDIANTES`, `VER_PERFILES_AUXILIARES`, `GESTIONAR_USUARIOS` |
| GET | `/api/estudiantes/{id}` | Obtiene estudiante | `VER_ESTUDIANTES`, `VER_PERFILES_AUXILIARES`, `GESTIONAR_USUARIOS` |
| POST | `/api/estudiantes` | Crea estudiante | `CREAR_USUARIOS`, `GESTIONAR_USUARIOS` |
| PUT | `/api/estudiantes/{id}` | Actualiza estudiante | `EDITAR_USUARIOS`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/estudiantes/{id}/activo?activo=true|false` | Cambia estado activo | `CAMBIAR_ESTADO_ESTUDIANTES`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/estudiantes/{id}/conciliacion?conciliacion=true|false` | Habilita o deshabilita conciliación | `EDITAR_USUARIOS`, `GESTIONAR_USUARIOS` |
| DELETE | `/api/estudiantes/{id}` | Desactiva lógicamente | `GESTIONAR_USUARIOS` |
| POST | `/api/estudiantes/importar` | Importa estudiantes desde archivo | `GESTIONAR_USUARIOS` |

### Importación

```http
POST /api/estudiantes/importar
```

Permiso requerido:

```text
GESTIONAR_USUARIOS
```

Tipo de contenido:

```http
multipart/form-data
```

Parámetro:

| Nombre | Tipo | Requerido | Descripción |
|---|---|---|---|
| `archivo` | archivo | Sí | Archivo procesado mediante `XSSFWorkbook`. |

#### Estructura esperada

La primera fila contiene los encabezados en el orden definido por el servicio. La comparación del texto se realiza sin distinguir mayúsculas y minúsculas:

```text
Nombre
TipoDocumentoId
Documento
Email
Telefono
Usuario
SedeId
Codigo
AsesorId
Activo
Conciliacion
```

Las filas siguientes se transforman en datos de estudiante y se procesan individualmente. Una fila con error incrementa el contador de fallidos y agrega su mensaje a `errores`, conservando el procesamiento de las demás filas.

#### Respuesta `200 OK`

El endpoint retorna `ImportacionEstudiantesDTO`, incluyendo resultados exitosos y fallidos de las filas procesadas:

```json
{
  "totalFilas": 3,
  "exitosos": 2,
  "fallidos": 1,
  "errores": [
    "Fila 3: <mensaje>"
  ]
}
```

#### Respuestas construidas por la operación

| Estado | Cuerpo | Condición procesada |
|---|---|---|
| `400 Bad Request` | Texto | El servicio genera `IllegalArgumentException`, por ejemplo cuando el archivo no contiene encabezados. |
| `500 Internal Server Error` | Texto iniciado por `Error interno: ` | El servicio genera `RuntimeException`, incluyendo errores de formato de columnas o lectura del archivo. |

---

## 6. Conciliadores

Ruta base:

```http
/api/conciliadores
```

| Método | Endpoint | Descripción | Permisos principales |
|---|---|---|---|
| GET | `/api/conciliadores` | Lista conciliadores | `VER_CONCILIADORES`, `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/conciliadores/activos` | Lista conciliadores activos | `VER_PERFILES_AUXILIARES`, `VER_CONCILIADORES`, `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |
| GET | `/api/conciliadores/{id}` | Obtiene conciliador | `VER_CONCILIADORES`, `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |
| POST | `/api/conciliadores` | Crea conciliador | `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |
| PUT | `/api/conciliadores/{id}` | Actualiza conciliador | `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |
| PATCH | `/api/conciliadores/{id}/activo?activo=true|false` | Cambia estado activo | `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |
| DELETE | `/api/conciliadores/{id}` | Desactiva lógicamente | `GESTIONAR_CONCILIADORES`, `GESTIONAR_USUARIOS` |

---

## 7. Reglas transversales de la API de perfiles

- En creación no se debe enviar `id`.
- En actualización no se permite cambiar el `id` del perfil.
- `PUT` actualiza datos generales del perfil.
- `PATCH /activo` cambia estado lógico.
- `DELETE` realiza desactivación lógica.
- La desactivación de asesores, estudiantes y monitores valida consultas operativas asociadas.
- La activación/desactivación realizada desde un endpoint de perfil sincroniza el estado del `UsuarioSistema` asociado.
- Las respuestas devuelven DTOs del perfil correspondiente.

---

## 8. Relación con UsuarioSistema

Cada perfil operativo se vincula con un usuario del sistema. Esa relación permite que autenticación, rol, permisos y alcance se resuelvan desde una cuenta de acceso única, conservando la identidad funcional en el módulo correspondiente.


---

## 9. Precisiones de alcance y validaciones efectivas

La anotación de seguridad de los controllers expresa los permisos de entrada al endpoint. Además de esa validación, los servicios de acceso aplican reglas internas de alcance por perfil autenticado. Estas reglas hacen parte del comportamiento efectivo de la API.

### 9.1 Administrativos y marca de directora

La gestión de administrativos tiene una regla especial. Para crear, actualizar, cambiar estado, cambiar marca de directora o desactivar administrativos, el backend valida que el usuario autenticado:

- opere como administrador;
- tenga un perfil administrativo activo;
- y que ese perfil administrativo tenga `directora=true`.

La consulta de administrativos también exige operar como administrador. Esta regla se implementa en `AdministrativoAccessService`.

### 9.2 Alcance de estudiantes

El listado de estudiantes aplica alcance funcional:

- el administrador puede consultar todos los estudiantes según el endpoint usado;
- el asesor consulta únicamente estudiantes asociados a su perfil de asesor;
- otros perfiles no reciben estudiantes desde el query service cuando no tienen alcance funcional.

El endpoint `GET /api/estudiantes/activos/asesor/{asesorId}` valida adicionalmente el asesor solicitado. El administrador puede consultar estudiantes activos de cualquier asesor; un asesor autenticado solo puede consultar estudiantes de su propio perfil.

### 9.3 Estudiantes habilitados para conciliación

`GET /api/estudiantes/conciliacion` devuelve estudiantes activos con `conciliacion=true`. Este endpoint se usa como catálogo operativo para conciliación. Los resultados también pasan por la regla de visibilidad del servicio de estudiantes.

### 9.4 Importación masiva de estudiantes

`POST /api/estudiantes/importar` recibe un archivo Excel en el parámetro multipart `archivo`. El backend procesa la primera hoja del libro y valida estos encabezados en el orden definido, sin distinguir mayúsculas y minúsculas:

| Columna | Uso |
|---|---|
| `Nombre` | Nombre completo del estudiante. |
| `TipoDocumentoId` | Identificador del tipo de documento. |
| `Documento` | Número de documento. |
| `Email` | Correo del estudiante y base para su usuario del sistema. |
| `Telefono` | Teléfono. |
| `Usuario` | Nombre de usuario funcional del perfil. |
| `SedeId` | Sede asociada. |
| `Codigo` | Código del estudiante. |
| `AsesorId` | Asesor activo asociado. |
| `Activo` | Estado lógico inicial. |
| `Conciliacion` | Habilitación para apoyar conciliación. |

El procesamiento inicia desde la segunda fila. Cada fila se transforma en un `EstudianteDTO` y se procesa mediante el mismo servicio de creación individual de estudiante. El resultado puede ser mixto: filas creadas correctamente y filas rechazadas por validaciones. La respuesta `ImportacionEstudiantesDTO` resume total de filas, exitosos, fallidos y errores por fila.

### 9.5 Estado del perfil y UsuarioSistema

Cuando el cambio de estado se realiza desde el endpoint propio del perfil (`PATCH /activo` o `DELETE` lógico de administrativos, asesores, estudiantes, monitores o conciliadores), el backend sincroniza el estado del `UsuarioSistema` asociado.

El cambio directo de estado de `UsuarioSistema` mediante `/api/usuarios-sistema/{id}/activo` modifica la cuenta de acceso. Ese endpoint no reactiva ni desactiva automáticamente el perfil real asociado.
