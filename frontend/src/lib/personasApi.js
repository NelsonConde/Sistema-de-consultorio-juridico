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

function construirRuta({
  search = "",
  page = PAGINA_POR_DEFECTO,
  size = TAMANO_POR_DEFECTO,
  sortBy = "nombres",
  direction = "asc",
  soloActivas = true,
}) {
  validarPaginacion(page, size);

  const params = new URLSearchParams();
  const texto = String(search || "").trim();

  if (texto) {
    params.set("search", texto);
  }

  params.set("page", String(page));
  params.set("size", String(size));
  params.set("sortBy", String(sortBy || "nombres"));
  params.set("direction", String(direction || "asc").toLowerCase());

  const base = soloActivas ? "/personas/activos" : "/personas";
  return `${base}?${params.toString()}`;
}

function obtenerTipoPersona(persona) {
  if (typeof persona?.tipoPersona === "string") {
    return persona.tipoPersona;
  }

  if (persona?.tipoPersona?.nombre) {
    return persona.tipoPersona.nombre;
  }

  if (persona?.tipoPersonaNombre) {
    return persona.tipoPersonaNombre;
  }

  if (persona?.tipoPersonaId !== null && persona?.tipoPersonaId !== undefined) {
    return String(persona.tipoPersonaId);
  }

  return null;
}

function normalizarPersonaResumen(persona) {
  return {
    id: persona?.id ?? null,
    version: persona?.version ?? null,
    nombres: persona?.nombres ?? "",
    apellidos: persona?.apellidos ?? "",
    tipoDocumento: persona?.tipoDocumento ?? "",
    numeroDocumentoEnmascarado:
      persona?.numeroDocumentoEnmascarado ??
      persona?.numeroDocumento ??
      null,
    tipoPersona: obtenerTipoPersona(persona),
    activo: persona?.activo ?? true,
  };
}

function normalizarPagina(payload) {
  if (!payload || !Array.isArray(payload.content)) {
    throw new Error("Respuesta paginada de personas inválida");
  }

  const page = Number(payload.page);
  const size = Number(payload.size);
  const totalElements = Number(payload.totalElements);
  const totalPages = Number(payload.totalPages);

  if (
    !Number.isInteger(page) || page < 1 ||
    !Number.isInteger(size) || size < 1 || size > TAMANO_MAXIMO ||
    !Number.isFinite(totalElements) || totalElements < 0 ||
    !Number.isInteger(totalPages) || totalPages < 0
  ) {
    throw new Error("Contrato paginado de personas inválido");
  }

  return {
    content: payload.content.map(normalizarPersonaResumen),
    page,
    size,
    totalElements,
    totalPages,
  };
}

export async function buscarPersonas({
  search = "",
  page = PAGINA_POR_DEFECTO,
  size = TAMANO_POR_DEFECTO,
  sortBy = "nombres",
  direction = "asc",
  soloActivas = true,
  signal,
} = {}) {
  const options = {
    search,
    page,
    size,
    sortBy,
    direction,
    soloActivas,
  };

  const path = construirRuta(options);
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
  return buscarPersonas({
    ...options,
    soloActivas: true,
  });
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
