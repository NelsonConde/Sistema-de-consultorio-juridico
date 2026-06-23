# Backend - Catálogos

El módulo de catálogos administra datos auxiliares usados por formularios, relaciones y reglas de negocio del sistema.

Los catálogos permiten mantener información normalizada para personas, consultas jurídicas, ubicación, clasificación y parametrización funcional.

## Paquetes principales

```text
business/controller/catalogo
business/dto/catalogo
business/model/catalogo
business/repository/catalogo
business/service/catalogo
business/service/catalogo/{submodulo}
business/controller/proceso
business/dto/proceso
business/model/proceso
business/repository/proceso
business/service/proceso
business/service/proceso/catalogo
```

## Permisos usados

El módulo usa permisos centralizados en `PermisoNombre`.

| Permiso | Uso |
|---|---|
| `Ver catálogos` | Permite consultar catálogos activos y obtener registros por id. |
| `Gestionar catálogos` | Permite administrar catálogos, consultar todos, crear, actualizar, cambiar estado y eliminar lógicamente cuando aplica. |

## Catálogos administrados

| Catálogo | Controller | Endpoint base |
|---|---|---|
| Área | `AreaController` | `/api/areas` |
| Tema | `TemaController` | `/api/temas` |
| Tipo | `TipoController` | `/api/tipos` |
| Departamento | `DepartamentoController` | `/api/departamentos` |
| Municipio | `MunicipioController` | `/api/municipios` |
| Barrio | `BarrioController` | `/api/barrios` |
| Sede | `SedeController` | `/api/sedes` |
| Nacionalidad | `NacionalidadController` | `/api/nacionalidades` |
| Tipo de documento | `TipoDocumentoController` | `/api/tipos-documento` |
| Órgano de control | `OrganoControlController` | `/api/organos-control` |
| Especialidad | `EspecialidadController` | `/api/especialidades` |

## Entidades

| Entidad | Tabla | Campos principales | Relaciones |
|---|---|---|---|
| `Area` | `area` | `id`, `nombre`, `activo` | Tiene temas. |
| `Tema` | `tema` | `id`, `nombre`, `activo` | Pertenece a un área y tiene tipos. |
| `Tipo` | `tipo` | `id`, `nombre`, `activo` | Pertenece a un tema. |
| `Departamento` | `departamento` | `id`, `nombre`, `activo` | Agrupa municipios. |
| `Municipio` | `municipio` | `id`, `nombre`, `activo` | Pertenece a un departamento y agrupa barrios. |
| `Barrio` | `barrio` | `id`, `nombre`, `activo` | Pertenece a un municipio. |
| `Sede` | `sede` | `id`, `nombre`, `activo` | Se usa como sede del sistema. |
| `Nacionalidad` | `nacionalidades` | `id`, `nombre`, `activo` | Se usa en datos de persona. |
| `TipoDocumento` | `tipodoc` | `id`, `nombre`, `activo` | Se usa en identificación de personas y usuarios. |
| `OrganoControl` | `organo_control` | `id`, `nombre`, `activo` | Agrupa especialidades asociadas a procesos. |
| `Especialidad` | `especialidad` | `id`, `nombre`, `activo`, `organo_control_id` | Pertenece a un órgano de control. |

## DTOs

