/**
 * Implementation detail.
 *
 * Implementation detail.
 * Normalize the URL scheme and API path before using the configured endpoint.
 * Role handling.
 *
 * Handles numeric identifiers consistently for list ordering.
 * - `NEXT_PUBLIC_API_URL_BASE`
 * - `NEXT_PUBLIC_API_URL`
 *
 * @module lib/config
 */

/**
 * API routes are built from the configured base URL and a relative path.
 * Normalize the URL scheme and API path before using the configured endpoint.
 * Implementation detail.
 * - Termine en `/api`.
 *
 * @param {string|undefined} url - Parameter description.
 * @returns {string} Result value.
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
 * Implementation detail.
 * API routes are built from the configured base URL and a relative path.
 *
 * @type {string}
 * @example
 * Normalize the URL scheme and API path before using the configured endpoint.
 * apiClient.get(`${API_URL_BASE}/auth/me`)
 */
export const API_URL_BASE = normalizarApiUrl(
  process.env.NEXT_PUBLIC_API_URL_BASE ||
    process.env.NEXT_PUBLIC_API_URL ||
    "http://localhost:8080/api"
);
