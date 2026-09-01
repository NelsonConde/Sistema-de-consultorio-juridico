import { apiClient } from "@/lib/apiClient";
import {
  getApiErrorDescription,
  getApiErrorTitle,
  readResponseBody,
} from "@/lib/api";

const PAGINA_POR_DEFECTO = 1;
const TAMANO_POR_DEFECTO = 10;
const TAMANO_MAXIMO = 50;
const DOCUMENTO_VISIBLE = 4;

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

  const base = soloActivas ? "/personas/activos" : "/personas";
  return `${base}?${params.toString()}`;
}

function enmascararDocumento(numeroDocumento) {
  const documento = String(numeroDocumento || "").trim();

  if (!documento) {
    return null;
  }

  if (documento.length <= DOCUMENTO_VISIBLE) {
    return "*".repeat(documento.length);
  }

  const ocultos = documento.length - DOCUMENTO_VISIBLE;
  return "*".repeat(ocultos) + documento.slice(-DOCUMENTO_VISIBLE);
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
      enmascararDocumento(persona?.numeroDocumento),
    tipoPersona: obtenerTipoPersona(persona),
    activo: persona?.activo ?? true,
  };
}

function coincideBusqueda(persona, search) {
  const termino = String(search || "").trim().toLocaleLowerCase();

  if (!termino) {
    return true;
  }

  const valores = [
    persona?.nombres,
    persona?.apellidos,
    `${persona?.nombres || ""} ${persona?.apellidos || ""}`,
    persona?.numeroDocumento,
    persona?.numeroDocumentoEnmascarado,
    persona?.tipoDocumento,
  ];

  return valores.some((valor) =>
    String(valor || "").toLocaleLowerCase().includes(termino)
  );
}

function normalizarPaginaPaginada(payload) {
  return {
    content: payload.content.map(normalizarPersonaResumen),
    page: Number(payload.page ?? PAGINA_POR_DEFECTO),
    size: Number(payload.size ?? TAMANO_POR_DEFECTO),
    totalElements: Number(payload.totalElements ?? payload.content.length),
    totalPages: Number(payload.totalPages ?? 0),
  };
}

/**
 * Compatibility fallback for branches where the backend still returns an array.
 * The preferred SEC-07 contract is the paginated object handled above.
 * This fallback only prevents the existing frontend from breaking while the
 * backend remains unchanged.
 */
function normalizarPaginaLegacy(
  payload,
  {
    search = "",
    page = PAGINA_POR_DEFECTO,
    size = TAMANO_POR_DEFECTO,
    soloActivas = true,
  } = {}
) {
  const filtradas = payload.filter((persona) => {
    if (soloActivas && persona?.activo === false) {
      return false;
    }

    return coincideBusqueda(persona, search);
  });

  const totalElements = filtradas.length;
  const totalPages =
    totalElements === 0 ? 0 : Math.ceil(totalElements / size);

  const paginaSegura =
    totalPages === 0 ? 1 : Math.min(Math.max(page, 1), totalPages);

  const inicio = (paginaSegura - 1) * size;
  const fin = inicio + size;

  return {
    content: filtradas
      .slice(inicio, fin)
      .map(normalizarPersonaResumen),
    page: paginaSegura,
    size,
    totalElements,
    totalPages,
  };
}

function normalizarPagina(payload, options = {}) {
  if (payload && Array.isArray(payload.content)) {
    return normalizarPaginaPaginada(payload);
  }

  if (Array.isArray(payload)) {
    return normalizarPaginaLegacy(payload, options);
  }

  throw new Error("Respuesta de personas inválida");
}

export async function buscarPersonas({
  search = "",
  page = PAGINA_POR_DEFECTO,
  size = TAMANO_POR_DEFECTO,
  soloActivas = true,
  signal,
} = {}) {
  const options = {
    search,
    page,
    size,
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

  return normalizarPagina(payload, options);
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