| DTO | Campos | Validaciones principales |
|---|---|---|
| `AreaDTO` | `id`, `nombre`, `activo` | `nombre` obligatorio, máximo 50 caracteres. |
| `TemaDTO` | `id`, `nombre`, `areaId`, `activo` | `nombre` obligatorio, máximo 80 caracteres, `areaId` obligatorio. |
| `TipoDTO` | `id`, `nombre`, `temaId`, `activo` | `nombre` obligatorio, máximo 80 caracteres, `temaId` obligatorio. |
| `DepartamentoDTO` | `id`, `nombre`, `activo` | `nombre` obligatorio, máximo 80 caracteres. |
| `MunicipioDTO` | `id`, `nombre`, `departamentoId`, `activo` | `nombre` obligatorio, máximo 100 caracteres, `departamentoId` obligatorio. |
| `BarrioDTO` | `id`, `nombre`, `municipioId`, `activo` | `nombre` obligatorio, máximo 100 caracteres, `municipioId` obligatorio. |
| `SedeDTO` | `id`, `nombre`, `activo` | `nombre` obligatorio, máximo 100 caracteres. |
| `NacionalidadDTO` | `id`, `nombre`, `activo` | `nombre` obligatorio, máximo 100 caracteres. |
| `TipoDocumentoDTO` | `id`, `nombre`, `activo` | `nombre` obligatorio, máximo 100 caracteres. |
| `OrganoControlDTO` | `id`, `nombre`, `activo` | `nombre` obligatorio, máximo 80 caracteres. |
| `EspecialidadDTO` | `id`, `nombre`, `organoControlId`, `activo` | `nombre` obligatorio, máximo 80 caracteres; `organoControlId` obligatorio. |

## Patrón general del módulo

La mayoría de catálogos sigue este patrón:

| Operación | Descripción |
|---|---|
| Listar activos | Devuelve registros con `activo=true`, ordenados por nombre. |
| Listar todos | Devuelve registros activos e inactivos para administración. |
| Obtener por id | Consulta un registro por identificador. |
| Crear | Valida DTO, normaliza nombre, valida duplicados y guarda activo. |
| Actualizar | Valida DTO, valida id, valida duplicados y aplica cambios. |
| Cambiar estado | Cambia el campo `activo`. |
| Eliminar | Realiza desactivación lógica cuando el catálogo lo permite. |

## Normalización

Los validators usan normalización de texto para limpiar entradas antes de validar y persistir.

La normalización permite:

- eliminar espacios innecesarios;
- validar nombres vacíos después de limpiar;
- comparar nombres de forma consistente;
- evitar duplicados por diferencias superficiales de escritura.

## Reglas comunes

### Creación

En creación:

- no se permite enviar `id`;
- el DTO debe estar presente;
- el nombre es obligatorio;
- el nombre se normaliza;
- se valida longitud máxima;
- se valida disponibilidad del nombre;
- se guarda el registro con `activo=true`.

### Actualización

En actualización:

- el DTO debe estar presente;
- si el DTO trae `id`, debe coincidir con el `id` de la ruta;
- se normalizan los datos;
- se valida disponibilidad del nombre;
- se valida que existan cambios efectivos;
- se actualiza el registro existente.

### Cambio de estado

En cambio de estado:

- el parámetro `activo` es obligatorio;
- no se permite cambiar al mismo estado actual;
- se actualiza el campo `activo`.

### Eliminación

En los catálogos con endpoint `DELETE`, la eliminación se implementa como desactivación lógica mediante `activo=false` y responde `204 No Content`.

Esto conserva referencias históricas de otros módulos.

### Tipo de documento

`TipoDocumentoController` administra el estado mediante `PATCH /{id}/activo`. `GET /api/tipos-documento` lista todos los registros, `GET /api/tipos-documento/activos` lista únicamente activos y `TipoDocumentoService.obtenerPorId` consulta el registro por identificador, incluyendo registros activos o inactivos.

Este catálogo conserva los registros para no afectar información asociada a casos, personas o usuarios.

## Reglas de duplicado

| Catálogo | Regla de duplicado |
|---|---|
| Área | Nombre único. |
| Departamento | Nombre único. |
| Nacionalidad | Nombre único. |
| Sede | Nombre único. |
| Tipo de documento | Nombre único. |
| Municipio | Nombre único dentro del departamento. |
| Barrio | Nombre único dentro del municipio. |
| Tema | Nombre único dentro del área. |
| Tipo | Nombre único dentro del tema. |
| Órgano de control | Nombre único. |
| Especialidad | Nombre único dentro del órgano de control. |

## Relaciones jerárquicas

