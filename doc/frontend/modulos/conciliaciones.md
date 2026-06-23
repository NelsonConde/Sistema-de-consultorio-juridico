# Frontend - Módulo de conciliaciones

## 1. Propósito del módulo

El módulo de conciliaciones permite gestionar, desde la interfaz web, las conciliaciones asociadas a consultas jurídicas. Su objetivo es ofrecer una vista operativa para consultar conciliaciones, crear una conciliación desde una consulta, revisar el detalle, asignar estudiante y conciliador, cambiar estados operativos, cargar documentos PDF y finalizar el trámite cuando existe acta.

La implementación frontend se concentra en la ruta:

```text
/conciliaciones
```

y en el componente:

```text
src/components/forms/conciliacion/ConciliacionesForm.jsx
```

La página de conciliaciones también integra el componente de reuniones de conciliación mediante pestañas, pero la gestión específica de programación y reprogramación se documenta en `doc/frontend/modulos/reuniones-conciliacion.md`.

## 2. Ruta y composición visual

La página se define en:

```text
src/app/(dashboard)/conciliaciones/page.js
```

La ruta presenta dos pestañas principales:

| Pestaña | Componente | Uso |
|---|---|---|
| Conciliaciones | `ConciliacionesForm` | Consulta, creación, asignación, documentos, estados y finalización de conciliaciones. |
| Reuniones | `ReunionesConciliacionForm` | Programación y reprogramación de reuniones de conciliación. |

Esta composición permite mantener en una sola sección funcional todo el flujo conciliatorio, pero separando la administración general de la conciliación y la gestión de reuniones.

## 3. Archivos de código relacionados

```text
src/app/(dashboard)/conciliaciones/page.js
src/components/forms/conciliacion/ConciliacionesForm.jsx
src/components/forms/conciliacion/ReunionesConciliacionForm.jsx
src/lib/config.js
src/lib/permission.js
src/lib/authz.js
```

El componente usa `API_URL_BASE` para consumir endpoints del backend y `FILE_STORAGE_API_URL_BASE` para descargar documentos almacenados por ruta.

## 4. Control de sesión y permisos

Al cargar el módulo, el componente consulta el usuario autenticado mediante:

```text
GET /api/auth/me
```

La pantalla exige que el usuario tenga acceso al módulo de conciliaciones. En código se valida la combinación de permisos:

```text
Acceder conciliaciones
Ver conciliaciones
```

Si la sesión no es válida, el frontend redirige al login. Si el usuario no cuenta con los permisos necesarios para el módulo, redirige a `/inicio`.

## 5. Permisos y acciones visibles en la pantalla

El componente utiliza permisos declarados en `src/lib/permission.js` y evaluados con helpers de `src/lib/authz.js`.

### Acceso y datos principales

| Elemento visible o carga | Condición evaluada por la interfaz |
|---|---|
| Ingreso a `/conciliaciones` | Requiere simultáneamente `Acceder conciliaciones` y `Ver conciliaciones`. |
| Listado y detalle de conciliaciones | Se cargan después de validar el acceso al módulo. |
| Consultas para crear una conciliación | Se solicitan cuando el usuario tiene `Ver consultas`. |
| Estudiantes habilitados para conciliación | Se solicitan cuando el usuario tiene `Ver estudiantes`, `Ver perfiles auxiliares`, `Gestionar conciliaciones` o `Concluir conciliaciones`. |
| Conciliadores activos | Se solicitan cuando el usuario tiene `Ver conciliadores`, `Ver perfiles auxiliares` o `Gestionar conciliaciones`. |

Los catálogos auxiliares se cargan mediante peticiones separadas y, cuando una de ellas no retorna datos disponibles para la interfaz, el componente conserva vacío el conjunto correspondiente.

### Capacidad operativa y acciones

El componente calcula una capacidad operativa para acciones sobre el detalle:

```text
Gestionar conciliaciones
o
Concluir conciliaciones cuando el usuario no es estudiante
```

Para las acciones administrativas utiliza `esRolAdministrador(usuario)`, que reconoce el perfil administrativo o los roles `Administrador`, `Administrativo` y `Director`.

