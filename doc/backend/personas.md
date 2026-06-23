# Backend - Personas

El módulo de personas administra la información de las personas relacionadas con el consultorio jurídico y sus casos.

Este módulo centraliza datos de identificación, caracterización, ubicación, situación socioeconómica, información de acudiente y datos de contacto.

## Paquetes principales

```text
business/controller/persona
business/dto/persona
business/model/persona
business/repository/persona
business/service/persona
business/service/acceso/persona
```

## Componentes principales

| Componente | Responsabilidad |
|---|---|
| `PersonaController` | Expone endpoints HTTP del módulo de personas. |
| `PersonaService` | Fachada del módulo. |
| `PersonaCommandService` | Orquesta creación, actualización, desactivación y reactivación. |
| `PersonaQueryService` | Orquesta consultas y listados. |
| `PersonaValidator` | Centraliza reglas de negocio de persona. |
| `PersonaMapper` | Convierte entidad a DTO y aplica datos del DTO sobre entidad. |
| `PersonaAccessService` | Valida permisos funcionales sobre personas. |
| `PersonaRepository` | Acceso a persistencia con Spring Data JPA. |

## Permisos usados

| Permiso | Uso |
|---|---|
| `Ver personas` | Permite consultar personas. |
| `Crear personas` | Permite crear personas. |
| `Editar personas` | Permite actualizar datos de personas. |
| `Cambiar estado personas` | Permite activar, desactivar o reactivar personas. |
| `Gestionar personas` | Permiso amplio conservado para gestión de personas. |

## Entidad principal

### `Persona`

Tabla:

```text
persona
```

La entidad agrupa información en bloques funcionales:

| Bloque | Campos principales |
|---|---|
| Identificación | tipo de persona, tipo de documento, número de documento, fecha y ciudad de expedición. |
| Datos personales | nombres, apellidos, nombre identitario, pronombre, sexo, género, orientación sexual, fecha de nacimiento. |
| Contacto | teléfono, correo. |
| Caracterización | nacionalidad, estado civil, escolaridad, grupo étnico, condición actual, lectura/escritura, discapacidad, caracterización PCD, ajuste PCD. |
| Vivienda | municipio, barrio, dirección, comuna, localidad, estrato, tipo de vivienda, zona, tenencia, personas a cargo, servicios públicos. |
| Aspectos económicos | ocupación, empresa, salario, cargo, dirección y teléfono de empresa. |
| Acudiente | nombre, relación, teléfono, correo y dirección del acudiente. |
| Servicio | cómo se enteró del servicio y relación con la universidad. |
| Control | activo. |

## DTO principal

### `PersonaDTO`

`PersonaDTO` representa el contrato de entrada y salida del módulo de personas.

Incluye validaciones con Jakarta Validation para campos obligatorios, tamaños máximos, formatos de correo y valores mínimos.

Bloques del DTO:

- información básica;
- contacto;
- caracterización;
- vivienda;
- aspectos económicos;
- datos del acudiente;
- información del servicio;
- estado activo.

## Endpoints de persona

Base path:

