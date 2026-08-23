import { apiClient } from "@/lib/apiClient";
import { leerRespuesta, mensajeError } from "./procesos.utils";

export async function apiGet(url) {
  const response = await apiClient.request(url, { credentials: "include" });
  const payload = await leerRespuesta(response);
  if (response.status === 401) { const e = new Error("Sesión vencida. Inicia sesión nuevamente."); e.status = 401; throw e; }
  if (response.status === 403) { const e = new Error("No tienes permisos para consultar esta información."); e.status = 403; throw e; }
  if (!response.ok) throw new Error(mensajeError(payload, "No se pudo consultar la información."));
  return payload;
}

/**
 * Realiza una solicitud POST/PUT/DELETE al API con manejo de errores y autenticación.
 * Automáticamente agrega header Content-Type: application/json.
 * 
 * @async
 * @param {string} url - URL del endpoint
 * @param {Object} options - Opciones de fetch (method, body, headers, etc)
 * @returns {Promise<*>} Datos de respuesta
 * @throws {Error} Si falla la autenticación, autorización o la solicitud
 */
export async function apiEnviar(url, options) {
  const response = await apiClient.request(url, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options?.headers || {}) },
    ...options,
  });
  const payload = await leerRespuesta(response);
  if (response.status === 401) { const e = new Error("Sesión vencida. Inicia sesión nuevamente."); e.status = 401; throw e; }
  if (response.status === 403) { const e = new Error("No tienes permisos para realizar esta acción."); e.status = 403; throw e; }
  if (!response.ok) throw new Error(mensajeError(payload, "No se pudo procesar la solicitud."));
  return payload;
}

/**
 * Verifica si el usuario puede acceder a la sección de procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene alguno de los permisos necesarios
 */