| Acción visible | Condición evaluada por `ConciliacionesForm` |
|---|---|
| Crear conciliación desde consulta | Tiene `Gestionar conciliaciones` y no corresponde a estudiante ni conciliador. |
| Asignar estudiante | Tiene capacidad operativa y no corresponde a estudiante. |
| Asignar conciliador | Tiene `Gestionar conciliaciones` y cumple la condición administrativa/directiva. |
| Cambiar estado no final | Tiene capacidad operativa y no corresponde a estudiante. |
| Finalizar con acta | Tiene capacidad operativa y no corresponde a estudiante. |
| Reemplazar solicitud | Tiene `Gestionar conciliaciones` y cumple la condición administrativa/directiva. |
| Desactivar conciliación | Tiene `Gestionar conciliaciones` y cumple la condición administrativa/directiva. |

La autorización y las reglas funcionales de cada operación se aplican en el backend al recibir la petición.

## 6. Estados usados por la interfaz

La interfaz muestra el estado recibido en los datos de la conciliación y ofrece controles específicos para transición operativa y finalización.

| Uso en la interfaz | Estado o estados utilizados |
|---|---|
| Estado no final seleccionable en el panel `Cambiar estado no final` | `ESPERANDO_REUNION` |
| Estados seleccionables en el panel `Finalizar con acta` | `COMPLETO_CONCILIADO`, `COMPLETO_NO_CONCILIADO` |
| Estados que el listado o detalle puede presentar según la respuesta recibida | Incluyen `EN_ESPERA`, `REUNION_PROGRAMADA` y estados cuyo código contiene `COMPLETO`. |

La acción de finalización envía el estado final junto con el acta PDF mediante un formulario multipart, utilizando un endpoint diferente al cambio de estado no final.

## 7. Carga inicial de datos

Después de validar la sesión y el acceso al módulo, el componente carga el listado principal:

```text
GET /api/conciliaciones
```

También intenta cargar información auxiliar según los permisos evaluados en la interfaz:

| Datos auxiliares | Ruta consumida | Condición frontend |
|---|---|---|
| Consultas | `GET /api/consultas` | `Ver consultas`. |
| Estudiantes habilitados para conciliación | `GET /api/estudiantes/conciliacion` | `Ver estudiantes`, `Ver perfiles auxiliares`, `Gestionar conciliaciones` o `Concluir conciliaciones`. |
| Conciliadores activos | `GET /api/conciliadores/activos` | `Ver conciliadores`, `Ver perfiles auxiliares` o `Gestionar conciliaciones`. |

Cada endpoint conserva la autorización configurada en backend. En la vista, las listas auxiliares se utilizan para habilitar selecciones en los paneles de creación y asignación.

El listado de conciliaciones se pagina y filtra en el cliente para facilitar la consulta visual.

## 8. Endpoints consumidos por `ConciliacionesForm`

### 8.1 Consultar conciliaciones

```text
GET /api/conciliaciones
```

Carga el listado visible para el usuario autenticado. El alcance del listado corresponde al backend.

### 8.2 Consultar detalle de conciliación

```text
GET /api/conciliaciones/{id}
```

Permite cargar el detalle de una conciliación seleccionada. El detalle se usa para mostrar documentos, responsables, estado y reunión asociada cuando existe.

### 8.3 Crear conciliación desde consulta

```text
POST /api/conciliaciones/consulta/{consultaId}
Content-Type: multipart/form-data
```

El frontend envía un `FormData` con el campo:

```text
solicitud
```

El archivo de solicitud es obligatorio en la interfaz y debe ser PDF. La función `archivoEsPdf(...)` acepta el archivo cuando su tipo MIME es `application/pdf` o cuando su nombre termina en `.pdf`.

### 8.4 Asignar estudiante

```text
PATCH /api/conciliaciones/{id}/estudiante?estudianteId={estudianteId}
```

El estudiante se selecciona desde la lista de estudiantes activos habilitados para conciliación. La pantalla valida que exista selección antes de enviar la petición.

### 8.5 Asignar conciliador

```text
PATCH /api/conciliaciones/{id}/conciliador?conciliadorId={conciliadorId}
```

