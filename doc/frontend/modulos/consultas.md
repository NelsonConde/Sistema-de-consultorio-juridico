# Frontend - Módulo de consultas jurídicas

## 1. Propósito del módulo

El módulo de consultas jurídicas permite registrar, consultar, editar, asignar responsables, cambiar estado, archivar y visualizar consultas jurídicas desde la interfaz web del sistema. En el frontend actual se implementa mediante dos formularios principales:

- `NuevaConsultaForm.jsx`, usado para crear una consulta nueva.
- `ConsultasJuridicasForm.jsx`, usado para listar, filtrar, abrir, editar, cambiar estado, archivar y consultar archivos de una consulta.

Las rutas del dashboard asociadas son:

| Ruta | Componente de página | Formulario principal |
|---|---|---|
| `/nuevaconsulta` | `src/app/(dashboard)/nuevaconsulta/page.js` | `NuevaConsultaForm` |
| `/consultasjuridicas` | `src/app/(dashboard)/consultasjuridicas/page.js` | `ConsultasJuridicasForm` |

El módulo consume directamente endpoints del backend mediante `API_URL_BASE` y maneja archivos con `FILE_STORAGE_API_URL_BASE` cuando se cargan o descargan soportes asociados a la consulta.

## 2. Archivos fuente validados

La documentación de este módulo se basa en los siguientes archivos del código fuente:

```text
frontend/src/app/(dashboard)/nuevaconsulta/page.js
frontend/src/app/(dashboard)/consultasjuridicas/page.js
frontend/src/components/forms/consulta/NuevaConsultaForm.jsx
frontend/src/components/forms/consulta/ConsultasJuridicasForm.jsx
frontend/src/components/forms/parts/ArchivoForm.jsx
frontend/src/components/forms/parts/ArchivosConsultaForm.jsx
frontend/src/components/forms/parts/PersonaMultiSelect.jsx
frontend/src/components/ui/ConfirmActionDialog.jsx
frontend/src/components/ui/Pagination.jsx
frontend/src/lib/config.js
frontend/src/lib/api.js
frontend/src/lib/authz.js
frontend/src/lib/permission.js
frontend/src/lib/list-utils.js
frontend/src/lib/form-validation.js
```

## 3. Vista de nueva consulta

La página `/nuevaconsulta` presenta el encabezado "Nueva Consulta" y renderiza el formulario `NuevaConsultaForm`. Este formulario concentra el registro inicial de una consulta jurídica.

### 3.1 Carga de sesión y permisos

Antes de permitir la operación, el formulario consulta:

```text
GET /api/auth/me
```

A partir del usuario autenticado verifica permisos de acceso y operación. La vista usa permisos definidos en `lib/permission.js`, especialmente:

| Permiso | Uso en frontend |
|---|---|
| `Acceder nueva consulta` | Permite entrar a la ruta de creación. |
| `Crear consultas` | Permite enviar el formulario de creación. |
| `Asignar responsables consulta` | Permite seleccionar asesor, estudiante y monitor desde el formulario. |

Cuando el usuario no cuenta con acceso suficiente, la interfaz muestra un mensaje con `toast.error` y redirecciona a una ruta segura del dashboard.

### 3.2 Catálogos consumidos

El formulario de creación carga listas activas desde backend. Los catálogos se consumen según el contexto y los permisos del usuario:

| Recurso | Endpoint usado |
|---|---|
| Personas activas | `GET /api/personas/activos` |
| Sedes | `GET /api/sedes` |
| Áreas | `GET /api/areas` |
| Temas por área | `GET /api/temas/area/{areaId}` |
| Tipos por tema | `GET /api/tipos/tema/{temaId}` |
| Asesores activos | `GET /api/asesores/activos` |
| Monitores activos | `GET /api/monitores/activos` |
| Estudiantes activos | `GET /api/estudiantes/activos` |

Los responsables solo se cargan y muestran cuando el usuario tiene permiso para asignarlos.

### 3.3 Campos de la consulta

El estado inicial del formulario incluye campos narrativos, catálogos, persona principal, partes, contrapartes y responsables internos. Entre los campos usados por el frontend están:

```text
fecha
descripcion
hechos
pretensiones
conceptoJuridico
tramite
observaciones
tipoViolencia
resultado
personaId
sedeId
areaId
temaId
tipoId
asesorId
monitorId
estudianteId
partesIds
contrapartesIds
```

El frontend normaliza valores de texto vacíos a `null`, convierte identificadores a número cuando corresponde y envía listas de identificadores para partes y contrapartes.

