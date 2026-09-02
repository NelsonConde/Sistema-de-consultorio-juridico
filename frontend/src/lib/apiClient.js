/**
 * Implementation detail.
 *
 * Responsabilidades:
 * Implementation detail.
 * - Serializar JSON.
 * Keep the CSRF token synchronized with the browser cookie and backend session.
 * - Send CSRF on POST, PUT, PATCH, and DELETE requests.
 * Keep the CSRF token synchronized with the browser cookie and backend session.
 *
 * Implementation detail.
 * Implementation detail.
 * Implementation detail.
 */

import { API_URL_BASE } from "@/lib/config";

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);
const REQUEST_ID_HEADER = "X-Request-ID";

function createRequestId() {
  const cryptoApi = globalThis.crypto;

  if (typeof cryptoApi?.randomUUID === "function") {
    return cryptoApi.randomUUID();
  }

  return null;
}

function addRequestId(headers) {
  if (headers.has(REQUEST_ID_HEADER)) return;

  const requestId = createRequestId();
  if (requestId) {
    headers.set(REQUEST_ID_HEADER, requestId);
  }
}

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
 * Keep the CSRF token synchronized with the browser cookie and backend session.
 *
 * Consultation flow detail.
 * Implementation detail.
 * Implementation detail.
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
    const headers = new Headers();
    addRequestId(headers);

    const response = await fetch(
      `${API_URL_BASE}/auth/csrf`,
      {
        method: "GET",
        credentials: "include",
        cache: "no-store",
        headers,
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
 * Keep the CSRF token synchronized with the browser cookie and backend session.
 */
async function refreshCsrfToken() {
  return getCsrfToken({
    force: true,
  });
}

/**
 * Implementation detail.
 *
 * Implementation detail.
 *
 * Data loading behavior.
 * Implementation detail.
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

  addRequestId(headers);

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
   * Implementation detail.
   * Implementation detail.
   *
   * Implementation detail.
   * Implementation detail.
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
   * Login and logout invalidate the CSRF token used by the backend.
   * Implementation detail.
   *
   * Implementation detail.
   * Implementation detail.
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
