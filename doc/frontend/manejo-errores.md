# Manejo de errores

El frontend interpreta respuestas del backend mediante helpers de `src/lib/api.js`, estados locales de componentes y toasts de Sonner. El formato consumido depende del contrato del endpoint: respuestas JSON de los handlers, objetos propios de una operación, texto plano o respuesta sin cuerpo.

## Formatos recibidos

### Respuesta estructurada de handlers

Los errores producidos por `GlobalExceptionHandler` y `SecurityExceptionHandler` incluyen campos como:

```json
{
  "fecha": "fecha-hora-del-error",
  "estado": 400,
  "error": "Tipo de error",
  "mensaje": "Mensaje descriptivo",
  "ruta": "/api/recurso"
}
```

Las respuestas de validación pueden incluir:

```json
{
  "detalles": {
    "campo": "Mensaje de validación"
  }
}
```

### Respuestas específicas de operaciones

El frontend también consume respuestas definidas por endpoints concretos:

| Operación | Formato consumido |
|---|---|
| Carga individual de archivos | Objeto JSON que puede contener `error`. |
| Carga múltiple de archivos | Lista JSON con resultado por archivo. |
| Descarga o listado de archivos no disponible | Respuesta sin cuerpo. |
| Importación de estudiantes con entrada inválida o error procesado | Texto plano. |
| Importación de estudiantes procesada | Objeto con contadores y errores por fila. |

## Helpers de `src/lib/api.js`

| Helper | Comportamiento implementado |
|---|---|
| `readResponseBody(response)` | Retorna `null` para respuesta `204` o cuerpo vacío; intenta parsear JSON y conserva texto si no es JSON válido. |
| `getApiErrorMessages(payload)` | Extrae mensajes desde `detalles`, `details`, `errors`, `fieldErrors` o `validaciones`; también acepta texto. |
| `getApiErrorTitle(payload, fallback)` | Lee `mensaje`, `message`, `descripcion` o `error`; si el payload es texto, utiliza ese texto. |
| `getApiErrorDescription(payload, fallback)` | Une mensajes de detalle o utiliza el título o fallback disponible. |

## Manejo centralizado en `useApiForm`

`useApiForm` envía formularios JSON con `credentials: "include"` y lee el cuerpo mediante `readResponseBody(...)`.

| Resultado HTTP | Tratamiento implementado |
|---|---|
| `401 Unauthorized` | Muestra toast `Sesión expirada`, informa que debe iniciar sesión nuevamente y redirige a `/`. |
| `403 Forbidden` | Muestra toast `No autorizado` sin redirección. |
| `response.ok` | Muestra el toast de éxito configurado y retorna los datos procesados. |
| Otra respuesta no exitosa | Usa `getApiErrorTitle(...)` y `getApiErrorDescription(...)` sobre el payload recibido. |
| Excepción de red | Muestra toast `Error de conexión`. |

## Manejo de importación de estudiantes

`ImportarEstudiantesForm.jsx` ejecuta directamente:

```http
POST /api/estudiantes/importar
```

con un `FormData` que envía el parámetro:

```text
archivo
```

y transporta la sesión mediante:

```javascript
credentials: "include"
```

### Respuestas procesadas en el componente

| Respuesta | Tratamiento visible |
|---|---|
| `200 OK` | Lee JSON, conserva el resultado y presenta contadores de filas exitosas, fallidas y mensajes de error. |
| `400 Bad Request` | Lee texto y lo presenta como error de formato. |
| `403 Forbidden` | Muestra toast indicando que el usuario no tiene permiso para importar estudiantes. |
| Otra respuesta no exitosa | Lee texto; si puede parsearlo como JSON utiliza `mensaje`, y en otro caso presenta el mensaje general de error interno. |
| Excepción de red | Muestra toast de error de conexión. |

Cuando el resultado `200 OK` contiene filas fallidas, el componente conserva el detalle por fila y presenta el balance de la importación.

## Manejo en autenticación

Algunos formularios de autenticación usan estado local para su retroalimentación:

| Componente | Comportamiento implementado |
|---|---|
| `LoginForm` | Muestra en el formulario el mensaje obtenido de una respuesta no exitosa. |
| `RecuperarPasswordForm` | Muestra localmente errores; después de una solicitud exitosa programa la navegación a `/`. |
| `RestablecerPasswordForm` | Muestra el mensaje de éxito o error y, tras éxito, programa la navegación a `/`. |

## Toasts y continuidad de interfaz

El frontend usa Sonner para notificaciones visuales. Los flujos revisados mantienen el formulario o la vista actual cuando una operación falla, salvo el tratamiento de sesión expirada (`401`) que redirige a la página de acceso.

| Caso implementado | Presentación |
|---|---|
| Operación exitosa mediante `useApiForm` | `toast.success(...)`. |
| Error no exitoso mediante `useApiForm` | `toast.error(título, { description })`. |
| Sesión expirada | Toast y redirección a `/`. |
| Operación sin permiso | Toast de no autorizado. |
| Error de red | Toast de conexión. |

## Relación con backend

El frontend consume los contratos de respuesta de cada endpoint. Las reglas de validación, autorización y procesamiento se ejecutan en backend; la interfaz presenta los resultados mediante mensajes locales, contadores o toasts según el flujo.