El módulo maneja jerarquías:

```text
Departamento -> Municipio -> Barrio
Area -> Tema -> Tipo
Órgano de control -> Especialidad
```

Reglas principales:

- para crear o actualizar municipio se requiere departamento activo;
- para crear o actualizar barrio se requiere municipio activo;
- para crear o actualizar tema se requiere área activa;
- para crear o actualizar tipo se requiere tema activo;
- para crear o actualizar una especialidad se requiere órgano de control activo;
- la consulta de especialidades por órgano retorna especialidades activas de un órgano activo;
- para desactivar un órgano de control no deben existir especialidades activas asociadas;
- los endpoints de consulta por padre activo validan que el padre esté activo;
- los endpoints administrativos `/todos` de las jerarquías que los exponen validan las reglas definidas por cada service.

## Catálogos asociados a procesos

Los catálogos `OrganoControl` y `Especialidad` se implementan en el paquete funcional de procesos y suministran las relaciones seleccionables en la creación y actualización de un proceso.

### Componentes

| Catálogo | Controller | Service | Validator | Mapper | Repository |
|---|---|---|---|---|---|
| Órgano de control | `OrganoControlController` | `OrganoControlService` | `OrganoControlValidator` | `OrganoControlMapper` | `OrganoControlRepository` |
| Especialidad | `EspecialidadController` | `EspecialidadService` | `EspecialidadValidator` | `EspecialidadMapper` | `EspecialidadRepository` |

### Órgano de control

`OrganoControlService` implementa:

| Operación | Comportamiento |
|---|---|
| `listar()` | Consulta órganos activos ordenados por nombre y los convierte a DTO. |
| `listarTodos()` | Consulta registros activos e inactivos y los ordena por nombre sin distinguir mayúsculas y minúsculas. |
| `obtenerPorId(id)` | Obtiene un órgano activo. |
| `crear(dto)` | Valida creación, normaliza nombre, valida unicidad y persiste una entidad activa. |
| `actualizar(id, dto)` | Busca el registro por id, valida cambios y actualiza únicamente el nombre. |
| `cambiarEstado(id, activo)` | Cambia el estado; cuando el valor solicitado es `false`, valida la ausencia de especialidades activas. |
| `eliminar(id)` | Busca un órgano activo, valida la ausencia de especialidades activas y establece `activo=false`. |

`OrganoControlValidator` aplica las reglas de id, nombre normalizado, longitud máxima de 80 caracteres, unicidad sin distinguir mayúsculas y minúsculas, existencia de cambios y control de desactivación.

### Especialidad

`EspecialidadService` implementa:

| Operación | Comportamiento |
|---|---|
| `listar()` | Consulta especialidades activas ordenadas por nombre y las convierte a DTO. |
| `listarTodos()` | Consulta registros activos e inactivos y los ordena por nombre sin distinguir mayúsculas y minúsculas. |
| `listarPorOrganoControl(organoControlId)` | Obtiene un órgano activo y lista sus especialidades activas ordenadas por nombre. |
| `obtenerPorId(id)` | Obtiene una especialidad activa. |
| `crear(dto)` | Valida creación, normaliza nombre, obtiene el órgano activo, valida unicidad contextual y persiste una entidad activa. |
| `actualizar(id, dto)` | Busca el registro por id, obtiene el órgano activo indicado y actualiza nombre y asociación. |
| `cambiarEstado(id, activo)` | Cambia el estado cuando el valor solicitado difiere del registrado. |
| `eliminar(id)` | Busca una especialidad activa y establece `activo=false`. |

`EspecialidadValidator` aplica las reglas de id, nombre normalizado, longitud máxima de 80 caracteres, `organoControlId` obligatorio, unicidad del nombre dentro del órgano seleccionado y existencia de cambios.

### Persistencia y mapeo

