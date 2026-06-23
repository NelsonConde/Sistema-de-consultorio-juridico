/**
 * Utilidades para leer y normalizar respuestas HTTP del backend.
 *
 * Provee funciones para extraer el cuerpo de la respuesta y construir
 * mensajes de error legibles a partir del DTO de error estándar del backend.
 *
 * @module lib/api
 */

/**
 * Lee el cuerpo de una `Response` de fetch y lo devuelve como objeto o string.
 * Devuelve `null` si la respuesta es 204 o no tiene cuerpo.
 *
 * @param {Response} response - La respuesta cruda de fetch.
 * @returns {Promise<object|string|null>} El cuerpo parseado como JSON,
 *   o como string si no es JSON válido, o `null` si está vacío.
 */
export async function readResponseBody(response) {
  if (!response || response.status === 204) return null;

  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

/**
 * Convierte un valor de campo de error a un string de mensaje.
 * Devuelve `null` si el valor está vacío o no es representable directamente.
 *
 * @param {unknown} value - Valor del campo.
 * @param {string|null} fieldName - Nombre del campo, usado como prefijo.
 * @returns {string|null} Mensaje formateado o `null`.
 */
function valueToMessage(value, fieldName) {
  if (value === null || value === undefined || value === "") return null;

  if (typeof value === "string") {
    return fieldName ? `${fieldName}: ${value}` : value;
  }

  if (typeof value === "number" || typeof value === "boolean") {
    return fieldName ? `${fieldName}: ${value}` : String(value);
  }

  return null;
}

/**
 * Recorre recursivamente un valor (string, array u objeto) y acumula
 * los mensajes de error encontrados en el array `messages`.
 *
 * @param {unknown} value - Valor a recorrer.
 * @param {string|null} fieldName - Nombre del campo actual.
 * @param {string[]} messages - Array acumulador de mensajes.
 * @returns {void}
 */
function collectMessages(value, fieldName, messages) {
  const directMessage = valueToMessage(value, fieldName);
  if (directMessage) {
    messages.push(directMessage);
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((item) => collectMessages(item, fieldName, messages));
    return;
  }

  if (value && typeof value === "object") {
    Object.entries(value).forEach(([key, nestedValue]) => {
      collectMessages(nestedValue, key, messages);
    });
  }
}

/**
 * Extrae todos los mensajes de error de detalle del payload de error del backend.
 * Busca en los campos `detalles`, `details`, `errors`, `fieldErrors` y `validaciones`.
 *
 * @param {object|string|null} payload - Cuerpo de la respuesta de error.
 * @returns {string[]} Lista de mensajes de error sin duplicados.
 */
export function getApiErrorMessages(payload) {
  if (!payload) return [];
  if (typeof payload === "string") return payload ? [payload] : [];

  const messages = [];
  const detailSources = [
    payload.detalles,
    payload.details,
    payload.errors,
    payload.fieldErrors,
    payload.validaciones,
  ];

  detailSources.forEach((source) => collectMessages(source, null, messages));

  return [...new Set(messages.filter(Boolean))];
}

/**
 * Extrae el título/mensaje principal del payload de error del backend.
 * Busca en los campos `mensaje`, `message`, `descripcion` y `error`.
 *
 * @param {object|string|null} payload - Cuerpo de la respuesta de error.
 * @param {string} [fallback="Error en la operación"] - Texto por defecto si no hay mensaje.
 * @returns {string} El título del error.
 */
export function getApiErrorTitle(payload, fallback = "Error en la operación") {
  if (!payload) return fallback;
  if (typeof payload === "string") return payload || fallback;

  return (
    payload.mensaje ||
    payload.message ||
    payload.descripcion ||
    payload.error ||
    fallback
  );
}

/**
 * Construye un texto de descripción de error combinando los mensajes de detalle.
 * Si no hay detalles, usa el título del error como descripción.
 *
 * @param {object|string|null} payload - Cuerpo de la respuesta de error.
 * @param {string} [fallback="Verifica la información e intenta nuevamente."] - Texto por defecto.
 * @returns {string} La descripción del error.
 */
export function getApiErrorDescription(payload, fallback = "Verifica la información e intenta nuevamente.") {
  const messages = getApiErrorMessages(payload);

  if (messages.length > 0) {
    return messages.join("\n");
  }

  const title = getApiErrorTitle(payload, "");
  return title || fallback;
}
