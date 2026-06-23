# Formularios y validaciones

El frontend usa `react-hook-form` en sus formularios principales y componentes reutilizables ubicados en `src/components/forms/parts`. Las validaciones de frontend sirven para mejorar experiencia de usuario; las reglas definitivas se validan en backend.

## Patrón general

Los formularios del sistema siguen un patrón común:

1. inicializar estado y `react-hook-form`;
2. cargar usuario, permisos, catálogos o datos iniciales;
3. renderizar campos con componentes reutilizables;
4. validar campos obligatorios o formatos simples;
5. construir payload compatible con DTO del backend;
6. enviar petición con `fetch`, `apiClient` o `useApiForm`;
7. mostrar toast o mensaje local;
8. actualizar listado, limpiar formulario o cerrar modal.

## Componentes reutilizables

### `FormInput`

`FormInput.jsx` renderiza un input integrado con `register`. Muestra etiqueta, asterisco para campos requeridos, estado `aria-invalid`, `aria-required` y mensaje de error.

Propiedades principales:

| Propiedad | Descripción |
|---|---|
| `name` | Nombre del campo. |
| `label` | Etiqueta visible. |
| `type` | Tipo de input, por defecto `text`. |
| `register` | Función de `react-hook-form`. |
| `errors` | Objeto de errores. |
| `rules` | Reglas de validación. |

### `FormSelect`

`FormSelect.jsx` renderiza un select compatible con `react-hook-form`. Usa lista de opciones `{ value, label }`, muestra etiqueta y error.

Se usa para catálogos, responsables, estados y opciones controladas.

### `FormCheckbox`

`FormCheckbox.jsx` renderiza checkboxes controlados por `react-hook-form`. Se usa para valores booleanos como activación de opciones o flags de formulario.

### `FormMultiSelect`

`FormMultiSelect.jsx` permite seleccionar múltiples elementos desde una lista de opciones. Es útil para relaciones múltiples o selección de permisos.

### `PersonaMultiSelect`

`PersonaMultiSelect.jsx` permite buscar y seleccionar personas en modo simple o múltiple. Se usa en consultas para persona principal, partes y contrapartes.

Características:

- búsqueda por nombres, apellidos o número de documento;
- selección única o múltiple;
- visualización de tags seleccionados;
- botón para quitar selección;
- soporte para `required` en modo simple;
- soporte para `disabled`.

### `FormFileUpload`

`FormFileUpload.jsx` permite cargar archivos con validación visual. Se usa para documentos adjuntos o archivos de soporte.

### `FormFileUpload` y `ArchivosConsultaForm`

`FormFileUpload` se usa como componente base para selección de archivos en formularios. `ArchivosConsultaForm` se usa como soporte visual para archivos asociados a consulta. Los flujos activos de carga y descarga se implementan dentro de formularios específicos como nueva consulta, consultas jurídicas, seguimientos y conciliaciones.

`ArchivoForm.jsx` es un componente auxiliar disponible en el código frontend para cargar archivos.

## Reglas reutilizables de validación

`src/lib/form-validation.js` define reglas compatibles con `register`.

| Función | Uso |
|---|---|
| `isBlank(value)` | Verifica valores vacíos o solo espacios. |
| `optionalEmailRule()` | Valida email si el campo tiene valor. |
| `requiredEmailRule()` | Exige email y formato válido. |
| `nonNegativeNumberRule()` | Exige número mayor o igual a cero. |
| `maxNumberRule(max)` | Exige valor máximo. |
| `requiredSelectRule()` | Exige selección en campos select. |

Constantes:

| Constante | Descripción |
|---|---|
| `REQUIRED_MESSAGE` | Mensaje estándar de campo obligatorio. |
| `EMAIL_PATTERN` | Expresión regular para correo electrónico. |

## Validaciones por tipo de campo

| Campo | Validación frontend típica |
|---|---|
| Correo | Formato con `requiredEmailRule` u `optionalEmailRule`. |
| Contraseña | Obligatoria; en restablecimiento exige mínimo 8 caracteres. |
| Confirmación de contraseña | Debe coincidir con nueva contraseña. |
| Select | Debe tener opción seleccionada cuando sea obligatorio. |
| Número | No negativo o máximo según regla aplicable. |
| Archivo | Validación de tipo y tamaño según componente. |
| Personas | Selección simple o múltiple según formulario. |

## Validaciones específicas observadas

### Login

`LoginForm` exige correo y contraseña. El correo usa `requiredEmailRule`. La autenticación real se valida en backend.

### Recuperación de contraseña

`RecuperarPasswordForm` exige correo válido y envía `username` al backend.

### Restablecimiento de contraseña

`RestablecerPasswordForm` exige:

- nueva contraseña obligatoria;
- mínimo 8 caracteres;
- confirmación obligatoria;
- coincidencia entre contraseña y confirmación.

### Procesos

Los formularios de procesos reflejan la regla backend vigente:

- radicado opcional mientras el proceso está pendiente;
- si se informa radicado, se valida longitud de 23 caracteres;
- para estados finales, la interfaz evita finalizar sin radicado.

### Consultas

Los formularios de consulta manejan catálogos, responsables, persona principal, partes y contrapartes. El backend conserva las reglas definitivas: coherencia área-tema-tipo, responsables válidos, duplicados de personas y bloqueo de cambios estructurales con actividad asociada.

### Seguimientos

La interfaz permite configurar fecha de entrega, días de notificación, notificación a estudiante, notificación a partes y alerta disciplinaria. El backend valida la regla definitiva de estudiante activo cuando `notificarEstudiante` es verdadero.

### Conciliación

La carga de solicitud y acta se realiza con archivos. La programación de reunión exige datos de programación y el backend valida fecha, sede, responsables y estado funcional.

## Relación con backend

El frontend no duplica todas las validaciones de negocio. Su responsabilidad es:

- evitar errores evidentes antes de enviar;
- mostrar mensajes claros;
- enviar payloads completos y consistentes;
- respetar permisos visuales;
- reflejar reglas principales en la interfaz.

El backend conserva la validación definitiva de permisos, alcance, estados, relaciones, integridad y reglas jurídicas.