### 3.4 Selección de personas y responsables

El formulario usa modales de selección para facilitar la búsqueda de:

- persona principal;
- partes adicionales;
- contrapartes;
- asesor;
- monitor;
- estudiante.

La interfaz evita seleccionar una misma persona simultáneamente como persona principal, parte adicional o contraparte. Esta validación visual acompaña las reglas del backend y mejora la consistencia del registro.

### 3.5 Relación área, tema y tipo

El formulario aplica dependencias entre catálogos:

1. Al seleccionar área, carga temas de esa área.
2. Al seleccionar tema, carga tipos asociados a ese tema.
3. Si se cambia el área, se reinician tema y tipo.
4. Si se cambia el tema, se reinicia el tipo.

Esta interacción refleja la jerarquía de catálogos usada por backend.

### 3.6 Envío de nueva consulta

La creación se realiza mediante:

```text
POST /api/consultas
```

El formulario envía JSON con los datos de la consulta. Después de crear la consulta, si el usuario adjuntó archivos, el frontend sube los documentos asociados con:

```text
POST /api/files/upload-multiple
```

Cuando la consulta se crea correctamente, la interfaz muestra confirmación y redirige a `/consultasjuridicas` con un parámetro de refresco.

### 3.7 Manejo de archivos en creación

`NuevaConsultaForm` permite seleccionar archivos de soporte. La carga se realiza después de que backend devuelve la consulta creada, porque la asociación documental requiere el identificador de la consulta.

El frontend diferencia dos situaciones:

- si la consulta se crea y los archivos suben correctamente, muestra confirmación completa;
- si la consulta se crea pero falla la subida de archivos, muestra advertencia sin revertir la consulta creada.

Este comportamiento conserva la operación principal y comunica al usuario el estado de los soportes.

## 4. Vista de consultas jurídicas

La página `/consultasjuridicas` renderiza `ConsultasJuridicasForm`. Esta vista permite administrar las consultas según los permisos del usuario autenticado.

### 4.1 Carga de usuario y acceso

Al iniciar, la vista consulta:

```text
GET /api/auth/me
```

Con la información del usuario determina si puede ingresar, ver, editar, cambiar estado, archivar o asignar responsables.

Permisos usados:

| Permiso | Uso |
|---|---|
| `Acceder consultas jurídicas` | Entrada a la ruta. |
| `Ver consultas` | Carga del listado. |
| `Editar consultas` | Apertura del modal de edición. |
| `Cambiar estado consultas` | Apertura y envío de cambio de estado. |
| `Archivar consultas` | Acción de archivo lógico. |
| `Asignar responsables consulta` | Edición de asesor, estudiante y monitor. |

### 4.2 Listado y búsqueda

El listado se carga desde:

```text
GET /api/consultas
GET /api/consultas?search={texto}
```

El componente normaliza las respuestas para soportar diferentes envoltorios de datos, como `content`, `data`, `items`, `rows`, `consultas`, `resultado`, `result` o `payload`. Después ordena las consultas por `id` ascendente y aplica paginación en frontend mediante `Pagination` y utilidades de `list-utils.js`.

La vista permite buscar por texto y filtrar visualmente por estado y área.

### 4.3 Estados disponibles

La vista reconoce los siguientes estados de consulta:

```text
ACTIVO
EN_PROCESO
PENDIENTE
URGENTE
CERRADO
ARCHIVADO
```

Estos estados se muestran en la tabla y se usan para habilitar o restringir acciones de interfaz.

### 4.4 Apertura y edición de una consulta

Para abrir una consulta en edición, el frontend consulta:

```text
GET /api/consultas/{id}
```

También carga temas y tipos relacionados con el área y tema de la consulta. La actualización se realiza mediante:

```text
PUT /api/consultas/{id}
```

La edición no cambia el estado funcional de la consulta; el cambio de estado se gestiona desde una acción separada.

### 4.5 Responsables internos

Cuando el usuario tiene permiso de asignación, la vista permite seleccionar o modificar:

- asesor;
- monitor;
- estudiante.

La interfaz filtra responsables para facilitar coherencia operativa:

- asesores disponibles según área;
- estudiantes disponibles según asesor o área;
- monitores desde la lista de monitores activos.

Cuando el usuario no tiene permiso para asignar responsables, la interfaz conserva la información y muestra un mensaje indicando que esos responsables no pueden cambiarse con sus permisos actuales.

### 4.6 Cambio de estado

El cambio de estado se realiza mediante:

