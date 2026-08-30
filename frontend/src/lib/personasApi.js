import { apiClient } from "@/lib/apiClient";
import {
  getApiErrorDescription,
  getApiErrorTitle,
  readResponseBody,
} from "@/lib/api";

const PAGINA_POR_DEFECTO = 1;
const TAMANO_POR_DEFECTO = 10;
const TAMANO_MAXIMO = 50;

function crearErrorHttp(response, payload, fallback) {
  const error = new Error(
    getApiErrorDescription(
      payload,
      getApiErrorTitle(payload, fallback)
    )
  );

  error.status = response.status;
  error.payload = payload;
  return error;
}

function validarPaginacion(page, size) {
  if (!Number.isInteger(page) || page < 1) {
    throw new Error("La página debe ser mayor o igual a 1");
  }

  if (!Number.isInteger(size) || size < 1 || size > TAMANO_MAXIMO) {
    throw new Error("El tamaño debe estar entre 1 y 50");
  }
}

function construirRuta({ search = "", page = 1, size = 10, soloActivas = true }) {
  validarPaginacion(page, size);

  const params = new URLSearchParams();
  const texto = String(search || "").trim();

  if (texto) {
    params.set("search", texto);
  }

  params.set("page", String(page));
  params.set("size", String(size));

  const base = soloActivas ? "/personas/activos" : "/personas";
  return `${base}?${params.toString()}`;
}

function normalizarPagina(payload) {
  if (!payload || !Array.isArray(payload.content)) {
    throw new Error("Respuesta paginada de personas inválida");
  }

  return {
    content: payload.content,
    page: Number(payload.page ?? PAGINA_POR_DEFECTO),
    size: Number(payload.size ?? TAMANO_POR_DEFECTO),
    totalElements: Number(payload.totalElements ?? 0),
    totalPages: Number(payload.totalPages ?? 0),
  };
}

export async function buscarPersonas({
  search = "",
  page = PAGINA_POR_DEFECTO,
  size = TAMANO_POR_DEFECTO,
  soloActivas = true,
  signal,
} = {}) {
  const path = construirRuta({ search, page, size, soloActivas });
  const response = await apiClient.get(path, { signal });
  const payload = await readResponseBody(response);

  if (!response.ok) {
    throw crearErrorHttp(
      response,
      payload,
      "No se pudieron consultar las personas"
    );
  }

  return normalizarPagina(payload);
}

export async function buscarPersonasActivas(options = {}) {
  return buscarPersonas({ ...options, soloActivas: true });
}

export async function obtenerPersonaDetalle(id, { signal } = {}) {
  if (!id) {
    throw new Error("Debe indicar la persona a consultar");
  }

  const response = await apiClient.get(`/personas/${id}`, { signal });
  const payload = await readResponseBody(response);

  if (!response.ok) {
    throw crearErrorHttp(
      response,
      payload,
      "No se pudo consultar la persona"
    );
  }

  return payload;
}
