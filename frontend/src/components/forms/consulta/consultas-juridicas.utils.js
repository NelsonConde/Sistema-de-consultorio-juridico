import { API_URL_BASE } from "@/lib/config";
import { getApiErrorMessages, getApiErrorTitle, readResponseBody } from "@/lib/api";
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
  return readResponseBody(res);
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

function tieneDuplicados(values = []) {
  const normalizados = values.map(Number).filter((value) => !Number.isNaN(value));
  return new Set(normalizados).size !== normalizados.length;
}

/**
 * Validation rule.
 * Data loading behavior.
 */
export function validarCoherenciaConsultaFrontend({
  form,
  temas = [],
  tipos = [],
  asesores = [],
  monitores = [],
  estudiantes = [],
}) {
  const areaId = idNormalizado(form?.areaId);
  const temaId = idNormalizado(form?.temaId);
  const tipoId = idNormalizado(form?.tipoId);
  const asesorId = idNormalizado(form?.asesorId);
  const monitorId = idNormalizado(form?.monitorId);
  const estudianteId = idNormalizado(form?.estudianteId);

  if (temaId && temas.length > 0 && !temas.some((tema) => idNormalizado(tema?.id) === temaId)) {
    return "El tema seleccionado no pertenece al área de la consulta";
  }

  if (tipoId && tipos.length > 0 && !tipos.some((tipo) => idNormalizado(tipo?.id) === tipoId)) {
    return "El tipo seleccionado no pertenece al tema de la consulta";
  }

  if (asesorId && asesores.length > 0) {
    const asesor = asesores.find((item) => idNormalizado(item?.id) === asesorId);

    if (!asesor) {
      return "Asesor no encontrado o inactivo";
    }

    if (!areaId || obtenerAreaIdAsesor(asesor) !== areaId) {
      return "El asesor asignado no pertenece al área de la consulta";
    }
  }

  if (monitorId && monitores.length > 0) {
    const monitor = monitores.find((item) => idNormalizado(item?.id) === monitorId);

    if (!monitor) {
      return "Monitor no encontrado o inactivo";
    }
  }

  if (estudianteId && estudiantes.length > 0) {
    const estudiante = estudiantes.find(
      (item) => idNormalizado(item?.id) === estudianteId
    );

    if (!estudiante) {
      return "Estudiante no encontrado o inactivo";
    }

    const asesorEstudianteId = obtenerAsesorIdEstudiante(estudiante);

    if (!asesorEstudianteId) {
      return "El estudiante seleccionado no tiene asesor asignado";
    }

    if (asesorId && asesorEstudianteId !== asesorId) {
      return "El estudiante asignado no pertenece al asesor seleccionado";
    }

    if (asesores.length > 0) {
      const asesorDelEstudiante = asesores.find(
        (item) => idNormalizado(item?.id) === asesorEstudianteId
      );

      if (!asesorDelEstudiante) {
        return "El asesor asignado al estudiante no existe o está inactivo";
      }

      if (!areaId || obtenerAreaIdAsesor(asesorDelEstudiante) !== areaId) {
        return "El asesor del estudiante no pertenece al área de la consulta";
      }
    }
  }

  const personaId = Number(form?.personaId);
  const partesIds = Array.isArray(form?.partesIds)
    ? form.partesIds.map(Number).filter((value) => !Number.isNaN(value))
    : [];
  const contrapartesIds = Array.isArray(form?.contrapartesIds)
    ? form.contrapartesIds.map(Number).filter((value) => !Number.isNaN(value))
    : [];

  if (!Number.isNaN(personaId) && partesIds.includes(personaId)) {
    return "La persona principal no puede repetirse como parte adicional";
  }

  if (!Number.isNaN(personaId) && contrapartesIds.includes(personaId)) {
    return "La persona principal no puede repetirse como contraparte";
  }

  if (partesIds.some((id) => contrapartesIds.includes(id))) {
    return "Una misma persona no puede estar como parte y contraparte";
  }

  if (tieneDuplicados(form?.partesIds || [])) {
    return "Existen personas repetidas en partes";
  }

  if (tieneDuplicados(form?.contrapartesIds || [])) {
    return "Existen personas repetidas en contrapartes";
  }

  return null;
}