```text
PATCH /api/consultas/{id}/estado?estado={estado}
```

El frontend bloquea visualmente cambios inválidos y evita usar el cambio de estado para archivar. Para archivar existe acción específica.

Antes de cerrar una consulta, el frontend valida que exista `resultado` o conclusión final guardada. Si el resultado fue escrito en el modal pero aún no se guardó, se pide guardar primero la consulta antes de intentar cerrar.

Esta validación de interfaz acompaña la validación del backend.

### 4.7 Archivo lógico

El archivo de una consulta se ejecuta mediante:

```text
PATCH /api/consultas/{id}/archivar
```

La acción se presenta mediante confirmación antes de enviarse. Después de archivar, el listado se refresca.

### 4.8 Consulta de archivos

Desde `ConsultasJuridicasForm` se pueden consultar archivos asociados a una consulta usando:

```text
GET /api/files/list/{consultaId}
```

Y descargar archivos mediante:

```text
GET /api/files/download/{consultaId}/{fileName}
```

La base de URL para archivos se toma de `FILE_STORAGE_API_URL_BASE`.

## 5. Validaciones de interfaz

El frontend valida condiciones básicas antes de enviar datos:

- selección de persona principal;
- selección de sede, área y tema;
- coherencia de tema y tipo según catálogos cargados;
- existencia de resultado antes de cerrar;
- permisos de edición, estado, archivo y asignación;
- datos mínimos del formulario;
- exclusión visual de personas repetidas en parte principal, partes adicionales y contrapartes.

Las validaciones del frontend no sustituyen las reglas del backend; funcionan como apoyo para mejorar experiencia y reducir errores de captura.

## 6. Relación con backend

El módulo de consultas del frontend está alineado con las reglas del backend:

| Regla backend | Reflejo en frontend |
|---|---|
| Consulta nueva inicia en `PENDIENTE`. | El formulario de creación no envía un estado operativo arbitrario. |
| Cambio de estado usa endpoint específico. | La edición general no modifica estado. |
| Cierre exige resultado. | El frontend pide guardar resultado antes de cerrar. |
| Archivo es acción independiente. | La vista usa botón y endpoint de archivo. |
| Responsables requieren permiso. | La UI muestra/oculta controles según permisos. |
| Consultas cerradas o archivadas limitan operación. | La vista reduce acciones y muestra avisos de solo visualización cuando aplica. |

## 7. Manejo de errores

Los errores del backend se procesan mediante utilidades de `lib/api.js`, especialmente `getApiErrorMessages`, `getApiErrorTitle`, `getApiErrorDescription` y lectura segura del cuerpo de respuesta.

La interfaz usa `toast.error`, `toast.success` y `toast.warning` para comunicar:

- falta de permisos;
- errores de carga;
- errores de validación;
- éxito al crear o actualizar;
- éxito al cambiar estado;
- éxito al archivar;
- advertencias de carga de archivos.

## 8. Componentes relacionados

| Componente | Función |
|---|---|
| `NuevaConsultaForm` | Registro inicial de consulta. |
| `ConsultasJuridicasForm` | Listado, edición, archivo y cambio de estado. |
| `ArchivosConsultaForm` | Gestión visual de soportes asociados. |
| `PersonaMultiSelect` | Selección múltiple de personas. |
| `ConfirmActionDialog` | Confirmación de acciones sensibles. |
| `Pagination` | Paginación de listados. |

## 8.1 Archivos asociados a consulta

El flujo activo de archivos de consulta usa componentes y servicios de soporte para cargar, listar y descargar documentos asociados al registro de consulta. La carga se realiza contra `POST /api/files/upload-multiple` y la consulta/listado descarga archivos desde rutas relativas administradas por el módulo de almacenamiento.

Los componentes documentados como operativos en este flujo son `FormFileUpload` y `ArchivosConsultaForm`. `ArchivoForm.jsx` no se presenta como flujo funcional independiente porque no está conectado a una ruta principal del sistema.

## 9. Consideraciones de mantenimiento

Al modificar este módulo debe verificarse:

1. Que las constantes de permisos sigan coincidiendo con backend.
2. Que los endpoints usados existan en controllers actuales.
3. Que el cierre de consulta conserve la validación de resultado.
4. Que la edición general no cambie el estado funcional.
5. Que el archivo siga usando endpoint específico.
6. Que las dependencias área-tema-tipo se mantengan sincronizadas.
7. Que el manejo de archivos use `FILE_STORAGE_API_URL_BASE`.
8. Que las restricciones de responsables se mantengan coherentes con el backend.
