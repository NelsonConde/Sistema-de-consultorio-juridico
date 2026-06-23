# Configuración de API

El frontend centraliza la configuración de conexión con el backend en `src/lib/config.js`. Los componentes no deben construir URLs base manualmente; deben importar `API_URL_BASE` y, cuando aplique, `FILE_STORAGE_API_URL_BASE`.

## Variables de entorno soportadas

El frontend usa variables públicas de Next.js. Estas variables son visibles en el navegador y por esa razón no deben contener secretos.

| Variable | Uso |
|---|---|
| `NEXT_PUBLIC_API_URL_BASE` | URL base del backend incluyendo `/api`. |
| `NEXT_PUBLIC_API_URL` | URL del backend sin `/api`; `config.js` agrega el sufijo si falta. |
| `NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE` | URL base para operaciones de archivos. Si no se define, usa la misma base del backend. |

`src/lib/config.js` normaliza la URL para asegurar que:

1. tenga esquema `http://` o `https://`;
2. no termine con `/`;
3. termine con `/api`.

## Valores por defecto

Si no se define ninguna variable de entorno, el frontend usa:

```text
http://localhost:8080/api
```

Este valor permite ejecutar el sistema en ambiente local con backend en el puerto 8080.

## Constantes exportadas

```javascript
export const API_URL_BASE
export const FILE_STORAGE_API_URL_BASE
```

`API_URL_BASE` se usa para los endpoints principales: autenticación, consultas, personas, procesos, seguimientos, perfiles, permisos, estadísticas y catálogos.

`FILE_STORAGE_API_URL_BASE` se usa para operaciones de archivos. Por defecto apunta al mismo backend.

## Ejemplo de uso correcto

```javascript
import { API_URL_BASE } from "@/lib/config";

const response = await fetch(`${API_URL_BASE}/auth/me`, {
  method: "GET",
  credentials: "include",
});
```

## Uso con archivos

Las operaciones de archivo pueden requerir `FormData` o descarga directa. En esos casos no se debe forzar `Content-Type: application/json`.

```javascript
const formData = new FormData();
formData.append("file", archivo);

await fetch(`${FILE_STORAGE_API_URL_BASE}/files/upload`, {
  method: "POST",
  credentials: "include",
  body: formData,
});
```

## Regla documental de seguridad

Las variables `NEXT_PUBLIC_*` son públicas. La documentación no debe incluir:

- tokens reales;
- API keys;
- contraseñas;
- usuarios reales;
- cadenas JDBC;
- secretos JWT;
- credenciales de correo;
- datos personales reales.

## Relación con Docker y desarrollo local

Cuando el entorno expone una URL sin `/api`, `config.js` la normaliza. Por ejemplo:

```text
NEXT_PUBLIC_API_URL=http://localhost:8080
```

se convierte internamente en:

```text
http://localhost:8080/api
```

Esta normalización evita duplicar configuración entre entornos.

## Uso consistente en componentes

La documentación y el código deben conservar esta convención:

| Caso | Uso recomendado |
|---|---|
| Petición JSON protegida | `API_URL_BASE` + `credentials: "include"` |
| Petición con archivo | `FILE_STORAGE_API_URL_BASE` + `FormData` |
| Endpoint absoluto excepcional | Validar que ya incluya protocolo y ruta completa |
| Login y sesión | `API_URL_BASE}/auth/...` |

No se deben hardcodear URLs como `http://localhost:8080/api/...` dentro de componentes. La URL base debe provenir de `config.js`.