El conciliador se selecciona desde la lista de conciliadores activos. La pantalla valida la selección antes del envío.

### 8.6 Cambiar estado operativo

```text
PATCH /api/conciliaciones/{id}/estado?estado={codigoEstado}
```

Esta acción se usa para el estado no final seleccionable en el formulario: `ESPERANDO_REUNION`. La finalización se realiza con un endpoint independiente porque requiere acta.

### 8.7 Finalizar conciliación con acta

```text
POST /api/conciliaciones/{id}/finalizar
Content-Type: multipart/form-data
```

El frontend envía un `FormData` con:

```text
estado
acta
```

El campo `acta` debe ser un PDF y se valida mediante el tipo MIME `application/pdf` o la extensión `.pdf`. El uso de `POST` corresponde al contrato consumido por la interfaz.

### 8.8 Reemplazar solicitud PDF

```text
POST /api/conciliaciones/{id}/solicitud
Content-Type: multipart/form-data
```

Permite reemplazar el documento de solicitud de conciliación. La interfaz valida que el archivo tenga tipo MIME `application/pdf` o nombre terminado en `.pdf`.

### 8.9 Desactivar conciliación

```text
DELETE /api/conciliaciones/{id}
```

La pantalla pide confirmación antes de desactivar la conciliación. La desactivación se presenta como una operación distinta de la finalización, de modo que el usuario visualiza que no equivale a concluir el trámite con acta.

### 8.10 Descargar documentos

```text
GET /api/files/download/{path}
```

El componente construye esta petición a partir de `FILE_STORAGE_API_URL_BASE`, que se normaliza con el sufijo `/api`, y codifica cada segmento del path antes de solicitar la descarga.

## 9. Validación de archivos PDF

El componente aplica la función `archivoEsPdf(file)` antes de construir los formularios multipart. El archivo se considera PDF cuando:

```text
file.type === "application/pdf"
o
el nombre del archivo termina en ".pdf"
```

Validaciones aplicadas por la interfaz:

- la solicitud utilizada para crear una conciliación debe existir y cumplir la validación PDF;
- el acta utilizada para finalizar una conciliación debe existir y cumplir la validación PDF;
- el reemplazo de solicitud debe cumplir la validación PDF;
- los inputs de archivo se limpian después de las operaciones exitosas correspondientes.

Los documentos recibidos se envían al backend mediante `FormData` en las operaciones que requieren solicitud o acta.

## 10. Gestión de mensajes y errores

El componente usa estados internos de error y mensaje para informar el resultado de las operaciones. Además, captura errores de red, errores HTTP y mensajes del backend.

El patrón general de ejecución es:

```text
1. Limpiar mensajes anteriores.
2. Activar estado de guardado.
3. Ejecutar petición al backend.
4. Refrescar listado y detalle si aplica.
5. Mostrar mensaje de éxito o error.
6. Finalizar estado de guardado.
```

Las operaciones críticas como desactivar conciliación usan confirmación del usuario.

## 11. Relación con reuniones de conciliación

`ConciliacionesForm` carga y muestra información general de la conciliación, incluyendo datos de reunión cuando el backend los entrega en el detalle. La programación y reprogramación se gestionan en `ReunionesConciliacionForm`, que consume los endpoints de reunión.

La separación por pestañas permite que el usuario trabaje con conciliaciones y reuniones dentro de la misma ruta sin mezclar responsabilidades en un solo formulario.

## 12. Alcance de la documentación

Este documento describe la implementación frontend real del módulo de conciliaciones. La definición completa de reglas de negocio, estados, permisos backend y efectos sobre notificaciones se desarrolla en:

```text
doc/backend/conciliaciones.md
doc/api/conciliaciones.md
doc/reglas/conciliaciones.md
```


## Documentos PDF y descarga

La solicitud y el acta se almacenan en backend mediante rutas lógicas de conciliación:

- `conciliacion/{id}/solicitud.pdf`;
- `conciliacion/{id}/acta.pdf`.

La vista descarga estos documentos usando los endpoints genéricos de archivos cuando el backend expone la ruta correspondiente en el detalle de conciliación.
