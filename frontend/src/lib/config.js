/**
 * Configuración de URLs de la aplicación.
 *
 * Las URLs se resuelven desde variables de entorno `NEXT_PUBLIC_*`.
 * Si no se definen, se usa `http://localhost:8080/api` como valor por defecto
 * para facilitar el desarrollo local.
 *
 * Variables de entorno soportadas (en orden de prioridad):
 * - `NEXT_PUBLIC_API_URL_BASE`
 * - `NEXT_PUBLIC_API_URL`
 * - `NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE`
 *
 * @module lib/config
 */

/**
 * Normaliza una URL de API asegurándose de que:
 * - Tenga esquema `http://` o `https://` (agrega `https://` si falta).
 * - No termine en `/`.
 * - Termine en `/api`.
 *
 * @param {string|undefined} url - URL cruda desde la variable de entorno.
 * @returns {string} URL normalizada lista para usar con `fetch`.
 */
function normalizarApiUrl(url) {
  let apiUrl = url || "http://localhost:8080/api";

  if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
    apiUrl = `https://${apiUrl}`;
  }

  if (apiUrl.endsWith("/") && apiUrl.length > 1) {
    apiUrl = apiUrl.slice(0, -1);
  }

  if (!apiUrl.endsWith("/api")) {
    apiUrl = `${apiUrl}/api`;
  }

  return apiUrl;
}

/**
 * URL base del backend principal del sistema jurídico.
 * Todas las rutas de la API se construyen concatenando esta URL con la ruta relativa.
 *
 * @type {string}
 * @example
 * // Resultado típico en desarrollo: "http://localhost:8080/api"
 * fetch(`${API_URL_BASE}/auth/me`)
 */
export const API_URL_BASE = normalizarApiUrl(
  process.env.NEXT_PUBLIC_API_URL_BASE ||
    process.env.NEXT_PUBLIC_API_URL ||
    "http://localhost:8080/api"
);

/**
 * URL base del servicio de almacenamiento de archivos.
 * Por defecto apunta al mismo backend que `API_URL_BASE`.
 *
 * @type {string}
 */
export const FILE_STORAGE_API_URL_BASE = normalizarApiUrl(
  process.env.NEXT_PUBLIC_FILE_STORAGE_API_URL_BASE ||
    process.env.NEXT_PUBLIC_API_URL_BASE ||
    process.env.NEXT_PUBLIC_API_URL ||
    "http://localhost:8080/api"
);
