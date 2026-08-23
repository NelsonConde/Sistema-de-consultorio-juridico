/**
 * Cliente HTTP centralizado para el backend.
 *
 * Responsabilidades:
 * - Incluir cookies con credentials: "include".
 * - Serializar JSON.
 * - Obtener el token CSRF desde /auth/csrf.
 * - Enviar CSRF en POST, PUT, PATCH y DELETE.
 * - Mantener el token CSRF únicamente en memoria.
 *
 * Se solicita un CSRF actualizado antes de cada operación mutable para evitar
 * enviar un token almacenado en memoria que ya no corresponda con la cookie
 * XSRF-TOKEN actual del navegador.
 */

import { API_URL_BASE } from "@/lib/config";

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

let csrfToken = null;
let csrfRequest = null;

function resolveUrl(path) {
  return path.startsWith("http")
    ? path
    : `${API_URL_BASE}${path}`;
}

function isCsrfRotationEndpoint(url) {
  const pathname = new URL(url).pathname;

  return (
    pathname.endsWith("/auth/login") ||
    pathname.endsWith("/auth/logout")
  );
}

function clearCsrfToken() {
  csrfToken = null;
}

/**
 * Obtiene el token CSRF actual.
 *
 * force=true descarta el token almacenado en memoria y consulta nuevamente
 * /auth/csrf. Las solicitudes simultáneas comparten la misma petición para
 * evitar carreras que puedan generar tokens distintos.
 */
async function getCsrfToken({ force = false } = {}) {
  if (csrfRequest) {
    return csrfRequest;
  }

  if (force) {
    clearCsrfToken();
  }

  if (csrfToken) {
    return csrfToken;
  }

  csrfRequest = (async () => {
    const response = await fetch(
      `${API_URL_BASE}/auth/csrf`,
      {
        method: "GET",
        credentials: "include",
        cache: "no-store",
      }
    );

    if (!response.ok) {
      throw new Error(
        `No fue posible obtener el token CSRF (${response.status})`
      );
    }

    const data = await response.json();

    if (!data?.headerName || !data?.token) {
      throw new Error(
        "Respuesta CSRF inválida"
      );
    }

    csrfToken = {
      headerName: data.headerName,
      token: data.token,
    };

    return csrfToken;
  })();

  try {
    return await csrfRequest;
  } finally {
    csrfRequest = null;
  }
}

/**
 * Fuerza la obtención de un nuevo token CSRF.
 */
async function refreshCsrfToken() {
  return getCsrfToken({
    force: true,
  });
}

/**
 * Ejecuta una petición HTTP.
 *
 * GET, HEAD y OPTIONS no requieren CSRF.
 *
 * POST, PUT, PATCH y DELETE obtienen un token
 * CSRF actualizado antes de ejecutar la petición.
 */
async function request(
  path,
  options = {}
) {
  const {
    json,
    headers: extraHeaders,
    method: requestedMethod = "GET",
    body,
    ...rest
  } = options;

  const method =
    String(
      requestedMethod
    ).toUpperCase();

  const headers =
    new Headers(
      extraHeaders || {}
    );

  const url =
    resolveUrl(path);

  if (
    json !== undefined &&
    !headers.has(
      "Content-Type"
    )
  ) {
    headers.set(
      "Content-Type",
      "application/json"
    );
  }

  /*
   * Antes de cada operación mutable obtenemos
   * el CSRF actual del backend.
   *
   * Esto evita enviar un token viejo almacenado
   * en memoria cuando la cookie XSRF-TOKEN ya cambió.
   */
  if (
    !SAFE_METHODS.has(
      method
    )
  ) {
    const csrf =
      await getCsrfToken({
        force: true,
      });

    headers.set(
      csrf.headerName,
      csrf.token
    );
  }

  const response =
    await fetch(url, {
      ...rest,
      method,
      credentials:
        "include",
      headers,
      body:
        json !== undefined
          ? JSON.stringify(
              json
            )
          : body,
    });

  /*
   * Login y logout invalidan el CSRF utilizado
   * en el backend.
   *
   * Se elimina la copia en memoria para que
   * la siguiente operación obtenga uno nuevo.
   */
  if (
    response.ok &&
    isCsrfRotationEndpoint(
      url
    )
  ) {
    clearCsrfToken();
  }

  return response;
}

function get(
  path,
  options = {}
) {
  return request(path, {
    method: "GET",
    ...options,
  });
}

function post(
  path,
  data,
  options = {}
) {
  return request(path, {
    method: "POST",
    json: data,
    ...options,
  });
}

function put(
  path,
  data,
  options = {}
) {
  return request(path, {
    method: "PUT",
    json: data,
    ...options,
  });
}

function patch(
  path,
  data,
  options = {}
) {
  return request(path, {
    method: "PATCH",
    json: data,
    ...options,
  });
}

function del(
  path,
  options = {}
) {
  return request(path, {
    method: "DELETE",
    ...options,
  });
}

export const apiClient = {
  request,
  get,
  post,
  put,
  patch,
  delete: del,
  getCsrfToken,
  refreshCsrfToken,
};