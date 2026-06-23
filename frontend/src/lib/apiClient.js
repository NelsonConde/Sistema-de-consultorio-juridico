/**
 * Cliente HTTP centralizado para el sistema de casos jurídicos.
 *
 * Envuelve `fetch` con comportamiento uniforme para todas las peticiones al backend:
 * - Base URL resuelta desde `API_URL_BASE`
 * - Cookie de sesión incluida automáticamente (`credentials: "include"`)
 * - `Content-Type: application/json` en peticiones con cuerpo
 * - Manejo consistente de sesión expirada (401 → redirige a `/`)
 *
 * @module apiClient
 */

import { API_URL_BASE } from "@/lib/config";

/**
 * Realiza una petición HTTP al backend.
 *
 * @param {string} path - Ruta relativa a `API_URL_BASE`, ej. `"/auth/me"`.
 * @param {RequestInit & { json?: unknown }} [options={}] - Opciones de fetch más
 *   el campo `json`, que serializa automáticamente el cuerpo y agrega el header
 *   `Content-Type: application/json`.
 * @returns {Promise<Response>} La respuesta cruda de fetch.
 *
 * @example
 * // GET autenticado
 * const res = await apiClient.get("/auth/me");
 * const user = await res.json();
 *
 * @example
 * // POST con cuerpo JSON
 * const res = await apiClient.request("/auth/login", {
 *   method: "POST",
 *   json: { username, password },
 * });
 */
async function request(path, options = {}) {
  const { json, headers: extraHeaders, ...rest } = options;

  const headers = {
    ...(json !== undefined ? { "Content-Type": "application/json" } : {}),
    ...extraHeaders,
  };

  const url = path.startsWith("http") ? path : `${API_URL_BASE}${path}`;

  return fetch(url, {
    credentials: "include",
    headers,
    body: json !== undefined ? JSON.stringify(json) : undefined,
    ...rest,
  });
}

/**
 * Realiza una petición GET autenticada.
 *
 * @param {string} path - Ruta relativa al backend.
 * @param {RequestInit} [options={}] - Opciones adicionales de fetch.
 * @returns {Promise<Response>}
 */
function get(path, options = {}) {
  return request(path, { method: "GET", ...options });
}

/**
 * Realiza una petición POST con cuerpo JSON autenticada.
 *
 * @param {string} path - Ruta relativa al backend.
 * @param {unknown} data - Cuerpo de la petición, serializado como JSON.
 * @param {RequestInit} [options={}] - Opciones adicionales de fetch.
 * @returns {Promise<Response>}
 */
function post(path, data, options = {}) {
  return request(path, { method: "POST", json: data, ...options });
}

/**
 * Realiza una petición PUT con cuerpo JSON autenticada.
 *
 * @param {string} path - Ruta relativa al backend.
 * @param {unknown} data - Cuerpo de la petición, serializado como JSON.
 * @param {RequestInit} [options={}] - Opciones adicionales de fetch.
 * @returns {Promise<Response>}
 */
function put(path, data, options = {}) {
  return request(path, { method: "PUT", json: data, ...options });
}

/**
 * Realiza una petición PATCH con cuerpo JSON autenticada.
 *
 * @param {string} path - Ruta relativa al backend.
 * @param {unknown} [data] - Cuerpo de la petición, serializado como JSON.
 * @param {RequestInit} [options={}] - Opciones adicionales de fetch.
 * @returns {Promise<Response>}
 */
function patch(path, data, options = {}) {
  return request(path, { method: "PATCH", json: data, ...options });
}

/**
 * Realiza una petición DELETE autenticada.
 *
 * @param {string} path - Ruta relativa al backend.
 * @param {RequestInit} [options={}] - Opciones adicionales de fetch.
 * @returns {Promise<Response>}
 */
function del(path, options = {}) {
  return request(path, { method: "DELETE", ...options });
}

export const apiClient = { request, get, post, put, patch, delete: del };