| Entidad | Persistencia y exposición |
|---|---|
| `OrganoControl` | Se guarda en `organo_control`, inicia con `activo=true` y expone `id`, `nombre` y `activo` en `OrganoControlDTO`. |
| `Especialidad` | Se guarda en `especialidad`, referencia obligatoriamente `organo_control_id`, inicia con `activo=true` y expone `organoControlId` en `EspecialidadDTO`. |

La actualización de datos no modifica el campo `activo`; el ciclo de vida se gestiona mediante los endpoints de cambio de estado y desactivación lógica.


## Endpoints por catálogo

### Áreas

Base path:

```text
/api/areas
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/areas` | `Ver catálogos` o `Gestionar catálogos` | Lista áreas activas. |
| GET | `/api/areas/todos` | `Gestionar catálogos` | Lista todas las áreas. |
| GET | `/api/areas/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta área por id. |
| POST | `/api/areas` | `Gestionar catálogos` | Crea área. |
| PUT | `/api/areas/{id}` | `Gestionar catálogos` | Actualiza área. |
| PATCH | `/api/areas/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/areas/{id}` | `Gestionar catálogos` | Desactiva área. |

### Departamentos

Base path:

```text
/api/departamentos
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/departamentos` | `Ver catálogos` o `Gestionar catálogos` | Lista departamentos activos. |
| GET | `/api/departamentos/todos` | `Gestionar catálogos` | Lista todos los departamentos. |
| GET | `/api/departamentos/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta departamento por id. |
| POST | `/api/departamentos` | `Gestionar catálogos` | Crea departamento. |
| PUT | `/api/departamentos/{id}` | `Gestionar catálogos` | Actualiza departamento. |
| PATCH | `/api/departamentos/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/departamentos/{id}` | `Gestionar catálogos` | Desactiva departamento. |

### Municipios

Base path:

```text
/api/municipios
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/municipios` | `Ver catálogos` o `Gestionar catálogos` | Lista municipios activos. |
| GET | `/api/municipios/todos` | `Gestionar catálogos` | Lista todos los municipios. |
| GET | `/api/municipios/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta municipio por id. |
| GET | `/api/municipios/departamento/{departamentoId}` | `Ver catálogos` o `Gestionar catálogos` | Lista municipios activos de un departamento activo. |
| GET | `/api/municipios/departamento/{departamentoId}/todos` | `Gestionar catálogos` | Lista todos los municipios de un departamento. |
| POST | `/api/municipios` | `Gestionar catálogos` | Crea municipio. |
| PUT | `/api/municipios/{id}` | `Gestionar catálogos` | Actualiza municipio. |
| PATCH | `/api/municipios/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/municipios/{id}` | `Gestionar catálogos` | Desactiva municipio. |

### Barrios

Base path:

```text
/api/barrios
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/barrios` | `Ver catálogos` o `Gestionar catálogos` | Lista barrios activos. |
| GET | `/api/barrios/todos` | `Gestionar catálogos` | Lista todos los barrios. |
| GET | `/api/barrios/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta barrio por id. |
| GET | `/api/barrios/municipio/{municipioId}` | `Ver catálogos` o `Gestionar catálogos` | Lista barrios activos de un municipio activo. |
| GET | `/api/barrios/municipio/{municipioId}/todos` | `Gestionar catálogos` | Lista todos los barrios de un municipio. |
| POST | `/api/barrios` | `Gestionar catálogos` | Crea barrio. |
| PUT | `/api/barrios/{id}` | `Gestionar catálogos` | Actualiza barrio. |
| PATCH | `/api/barrios/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/barrios/{id}` | `Gestionar catálogos` | Desactiva barrio. |

### Sedes

Base path:

