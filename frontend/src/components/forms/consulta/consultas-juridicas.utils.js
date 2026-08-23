import { API_URL_BASE } from "@/lib/config";
import { getApiErrorMessages, getApiErrorTitle } from "@/lib/api";
import { ESTADOS_CONSULTA } from "./consultas-juridicas.constants";

export function construirUrlConsultas(search = "") {
  const params = new URLSearchParams();
  const texto = String(search || "").trim();

  if (texto) {
    params.set("search", texto);
  }

  const query = params.toString();

  return `${API_URL_BASE}/consultas${query ? `?${query}` : ""}`;
}

export function ordenarConsultasPorIdAscendente(items) {
  return [...items].sort((a, b) => {
    const idA = Number(a?.id ?? Number.MAX_SAFE_INTEGER);
    const idB = Number(b?.id ?? Number.MAX_SAFE_INTEGER);

    return idA - idB;
  });
}

export async function leerJsonSeguro(res) {
  if (res.status === 204) return null;

  const text = await res.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export function obtenerArrayDesdeRespuesta(payload) {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== "object") return [];

  const posiblesClaves = [
    "content",
    "data",
    "items",
    "rows",
    "consultas",
    "resultado",
    "result",
    "payload",
  ];

  for (const clave of posiblesClaves) {
    const valor = payload[clave];
    if (Array.isArray(valor)) return valor;
    if (valor && typeof valor === "object") {
      const interno = obtenerArrayDesdeRespuesta(valor);
      if (interno.length > 0) return interno;
    }
  }

  return [];
}

export function valorDefinido(...valores) {
  return valores.find((valor) => valor !== undefined && valor !== null && valor !== "") ?? "";
}

export function idNormalizado(valor) {
  if (valor === undefined || valor === null || valor === "") {
    return "";
  }

  return String(valor);
}

export function obtenerAreaIdAsesor(asesor) {
  return idNormalizado(asesor?.areaId ?? asesor?.area?.id);
}

export function obtenerAsesorIdEstudiante(estudiante) {
  return idNormalizado(estudiante?.asesorId ?? estudiante?.asesor?.id);
}

export function normalizarEstadoConsulta(estado) {
  const texto = String(estado || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase()
    .replace(/\s+/g, "_");

  const equivalencias = {
    ACTIVO: "ACTIVO",
    ACTIVA: "ACTIVO",
    EN_PROCESO: "EN_PROCESO",
    PENDIENTE: "PENDIENTE",
    URGENTE: "URGENTE",
    CERRADO: "CERRADO",
    CERRADA: "CERRADO",
    ARCHIVADO: "ARCHIVADO",
    ARCHIVADA: "ARCHIVADO",
  };

  return equivalencias[texto] || texto || "";
}

export function textoVacio(valor) {
  return !String(valor || "").trim();
}

export function textoNormalizado(valor) {
  return String(valor || "").trim();
}

export function labelEstadoConsulta(estado) {
  const estadoNormalizado = normalizarEstadoConsulta(estado);
  return (
    ESTADOS_CONSULTA.find((item) => item.value === estadoNormalizado)?.label ||
    estado ||
    "Sin estado"
  );
}

export function normalizarConsultaFila(row) {
  const persona = row?.persona || row?.consultante || row?.partePrincipal || {};
  const id = valorDefinido(row?.id, row?.consultaId, row?.idConsulta);

  return {
    ...row,
    id,
    consulta: valorDefinido(
      row?.consulta,
      row?.descripcion,
      row?.descripcionConsulta,
      row?.hechos,
      id ? `Consulta #${id}` : "Consulta"
    ),
    fecha: valorDefinido(row?.fecha, row?.fechaConsulta, row?.createdAt, row?.fechaCreacion),
    nombre: valorDefinido(
      row?.nombre,
      row?.nombres,
      row?.personaNombre,
      row?.nombrePersona,
      persona?.nombre,
      persona?.nombres
    ),
    apellido: valorDefinido(
      row?.apellido,
      row?.apellidos,
      row?.personaApellido,
      row?.apellidoPersona,
      persona?.apellido,
      persona?.apellidos
    ),
    cedula: valorDefinido(
      row?.cedula,
      row?.documento,
      row?.numeroDocumento,
      row?.personaDocumento,
      persona?.documento,
      persona?.numeroDocumento
    ),
    estado: normalizarEstadoConsulta(valorDefinido(row?.estado, row?.estadoConsulta, "")),
  };
}

export function mensajeErrorDesdeRespuesta(payload, defecto) {
  const detalles = getApiErrorMessages(payload);

  if (detalles.length > 0) {
    return detalles.join("\n");
  }

  return getApiErrorTitle(payload, defecto);
}

export function accionPermitidaPorRegistro(row, claves = [], fallback = false) {
  const acciones = row?.accionesPermitidas;
  if (!acciones || typeof acciones !== "object") return fallback;

  for (const clave of claves) {
    if (Object.prototype.hasOwnProperty.call(acciones, clave)) {
      return Boolean(acciones[clave]);
    }
  }

  return fallback;
}
