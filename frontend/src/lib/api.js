import { apiClient } from "@/lib/apiClient";

const REQUEST_ID_HEADER = "X-Request-ID";

/**
 * Implementation detail.
 *
 * Implementation detail.
 * Error handling.
 *
 * @module lib/api
 */

/**
 * Implementation detail.
 * Implementation detail.
 *
 * @param {Response} response - Parameter description.
 * @returns {Promise<object|string|null>} Result value.
 * Implementation detail.
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
 * Returns the canonical correlation identifier for a completed HTTP response.
 * The response header is authoritative; the backend error payload is a fallback.
 */
export function getResponseCorrelationId(response, payload = null) {
  const fromHeader = response?.headers?.get?.(REQUEST_ID_HEADER);
  if (fromHeader) return fromHeader;

  if (payload && typeof payload === "object") {
    return payload.correlacionId || null;
  }

  return null;
}

/**
 * Appends a support reference without exposing backend payloads or internal data.
 */
export function withErrorReference(description, correlationId) {
  if (!correlationId) return description;
  return `${description}\nReferencia: ${correlationId}`;
}

/**
 * Error handling.
 * Implementation detail.
 *
 * @param {unknown} value - Parameter description.
 * @param {string|null} fieldName - Parameter description.
 * @returns {string|null} Result value.
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
 * Implementation detail.
 * Error handling.
 *
 * @param {unknown} value - Value to inspect.
 * @param {string|null} fieldName - Parameter description.
 * @param {string[]} messages - Parameter description.
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
 * Error handling.
 * Validation rule.
 *
 * @param {object|string|null} payload - Parameter description.
 * @returns {string[]} Result value.
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
 * Error handling.
 * Error handling.
 *
 * @param {object|string|null} payload - Parameter description.
 * @param {string} [fallback] - Fallback value.
 * @returns {string} Result value.
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
 * Error handling.
 * Error handling.
 *
 * @param {object|string|null} payload - Parameter description.
 * @param {string} [fallback] - Fallback value.
 * @returns {string} Result value.
 */
export function getApiErrorDescription(payload, fallback = "Verifica la información e intenta nuevamente.") {
  const messages = getApiErrorMessages(payload);

  if (messages.length > 0) {
    return messages.join("\n");
  }

  const title = getApiErrorTitle(payload, "");
  return title || fallback;
}
/**
 * Error handling.
 * State handling.
 * Implementation detail.
 */
export class ApiError extends Error {
  constructor(
    message,
    {
      status = 0,
      payload = null,
      response = null,
      correlationId = null,
    } = {}
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
    // Keep a body alias because DB-03 conflict helpers may receive errors from
    // different transport wrappers while preserving the same backend payload.
    this.body = payload;
    this.response = response;
    this.correlationId = correlationId;
  }
}

/**
 * Returns true only for optimistic-concurrency conflicts.
 * Authentication, authorization, validation, and transport failures must keep
 * their existing handling paths.
 */
export function isConcurrencyConflict(error) {
  const status = Number(
    error?.status ??
      error?.response?.status ??
      error?.payload?.estado ??
      error?.body?.estado ??
      0
  );

  return status === 409;
}

/**
 * Normalizes the backend concurrency payload without changing the draft owned
 * by the calling screen.
 */
export function toConcurrencyConflict(error) {
  const payload = error?.payload ?? error?.body ?? null;

  return {
    message:
      payload?.mensaje ||
      payload?.message ||
      error?.message ||
      "El recurso fue modificado por otro usuario.",
    correlationId: error?.correlationId ?? payload?.correlacionId ?? null,
    path: payload?.ruta ?? null,
  };
}

/**
 * Returns the exact server-owned version required by a DB-03 mutation.
 * The frontend must never invent or increment this value.
 */
export function requireResourceVersion(resource, label = "recurso") {
  const version = resource?.version;

  if (version === null || version === undefined || version === "") {
    throw new Error(
      `No se recibió la versión actual del ${label}. Actualiza la información antes de continuar.`
    );
  }

  return version;
}

/**
 * Appends the exact optimistic-locking version to an endpoint query string.
 */
export function withResourceVersion(path, resource, label = "recurso") {
  const version = requireResourceVersion(resource, label);
  const separator = path.includes("?") ? "&" : "?";
  return `${path}${separator}version=${encodeURIComponent(String(version))}`;
}

/**
 * Implementation detail.
 * Implementation detail.
 */
export async function apiResponse(path, options = {}) {
  const response = await apiClient.request(path, options);
  const data = await readResponseBody(response);
  const correlationId = getResponseCorrelationId(response, data);
  return { response, data, correlationId };
}

/**
 * Implementation detail.
 * Error handling.
 * Error handling.
 * Implementation detail.
 */
export async function apiRequestData(
  path,
  options = {},
  {
    fallback = "Error en la operación",
    statusMessages = {},
  } = {}
) {
  const { response, data, correlationId } = await apiResponse(path, options);

  if (response.ok) {
    return data;
  }

  const message =
    statusMessages?.[response.status] ||
    getApiErrorTitle(data, fallback);

  throw new ApiError(message, {
    status: response.status,
    payload: data,
    response,
    correlationId,
  });
}

/**
 * Error handling.
 */
export function getApiErrorStatus(error) {
  return Number(error?.status || 0);
}

/**
 * Reads an already-normalized correlation reference from an error.
 */
export function getApiErrorCorrelationId(error) {
  return (
    error?.correlationId ||
    error?.payload?.correlacionId ||
    error?.body?.correlacionId ||
    null
  );
}