```text
/api/sedes
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/sedes` | `Ver catálogos` o `Gestionar catálogos` | Lista sedes activas. |
| GET | `/api/sedes/todos` | `Gestionar catálogos` | Lista todas las sedes. |
| GET | `/api/sedes/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta sede por id. |
| POST | `/api/sedes` | `Gestionar catálogos` | Crea sede. |
| PUT | `/api/sedes/{id}` | `Gestionar catálogos` | Actualiza sede. |
| PATCH | `/api/sedes/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/sedes/{id}` | `Gestionar catálogos` | Desactiva sede. |

### Nacionalidades

Base path:

```text
/api/nacionalidades
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/nacionalidades` | `Ver catálogos` o `Gestionar catálogos` | Lista nacionalidades activas. |
| GET | `/api/nacionalidades/todos` | `Gestionar catálogos` | Lista todas las nacionalidades. |
| GET | `/api/nacionalidades/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta nacionalidad por id. |
| POST | `/api/nacionalidades` | `Gestionar catálogos` | Crea nacionalidad. |
| PUT | `/api/nacionalidades/{id}` | `Gestionar catálogos` | Actualiza nacionalidad. |
| PATCH | `/api/nacionalidades/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/nacionalidades/{id}` | `Gestionar catálogos` | Desactiva nacionalidad. |

### Temas

Base path:

```text
/api/temas
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/temas` | `Ver catálogos` o `Gestionar catálogos` | Lista temas activos. |
| GET | `/api/temas/todos` | `Gestionar catálogos` | Lista todos los temas. |
| GET | `/api/temas/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta tema por id. |
| GET | `/api/temas/area/{areaId}` | `Ver catálogos` o `Gestionar catálogos` | Lista temas activos de un área activa. |
| GET | `/api/temas/area/{areaId}/todos` | `Gestionar catálogos` | Lista todos los temas de un área. |
| POST | `/api/temas` | `Gestionar catálogos` | Crea tema. |
| PUT | `/api/temas/{id}` | `Gestionar catálogos` | Actualiza tema. |
| PATCH | `/api/temas/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/temas/{id}` | `Gestionar catálogos` | Desactiva tema. |

### Tipos

Base path:

```text
/api/tipos
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/tipos` | `Ver catálogos` o `Gestionar catálogos` | Lista tipos activos. |
| GET | `/api/tipos/todos` | `Gestionar catálogos` | Lista todos los tipos. |
| GET | `/api/tipos/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta tipo por id. |
| GET | `/api/tipos/tema/{temaId}` | `Ver catálogos` o `Gestionar catálogos` | Lista tipos activos de un tema activo. |
| GET | `/api/tipos/tema/{temaId}/todos` | `Gestionar catálogos` | Lista todos los tipos de un tema. |
| POST | `/api/tipos` | `Gestionar catálogos` | Crea tipo. |
| PUT | `/api/tipos/{id}` | `Gestionar catálogos` | Actualiza tipo. |
| PATCH | `/api/tipos/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/tipos/{id}` | `Gestionar catálogos` | Desactiva tipo. |

### Tipos de documento

Base path:

```text
/api/tipos-documento
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/tipos-documento` | `Ver catálogos` o `Gestionar catálogos` | Lista tipos de documento. |
| GET | `/api/tipos-documento/activos` | `Ver catálogos` o `Gestionar catálogos` | Lista tipos de documento activos. |
| GET | `/api/tipos-documento/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta tipo de documento por id. |
| POST | `/api/tipos-documento` | `Gestionar catálogos` | Crea tipo de documento. |
| PUT | `/api/tipos-documento/{id}` | `Gestionar catálogos` | Actualiza tipo de documento. |
| PATCH | `/api/tipos-documento/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |

### Órganos de control

Base path:

