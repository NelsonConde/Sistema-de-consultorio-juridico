# Servicios de API

El frontend consume el backend mediante `fetch`, helpers de respuesta, un cliente HTTP centralizado y un hook para formularios. El patrón común incluye cookie de sesión, lectura del cuerpo de respuesta, manejo de códigos HTTP y mensajes toast.

## Archivos principales

| Archivo | Responsabilidad |
|---|---|
| `src/lib/config.js` | Define `API_URL_BASE` y `FILE_STORAGE_API_URL_BASE`. |
| `src/lib/apiClient.js` | Cliente HTTP con métodos `get`, `post`, `put`, `patch`, `delete` y `request`. |
| `src/lib/api.js` | Lectura de respuestas y extracción de mensajes de error. |
| `src/hooks/useApiForm.js` | Hook para envío de formularios con toasts y manejo de sesión. |

## Cliente HTTP centralizado

`apiClient` envuelve `fetch` y aplica comportamiento uniforme:

- URL base desde `API_URL_BASE`;
- `credentials: "include"`;
- `Content-Type: application/json` cuando se usa la opción `json`;
- soporte para rutas relativas y URLs absolutas;
- métodos auxiliares para verbos HTTP.

Uso típico:

```javascript
import { apiClient } from "@/lib/apiClient";

const response = await apiClient.get("/auth/me");
const user = await response.json();
```

## Métodos disponibles

| Método | Uso |
|---|---|
| `apiClient.get(path, options)` | Petición GET autenticada. |
| `apiClient.post(path, data, options)` | Petición POST con JSON. |
| `apiClient.put(path, data, options)` | Petición PUT con JSON. |
| `apiClient.patch(path, data, options)` | Petición PATCH con JSON. |
| `apiClient.delete(path, options)` | Petición DELETE autenticada. |
| `apiClient.request(path, options)` | Petición base configurable. |

## Opción `json`

El método `request` acepta una opción `json`. Cuando se envía, el cliente serializa el cuerpo y agrega `Content-Type: application/json`.

```javascript
await apiClient.request("/areas", {
  method: "POST",
  json: { nombre: "Civil" },
});
```

## Lectura de respuesta

`src/lib/api.js` define `readResponseBody(response)`, que:

- devuelve `null` si la respuesta es `204` o no tiene cuerpo;
- intenta parsear JSON;
- devuelve texto si el cuerpo no es JSON válido.

Esto permite manejar respuestas exitosas y errores con un patrón único.

## Extracción de errores

`getApiErrorMessages(payload)` busca mensajes en:

- `detalles`;
- `details`;
- `errors`;
- `fieldErrors`;
- `validaciones`.

`getApiErrorTitle(payload, fallback)` busca el mensaje principal en:

- `mensaje`;
- `message`;
- `descripcion`;
- `error`.

`getApiErrorDescription(payload, fallback)` construye una descripción a partir de detalles o usa un fallback.

## Hook `useApiForm`

`useApiForm` centraliza el envío de formularios JSON. Recibe:

| Propiedad | Descripción |
|---|---|
| `endpoint` | URL completa del endpoint. |
| `method` | Método HTTP, por defecto `POST`. |
| `successMessage` | Mensaje toast en caso de éxito. |

Devuelve:

| Propiedad | Descripción |
|---|---|
| `submit(data)` | Función asincrónica que envía los datos. |
| `isSubmitting` | Estado de envío para deshabilitar botones. |

## Comportamiento de `useApiForm`

El hook:

1. activa `isSubmitting` antes de enviar;
2. envía JSON con cookie de sesión;
3. lee la respuesta con `readResponseBody`;
4. redirige a `/` si recibe `401`;
5. muestra toast de no autorizado si recibe `403`;
6. muestra toast de éxito si `response.ok`;
7. muestra error del backend si la respuesta falla;
8. muestra error de conexión si `fetch` lanza excepción;
9. desactiva `isSubmitting` al finalizar.

## Manejo de FormData

Para cargas de archivos, los componentes usan `fetch` directamente con `FormData`. No se debe establecer manualmente `Content-Type` en estas peticiones, porque el navegador genera el boundary del multipart.

## Patrón de consumo en módulos

Los formularios del sistema usan estos patrones:

| Tipo de operación | Patrón |
|---|---|
| Cargar catálogos o listados | `fetch` o `apiClient.get` con `credentials: include`. |
| Guardar formularios JSON simples | `useApiForm` o `fetch` con `Content-Type: application/json`. |
| Cambiar estado | `PATCH` o endpoint específico según API backend. |
| Subir archivos | `FormData` con `fetch`. |
| Descargar archivos o PDF | `fetch` y manejo de `Blob` si corresponde. |
| Error de sesión | redirección a `/`. |

## Relación con documentación API

Los nombres exactos de endpoints se documentan en `doc/api`. La documentación frontend describe cómo se consumen desde componentes y hooks; no reemplaza la especificación del backend.