```text
/api/personas
```

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/personas` | `Ver personas` o `Gestionar personas` | Lista personas. |
| GET | `/api/personas/{id}` | `Ver personas` o `Gestionar personas` | Consulta persona por id. |
| GET | `/api/personas/activos` | `Ver personas` o `Gestionar personas` | Lista personas activas. |
| POST | `/api/personas` | `Crear personas` o `Gestionar personas` | Crea persona. |
| PUT | `/api/personas/{id}` | `Editar personas` o `Gestionar personas` | Actualiza persona. |
| PATCH | `/api/personas/{id}/desactivar` | `Cambiar estado personas` o `Gestionar personas` | Desactiva persona. |
| PATCH | `/api/personas/{id}/reactivar` | `Cambiar estado personas` o `Gestionar personas` | Reactiva persona. |

## Reglas de negocio

### Creación

Al crear una persona:

- el DTO es obligatorio;
- no se permite enviar `id`;
- el número de documento es obligatorio;
- el número de documento se normaliza;
- el número de documento debe ser único;
- las relaciones obligatorias deben existir y estar activas;
- el barrio seleccionado debe pertenecer al municipio seleccionado;
- la persona se guarda con los datos normalizados.

### Actualización

Al actualizar:

- el DTO es obligatorio;
- si el DTO trae `id`, debe coincidir con el `id` de la ruta;
- se busca la persona existente;
- el documento se normaliza;
- si el documento cambia, debe seguir siendo único;
- se cargan relaciones activas;
- se valida que el barrio pertenezca al municipio;
- se aplican los datos sobre la entidad existente sin alterar campos de control no relacionados.

### Documento único

`PersonaRepository` valida unicidad por `numeroDocumento`.

Reglas:

- en creación, no puede existir una persona con el mismo documento;
- en actualización, el documento puede conservarse en la misma persona, pero no duplicarse en otra.

### Barrio y municipio

El backend valida que el barrio pertenezca al municipio seleccionado.

Esta regla protege la consistencia cuando el frontend no filtra correctamente barrios por municipio.

### Contacto

La persona debe informar al menos teléfono o correo.

El sistema usa utilidades de normalización para tratar textos no informados como datos vacíos cuando corresponda.

### Acudiente

Si la persona es menor de edad según fecha de nacimiento, el backend exige:

- nombre del acudiente;
- relación del acudiente;
- al menos un medio de contacto del acudiente: teléfono o correo.

El correo principal y el correo del acudiente deben enviarse vacíos/nulos o con formato válido. No se debe enviar un texto informativo como `No informa` en campos de correo, porque la validación de formato de correo aplica cuando el campo viene informado.

### Estado activo

La persona puede desactivarse y reactivarse mediante endpoints específicos. En el código actual, estas operaciones asignan directamente `activo=false` o `activo=true` sobre la persona encontrada por id; no validan que el estado previo sea distinto.

La desactivación mantiene el registro para conservar historial y relaciones con otros módulos.

## Relaciones cargadas en creación y actualización

`PersonaCommandService` carga relaciones activas desde repositories. La actualización de persona espera un DTO completo y no modifica el campo `activo`:

| Relación | Repository |
|---|---|
| TipoPersona | `TipoPersonaRepository` |
| Nacionalidad | `NacionalidadRepository` |
| Condicion | `CondicionRepository` |
| Municipio | `MunicipioRepository` |
| Barrio | `BarrioRepository` |
| Ocupacion | `OcupacionRepository` |
| Empresa | `EmpresaRepository` |

## Listados

`PersonaQueryService` expone dos tipos de listados:

| Método de service | Uso |
|---|---|
| `listar()` | Lista personas para usuarios con permiso de consulta. |
| `listarActivos()` | Lista personas activas ordenadas por nombres y apellidos. |

## Catálogos propios de persona

Además de `Persona`, el paquete `persona` administra catálogos directamente relacionados con caracterización o datos socioeconómicos.

### Condiciones

Entidad:

```text
Condicion
```

Base path:

```text
/api/condiciones
```

Uso:

- registrar condiciones actuales;
- consultar activas;
- administrar estado activo/inactivo.

### Empresas

Entidad:

```text
Empresa
```

Base path:

```text
/api/empresas
```

Uso:

- registrar empresas relacionadas con datos económicos;
- consultar activas;
- administrar estado activo/inactivo.

### Ocupaciones

Entidad:

```text
Ocupacion
```

Base path:

```text
/api/ocupaciones
```

Uso:

- registrar ocupaciones;
- consultar activas;
- administrar estado activo/inactivo.

### Tipos de persona

Entidad:

```text
TipoPersona
```

Base path:

```text
/api/tipos-persona
```

Uso:

- clasificar personas;
- consultar tipos activos;
- administrar estado activo/inactivo.

## Patrón de catálogos propios de persona

Los catálogos `Condicion`, `Empresa`, `Ocupacion` y `TipoPersona` siguen un patrón común:

| Operación | Descripción |
|---|---|
| Listar activos | Devuelve registros con `activo=true`. |
| Listar todos | Devuelve registros activos e inactivos para administración. |
| Obtener por id | Consulta un registro por identificador. |
| Crear | Valida DTO, normaliza nombre, valida duplicado y guarda activo. |
| Actualizar | Valida id, normaliza nombre, valida duplicado y cambio efectivo. |
| Cambiar estado | Cambia el campo `activo`. |
| Eliminar | Desactiva lógicamente el registro. |

## Endpoints de catálogos propios de persona

### Condiciones

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/condiciones` | `Ver catálogos` o `Gestionar catálogos` | Lista condiciones activas. |
| GET | `/api/condiciones/todos` | `Gestionar catálogos` | Lista todas las condiciones. |
| GET | `/api/condiciones/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta condición por id. |
| POST | `/api/condiciones` | `Gestionar catálogos` | Crea condición. |
| PUT | `/api/condiciones/{id}` | `Gestionar catálogos` | Actualiza condición. |
| PATCH | `/api/condiciones/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/condiciones/{id}` | `Gestionar catálogos` | Desactiva condición. |

### Empresas

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/empresas` | `Ver catálogos` o `Gestionar catálogos` | Lista empresas activas. |
| GET | `/api/empresas/todos` | `Gestionar catálogos` | Lista todas las empresas. |
| GET | `/api/empresas/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta empresa por id. |
| POST | `/api/empresas` | `Gestionar catálogos` | Crea empresa. |
| PUT | `/api/empresas/{id}` | `Gestionar catálogos` | Actualiza empresa. |
| PATCH | `/api/empresas/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/empresas/{id}` | `Gestionar catálogos` | Desactiva empresa. |

### Ocupaciones

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/ocupaciones` | `Ver catálogos` o `Gestionar catálogos` | Lista ocupaciones activas. |
| GET | `/api/ocupaciones/todos` | `Gestionar catálogos` | Lista todas las ocupaciones. |
| GET | `/api/ocupaciones/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta ocupación por id. |
| POST | `/api/ocupaciones` | `Gestionar catálogos` | Crea ocupación. |
| PUT | `/api/ocupaciones/{id}` | `Gestionar catálogos` | Actualiza ocupación. |
| PATCH | `/api/ocupaciones/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/ocupaciones/{id}` | `Gestionar catálogos` | Desactiva ocupación. |

### Tipos de persona

| Método | Ruta | Permiso | Uso |
|---|---|---|---|
| GET | `/api/tipos-persona` | `Ver catálogos` o `Gestionar catálogos` | Lista tipos de persona activos. |
| GET | `/api/tipos-persona/todos` | `Gestionar catálogos` | Lista todos los tipos de persona. |
| GET | `/api/tipos-persona/{id}` | `Ver catálogos` o `Gestionar catálogos` | Consulta tipo de persona por id. |
| POST | `/api/tipos-persona` | `Gestionar catálogos` | Crea tipo de persona. |
| PUT | `/api/tipos-persona/{id}` | `Gestionar catálogos` | Actualiza tipo de persona. |
| PATCH | `/api/tipos-persona/{id}/activo` | `Gestionar catálogos` | Cambia estado activo. |
| DELETE | `/api/tipos-persona/{id}` | `Gestionar catálogos` | Desactiva tipo de persona. |

## Reglas de duplicado en catálogos propios

| Catálogo | Regla |
|---|---|
| Condición | Nombre único, ignorando mayúsculas/minúsculas. |
| Empresa | Nombre único, ignorando mayúsculas/minúsculas. |
| Ocupación | Nombre único, ignorando mayúsculas/minúsculas. |
| Tipo de persona | Nombre único, ignorando mayúsculas/minúsculas. |

## Consideraciones para frontend

- Usar `/activos` o listados activos para formularios y combos.
- Usar endpoints administrativos `/todos` para gestión de catálogos.
- En persona, enviar `municipioId` y `barrioId` coherentes.
- En menores de edad, enviar información de acudiente.
- Enviar al menos teléfono o correo.
- Usar `credentials: "include"` en peticiones protegidas.
- Manejar errores de validación por campo y errores de negocio.


## Precisiones operativas validadas

- `GET /api/personas` lista el conjunto general de personas registradas.
- `GET /api/personas/activos` lista únicamente personas activas ordenadas por nombres y apellidos.
- `PATCH /api/personas/{id}/desactivar` y `PATCH /api/personas/{id}/reactivar` responden `204 No Content`.
- `PUT /api/personas/{id}` actualiza datos de persona, pero no cambia el estado lógico `activo`.
- La desactivación de persona no bloquea por relaciones existentes con consultas, partes o contrapartes; el código conserva el registro y solo cambia el estado lógico.