```text
/api/organos-control
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/organos-control` | `Ver catálogos` o `Gestionar catálogos` | Lista órganos activos ordenados por nombre. |
| GET | `/api/organos-control/todos` | `Gestionar catálogos` | Lista órganos activos e inactivos ordenados por nombre. |
| GET | `/api/organos-control/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta un órgano activo por id. |
| POST | `/api/organos-control` | `Gestionar catálogos` | Crea un órgano activo y responde `201 Created`. |
| PUT | `/api/organos-control/{id}` | `Gestionar catálogos` | Actualiza datos sin modificar estado. |
| PATCH | `/api/organos-control/{id}/activo?activo=` | `Gestionar catálogos` | Cambia estado y retorna DTO. |
| DELETE | `/api/organos-control/{id}` | `Gestionar catálogos` | Desactiva lógicamente y responde `204 No Content`. |

### Especialidades

Base path:

```text
/api/especialidades
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/especialidades` | `Ver catálogos` o `Gestionar catálogos` | Lista especialidades activas ordenadas por nombre. |
| GET | `/api/especialidades/todos` | `Gestionar catálogos` | Lista especialidades activas e inactivas ordenadas por nombre. |
| GET | `/api/especialidades/organo-control/{organoControlId}` | `Ver catálogos` o `Gestionar catálogos` | Lista especialidades activas de un órgano activo. |
| GET | `/api/especialidades/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta una especialidad activa por id. |
| POST | `/api/especialidades` | `Gestionar catálogos` | Crea una especialidad activa y responde `201 Created`. |
| PUT | `/api/especialidades/{id}` | `Gestionar catálogos` | Actualiza nombre y órgano activo asociado sin modificar estado. |
| PATCH | `/api/especialidades/{id}/activo?activo=` | `Gestionar catálogos` | Cambia estado y retorna DTO. |
| DELETE | `/api/especialidades/{id}` | `Gestionar catálogos` | Desactiva lógicamente y responde `204 No Content`. |


## Ejemplo de cuerpo JSON

Ejemplo general para catálogos simples:

```json
{
  "nombre": "Nombre del catálogo"
}
```

Ejemplo para catálogos dependientes:

```json
{
  "nombre": "Nombre",
  "departamentoId": 1
}
```

```json
{
  "nombre": "Nombre",
  "areaId": 1
}
```

```json
{
  "nombre": "Nombre",
  "temaId": 1
}
```

Ejemplo para una especialidad asociada a un órgano de control:

```json
{
  "nombre": "Nombre de la especialidad",
  "organoControlId": 1
}
```

## Repositories

Los repositories usan consultas derivadas de Spring Data JPA para:

- listar activos ordenados por nombre;
- listar todos ordenados por nombre;
- buscar por id activo;
- validar duplicados por nombre;
- validar duplicados por nombre y relación padre;
- listar hijos por entidad padre;
- listar hijos activos por entidad padre activa.

## Services

Los services aplican el flujo de negocio:

1. validar DTO;
2. normalizar datos;
3. obtener entidades relacionadas;
4. validar duplicados;
5. validar cambios;
6. persistir entidad;
7. convertir a DTO.

## Mappers

Los mappers convierten entidades a DTOs y aplican datos normalizados a entidades. Las validaciones de permisos y las consultas de persistencia se ejecutan en los componentes de servicio, acceso y repositorio correspondientes.

## Consideraciones para frontend

- Usar endpoints activos para formularios y selects.
- Usar endpoints `/todos` para pantallas administrativas.
- En catálogos jerárquicos, cargar hijos según el padre seleccionado.
- En formularios de procesos, cargar órganos activos y especialidades activas asociadas al órgano seleccionado.
- Manejar errores de validación y negocio devueltos por backend.
- Usar `credentials: "include"` en peticiones protegidas.


## Precisiones validadas sobre catálogos

- La mayoría de endpoints `GET /{id}` de catálogos usan búsquedas activas (`findByIdAndActivoTrue`).
- `TipoDocumento` consulta por id sobre el registro identificado y puede retornar tipos activos o inactivos.
- Los catálogos jerárquicos distinguen entre consulta operativa de hijos activos por padre activo y consulta administrativa `/todos` por padre existente.
- En frontend, la administración visible de catálogos está concentrada en áreas, temas y tipos; los demás catálogos documentados se consumen como datos auxiliares en sus formularios correspondientes.
- `OrganoControl` y `Especialidad` son catálogos del módulo de procesos; su relación activa se utiliza al registrar y actualizar procesos.
