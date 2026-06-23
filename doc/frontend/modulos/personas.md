# Frontend - Módulo de personas

## 1. Propósito del módulo

El módulo de personas permite registrar, consultar, editar y desactivar personas naturales usadas por el sistema como usuarios de consulta, partes, contrapartes o sujetos relacionados con una atención jurídica.

El frontend lo implementa con dos rutas principales:

| Ruta | Componente de página | Formulario principal |
|---|---|---|
| `/recepcion` | `src/app/(dashboard)/recepcion/page.js` | `PersonaForm` |
| `/personas` | `src/app/(dashboard)/personas/page.js` | `PersonasForm` |

## 2. Archivos fuente validados

```text
frontend/src/app/(dashboard)/recepcion/page.js
frontend/src/app/(dashboard)/personas/page.js
frontend/src/components/forms/persona/PersonaForm.jsx
frontend/src/components/forms/persona/PersonasForm.jsx
frontend/src/lib/config.js
frontend/src/lib/api.js
frontend/src/lib/authz.js
frontend/src/lib/permission.js
frontend/src/lib/form-validation.js
frontend/src/components/ui/ConfirmActionDialog.jsx
frontend/src/components/ui/Pagination.jsx
```

## 3. Registro desde recepción

La ruta `/recepcion` presenta el formulario `PersonaForm`. Esta vista está orientada al registro de una nueva persona en el sistema.

### 3.1 Validación de acceso

El formulario consulta:

```text
GET /api/auth/me
```

Y valida permisos relacionados con recepción y creación de personas:

| Permiso | Uso |
|---|---|
| `Acceder recepción` | Permite ingresar a la vista de registro. |
| `Crear personas` | Permite enviar el formulario de creación. |

Si el usuario no tiene permiso, la interfaz muestra error y redirige.

### 3.2 Datos capturados

`PersonaForm` maneja una estructura amplia de caracterización. Entre los campos del formulario están:

```text
tipoPersonaId
tipoDocumento
numeroDocumento
fechaExpedicion
ciudadExpedicion
nombres
apellidos
nombreIdentitario
pronombre
sexo
genero
orientacionSexual
fechaNacimiento
telefono
correo
nacionalidadId
estadoCivil
escolaridad
grupoEtnico
condicionActualId
sabeLeerEscribir
discapacidad
caracterizacionPcd
necesitaAjustePcd
departamentoId
municipioId
barrioId
direccion
comuna
localidad
estrato
tipoVivienda
zona
tenencia
numeroPersonasACargo
ingresosAdicionales
energiaElectrica
acueducto
alcantarillado
ocupacionId
empresaId
salario
cargo
direccionEmpresa
telefonoEmpresa
nombreCompletoAcudiente
relacionAcudiente
telefonoAcudiente
correoAcudiente
direccionAcudiente
comoSeEntero
relacionConUniversidad
```

Esta estructura permite registrar información personal, sociodemográfica, ubicación, vivienda, ocupación y acudiente.

### 3.3 Catálogos usados

El formulario carga catálogos desde backend mediante rutas bajo `API_URL_BASE`, incluyendo datos como:

- tipos de persona;
- nacionalidades;
- condiciones actuales;
- departamentos;
- municipios;
- barrios;
- ocupaciones;
- empresas.

También incluye opciones locales para algunos campos de selección, como pronombre, sexo, género, orientación sexual y estado civil.

### 3.4 Validaciones de formulario

El frontend aplica validaciones antes de enviar:

- campos obligatorios;
- correo con patrón de email;
- valores numéricos no negativos;
- información de contacto cuando corresponde;
- consistencia de campos dependientes.

La validación de correo usa `EMAIL_PATTERN` definido en `lib/form-validation.js`.

### 3.5 Envío de registro

El registro de persona se realiza mediante:

```text
POST /api/personas
```

El formulario usa `useApiForm` para manejar envío, estado de carga, mensajes y errores.

## 4. Administración de personas

La ruta `/personas` renderiza `PersonasForm`, que permite listar personas activas, buscar, editar y desactivar. El módulo de eliminación/reactivación ofrece la acción de reactivar personas.

### 4.1 Permisos

La vista valida:

| Permiso | Uso |
|---|---|
| `Acceder personas` | Permite entrar a la página de personas. |
| `Ver personas` | Permite cargar el listado. |
| `Editar personas` | Permite abrir edición. |
| `Cambiar estado personas` o `Gestionar personas` | Permite desactivar. |

### 4.2 Listado

El componente carga personas activas con:

```text
GET /api/personas/activos
```

El listado permite búsqueda y paginación. También presenta acciones según permisos.

### 4.3 Edición

La edición se realiza mediante:

```text
PUT /api/personas/{id}
```

El formulario de edición reutiliza la estructura de datos de persona y valida reglas antes de enviar.

### 4.4 Desactivación

La desactivación de persona se realiza con:

```text
PATCH /api/personas/{id}/desactivar
```

La respuesta del backend es `204 No Content`; por tanto, la interfaz actualiza la vista recargando o retirando el registro de la lista activa.

La acción usa `ConfirmActionDialog` para solicitar confirmación antes de enviarla.

## 5. Datos sensibles y presentación

El frontend administra información personal y de caracterización. Por esa razón, el módulo:

- consulta datos usando sesión autenticada;
- muestra acciones solo según permisos;
- usa rutas protegidas por el layout del dashboard;
- evita documentar o fijar datos reales en código;
- depende del backend para reglas de persistencia y alcance.

## 6. Manejo de errores

El módulo usa utilidades de `lib/api.js` para presentar errores de backend. Los mensajes se muestran mediante `toast.error`, `toast.success` o confirmaciones visuales.

Se manejan situaciones como:

- sesión inválida;
- falta de permisos;
- error cargando catálogos;
- error de validación;
- error de conexión;
- confirmación de desactivación.

## 7. Relación con otros módulos

Las personas registradas se usan en:

- consultas jurídicas como persona principal;
- partes adicionales;
- contrapartes;
- flujos de recepción;
- caracterización para estadísticas y reportes derivados.

## 8. Componentes relacionados

| Componente | Función |
|---|---|
| `PersonaForm` | Registro de persona desde recepción. |
| `PersonasForm` | Listado, edición y desactivación. |
| `ConfirmActionDialog` | Confirmación de desactivación. |
| `Pagination` | Paginación del listado. |

## 9. Consideraciones de mantenimiento

Al modificar el módulo debe verificarse:

1. Que los nombres de campos sigan coincidiendo con el DTO de backend.
2. Que los catálogos consumidos existan y estén activos.
3. Que las rutas `/recepcion` y `/personas` mantengan permisos diferenciados.
4. Que la desactivación use `PATCH` y no eliminación física.
5. Que las validaciones visuales no sustituyan las validaciones del backend.
6. Que los datos sensibles no se expongan en documentación ni configuración.


## Precisiones de frontend validadas

- `/recepcion` usa `PersonaForm` para crear nuevas personas mediante `POST /api/personas`.
- `/personas` usa `PersonasForm` para listar personas activas, editar por `PUT /api/personas/{id}` y desactivar por `PATCH /api/personas/{id}/desactivar`.
- La reactivación de personas se administra desde el módulo de eliminación/reactivación.
- El formulario usa catálogos activos y carga municipios/barrios dependientes para respetar la relación Departamento → Municipio → Barrio.
