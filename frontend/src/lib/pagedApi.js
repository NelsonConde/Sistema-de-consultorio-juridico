import {
  ApiError,
  getApiErrorDescription,
  getResponseCorrelationId,
  readResponseBody,
} from "@/lib/api";
import { apiClient } from "@/lib/apiClient";

export const DEFAULT_REMOTE_PAGE_SIZE = 10;
export const MAX_REMOTE_PAGE_SIZE = 50;

function toPositiveInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function buildPagedPath(
  path,
  {
    search = "",
    page = 1,
    size = DEFAULT_REMOTE_PAGE_SIZE,
    sortBy = "id",
    direction = "desc",
    filters = {},
  } = {}
) {
  const safePage = toPositiveInteger(page, 1);
  const safeSize = Math.min(
    MAX_REMOTE_PAGE_SIZE,
    toPositiveInteger(size, DEFAULT_REMOTE_PAGE_SIZE)
  );
  const safeDirection = String(direction || "desc").toLowerCase();

  if (!["asc", "desc"].includes(safeDirection)) {
    throw new Error("Dirección de orden inválida");
  }

  const params = new URLSearchParams();
  params.set("page", String(safePage));
  params.set("size", String(safeSize));
  params.set("sortBy", String(sortBy || "id"));
  params.set("direction", safeDirection);

  const normalizedSearch = String(search || "").trim();
  if (normalizedSearch) {
    params.set("search", normalizedSearch);
  }

  Object.entries(filters || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    params.set(key, String(value));
  });

  return `${path}?${params.toString()}`;
}

export function normalizePageResponse(payload, resourceName = "listado") {
  const validPayload =
    payload &&
    typeof payload === "object" &&
    !Array.isArray(payload) &&
    Array.isArray(payload.content) &&
    Number.isFinite(Number(payload.page)) &&
    Number.isFinite(Number(payload.size)) &&
    Number.isFinite(Number(payload.totalElements)) &&
    Number.isFinite(Number(payload.totalPages));

  if (!validPayload) {
    throw new Error(`Respuesta paginada de ${resourceName} inválida`);
  }

  return {
    content: payload.content,
    page: Number(payload.page),
    size: Number(payload.size),
    totalElements: Number(payload.totalElements),
    totalPages: Number(payload.totalPages),
  };
}

function normalizeLegacyArray(
  payload,
  { search = "", page = 1, size = DEFAULT_REMOTE_PAGE_SIZE } = {}
) {
  const normalizedSearch = String(search || "").trim().toLowerCase();
  const filtered = normalizedSearch
    ? payload.filter((item) =>
        Object.values(item || {})
          .filter((value) => value !== null && value !== undefined)
          .join(" ")
          .toLowerCase()
          .includes(normalizedSearch)
      )
    : payload;
  const safeSize = Math.min(
    MAX_REMOTE_PAGE_SIZE,
    toPositiveInteger(size, DEFAULT_REMOTE_PAGE_SIZE)
  );
  const totalElements = filtered.length;
  const totalPages = Math.ceil(totalElements / safeSize);
  const requestedPage = toPositiveInteger(page, 1);
  const safePage = totalPages > 0 ? Math.min(requestedPage, totalPages) : 1;
  const start = (safePage - 1) * safeSize;

  return {
    content: filtered.slice(start, start + safeSize),
    page: safePage,
    size: safeSize,
    totalElements,
    totalPages,
  };
}

export async function fetchPaged(
  path,
  {
    search = "",
    page = 1,
    size = DEFAULT_REMOTE_PAGE_SIZE,
    sortBy = "id",
    direction = "desc",
    filters = {},
    signal = undefined,
    resourceName = "listado",
    allowLegacyArray = false,
    legacyPath = "",
  } = {}
) {
  let response = await apiClient.get(
    buildPagedPath(path, {
      search,
      page,
      size,
      sortBy,
      direction,
      filters,
    }),
    { signal }
  );
  let payload = await readResponseBody(response);

  if (response.status === 404 && legacyPath) {
    response = await apiClient.get(legacyPath, { signal });
    payload = await readResponseBody(response);
  }

  if (!response.ok) {
    throw new ApiError(
      getApiErrorDescription(payload, `No fue posible cargar ${resourceName}.`),
      {
        status: response.status,
        payload,
        response,
        correlationId: getResponseCorrelationId(response, payload),
      }
    );
  }

  if (Array.isArray(payload)) {
    if (!allowLegacyArray && !legacyPath) {
      throw new Error(`Respuesta paginada de ${resourceName} inválida`);
    }

    return normalizeLegacyArray(payload, { search, page, size });
  }

  return normalizePageResponse(payload, resourceName);
}

export function isAbortError(error) {
  return (
    error?.name === "AbortError" ||
    error?.cause?.name === "AbortError" ||
    error?.message === "The operation was aborted."
  );
}
