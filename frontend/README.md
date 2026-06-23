# Frontend - Sistema de gestión de casos jurídicos

Aplicación frontend del sistema de gestión de casos jurídicos del consultorio jurídico.

El frontend está desarrollado con Next.js y consume el backend principal mediante endpoints REST.

## Tecnologías principales

- Next.js.
- React.
- Tailwind CSS.
- Radix UI / shadcn.
- Fetch API.
- Playwright.

## Requisitos

- Node.js 22 o superior.
- npm.

## Instalación

```bash
npm install
```

## Ejecución local

```bash
npm run dev
```

La aplicación se ejecuta por defecto en:

```text
http://localhost:3000
```

## Compilación

```bash
npm run build
```

## Ejecución en modo producción

```bash
npm run start
```

## Configuración de API

La configuración central de API se encuentra en:

```text
src/lib/config.js
```

Desde ese archivo se exportan las constantes:

```javascript
API_URL_BASE
FILE_STORAGE_API_URL_BASE
```

Los componentes deben usar estas constantes para consumir backend y archivos.

## Variables de entorno

El frontend usa variables públicas de Next.js para conocer las URLs del backend.

| Variable | Propósito |
|---|---|
| `NEXT_PUBLIC_API_URL` | Define la URL pública base del backend. |
| `NEXT_PUBLIC_API_URL_BASE` | Define la URL pública base de la API. |
| `NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE` | Define la URL pública base para archivos. |

Las variables `NEXT_PUBLIC_*` son visibles para el navegador y no deben contener secretos, tokens, llaves, credenciales ni datos sensibles.

## Autenticación

Las peticiones protegidas deben enviarse con:

```javascript
credentials: "include"
```

El backend maneja la sesión mediante cookie.

Ejemplo:

```javascript
const response = await fetch(`${API_URL_BASE}/auth/me`, {
  method: "GET",
  credentials: "include",
});
```

## Manejo de sesión

El frontend debe tratar las respuestas de autenticación y autorización según el estado HTTP:

- `401 Unauthorized`: sesión no válida o usuario no autenticado.
- `403 Forbidden`: usuario autenticado sin permiso o sin alcance sobre el recurso.

## Módulos principales

El frontend contiene pantallas y componentes para:

- autenticación;
- inicio;
- usuarios y roles;
- personas;
- catálogos;
- consultas jurídicas;
- seguimientos;
- procesos;
- conciliadores;
- conciliaciones;
- eliminación, reactivación y archivado.

## Archivos

Los endpoints de archivos usan:

```javascript
FILE_STORAGE_API_URL_BASE
```

Las peticiones de archivos protegidas también deben usar:

```javascript
credentials: "include"
```

Cuando un flujo requiera subir documentos, se debe usar `multipart/form-data`.

Ejemplo general:

```javascript
const formData = new FormData();
formData.append("file", archivo);

const response = await fetch(`${FILE_STORAGE_API_URL_BASE}/files/upload`, {
  method: "POST",
  credentials: "include",
  body: formData,
});
```

## Conciliación

El frontend debe consumir el módulo de conciliación considerando las reglas de autorización del backend.

Aspectos principales:

- la solicitud PDF se envía al crear la conciliación;
- el acta PDF se envía al finalizar la conciliación;
- los estados se reciben como `estadoId`, `estadoCodigo` y `estadoNombre`;
- las acciones visibles en interfaz deben respetar permisos;
- la autorización final siempre la valida el backend.

## Permisos

El frontend puede ocultar menús, botones y acciones según permisos del usuario.

La visibilidad en frontend no reemplaza la seguridad del backend. El backend valida permisos y alcance real sobre cada recurso.

Ejemplos de alcance:

- un conciliador opera conciliaciones donde está asignado;
- un estudiante consulta conciliaciones relacionadas;
- un asesor gestiona recursos asociados a sus consultas;
- un monitor gestiona recursos asociados a sus consultas.

## Manejo de errores

El frontend debe soportar respuestas de error estructuradas.

Forma general:

```json
{
  "fecha": "fecha-hora-del-error",
  "estado": 400,
  "error": "Tipo de error",
  "mensaje": "Mensaje descriptivo para el usuario",
  "ruta": "/ruta/del/endpoint"
}
```

Cuando existen errores de validación, puede venir un objeto `detalles`.

Forma general:

```json
{
  "fecha": "fecha-hora-del-error",
  "estado": 400,
  "error": "Error de validación",
  "mensaje": "Uno o más campos no son válidos",
  "ruta": "/ruta/del/endpoint",
  "detalles": {
    "campo": "Mensaje de validación"
  }
}
```

Helper recomendado:

```javascript
async function leerRespuesta(response) {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
}

function obtenerMensajeError(data, fallback = "Ocurrió un error") {
  if (!data) {
    return fallback;
  }

  if (data.detalles && typeof data.detalles === "object") {
    const detalle = Object.values(data.detalles).filter(Boolean).join(". ");

    if (detalle) {
      return detalle;
    }
  }

  return data.mensaje || data.message || data.error || fallback;
}
```

## Pruebas

Comandos disponibles:

```bash
npm run test
npm run test:ui
npm run test:headed
```

## Recomendaciones de desarrollo

- No hardcodear URLs del backend.
- Usar `API_URL_BASE` para endpoints funcionales.
- Usar `FILE_STORAGE_API_URL_BASE` para endpoints de archivos.
- Usar `credentials: "include"` en peticiones protegidas.
- Manejar `401` como sesión no válida.
- Manejar `403` como falta de permiso o alcance.
- No usar variables `NEXT_PUBLIC_*` para información sensible.
- No documentar credenciales reales ni usuarios reales de prueba.