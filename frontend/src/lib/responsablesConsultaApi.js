import { ApiError, apiResponse, getApiErrorDescription } from "@/lib/api";

export const TIPOS_RESPONSABLE_CONSULTA = Object.freeze({
  ESTUDIANTE: "ESTUDIANTE",
  ASESOR: "ASESOR",
  MONITOR: "MONITOR",
});

const ENDPOINTS_RESPONSABLE = Object.freeze({
  [TIPOS_RESPONSABLE_CONSULTA.ASESOR]: "/asesores",
  [TIPOS_RESPONSABLE_CONSULTA.MONITOR]: "/monitores",
  [TIPOS_RESPONSABLE_CONSULTA.ESTUDIANTE]: "/estudiantes",
});

async function solicitarDatos(path, options, fallback) {
  const { response, data, correlationId } = await apiResponse(path, options);

  if (response.ok) return data;

  throw new ApiError(getApiErrorDescription(data, fallback), {
    status: response.status,
    payload: data,
    response,
    correlationId,
  });
}

export function reasignarResponsableConsulta({
  consultaId,
  tipoResponsable,
  nuevoResponsableId,
  motivo,
  version,
}) {
  return solicitarDatos(
    `/consultas/${encodeURIComponent(String(consultaId))}/responsable`,
    {
      method: "PATCH",
      json: {
        tipoResponsable,
        nuevoResponsableId: Number(nuevoResponsableId),
        motivo: String(motivo || "").trim(),
        version,
      },
    },
    "No fue posible reasignar el responsable.",
  );
}

export async function obtenerNombreResponsableConsulta(
  tipoResponsable,
  responsableId,
  { signal } = {},
) {
  const endpoint =
    ENDPOINTS_RESPONSABLE[String(tipoResponsable || "").toUpperCase()];
  if (!endpoint || !responsableId) return null;

  const payload = await solicitarDatos(
    `${endpoint}/${encodeURIComponent(String(responsableId))}`,
    { method: "GET", signal },
    "No fue posible consultar el nombre del responsable.",
  );
  const responsable = payload?.data || payload?.perfil || payload;

  return textoOpcional(responsable?.nombre, responsable?.nombreCompleto);
}

function extraerEventos(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.historial)) return payload.historial;
  if (Array.isArray(payload?.items)) return payload.items;
  return null;
}

function textoOpcional(...valores) {
  const valor = valores.find(
    (item) => typeof item === "string" && item.trim().length > 0,
  );
  return valor?.trim() || null;
}

function numeroOpcional(...valores) {
  const valor = valores.find(
    (item) => item !== null && item !== undefined && item !== "",
  );
  if (valor === undefined) return null;

  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : null;
}

function normalizarEvento(evento) {
  return {
    id: numeroOpcional(evento?.id),
    consultaId: numeroOpcional(evento?.consultaId, evento?.consulta?.id),
    tipoResponsable: textoOpcional(
      evento?.tipoResponsable,
      evento?.rolResponsable,
      evento?.tipo,
    ),
    responsableAnteriorId: numeroOpcional(
      evento?.responsableAnteriorId,
      evento?.responsableAnterior?.id,
    ),
    responsableAnteriorNombre: textoOpcional(
      evento?.responsableAnteriorNombre,
      evento?.nombreResponsableAnterior,
      evento?.responsableAnterior?.nombre,
      evento?.responsableAnterior?.nombreCompleto,
    ),
    responsableNuevoId: numeroOpcional(
      evento?.responsableNuevoId,
      evento?.responsableNuevo?.id,
    ),
    responsableNuevoNombre: textoOpcional(
      evento?.responsableNuevoNombre,
      evento?.nombreResponsableNuevo,
      evento?.responsableNuevo?.nombre,
      evento?.responsableNuevo?.nombreCompleto,
    ),
    motivo: textoOpcional(evento?.motivo) || "Motivo no disponible",
    actorUsuarioId: numeroOpcional(
      evento?.actorUsuarioId,
      evento?.actor?.id,
      evento?.usuarioActorId,
    ),
    actorNombre: textoOpcional(
      evento?.actorNombre,
      evento?.nombreActor,
      evento?.actor?.nombre,
      evento?.actor?.nombreCompleto,
      evento?.usuarioActorNombre,
    ),
    ocurridoEn: textoOpcional(
      evento?.ocurridoEn,
      evento?.fecha,
      evento?.fechaEvento,
      evento?.creadoEn,
    ),
  };
}

function fechaOrdenable(valor) {
  const timestamp = Date.parse(valor || "");
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

export async function obtenerHistorialResponsables(
  consultaId,
  { signal } = {},
) {
  const payload = await solicitarDatos(
    `/consultas/${encodeURIComponent(String(consultaId))}/responsables/historial`,
    { method: "GET", signal },
    "No fue posible cargar el historial de responsables.",
  );
  const eventos = extraerEventos(payload);

  if (!eventos) {
    throw new Error(
      "La respuesta del historial de responsables no tiene un formato válido.",
    );
  }

  return eventos.map(normalizarEvento).sort((a, b) => {
    const diferenciaFecha =
      fechaOrdenable(b.ocurridoEn) - fechaOrdenable(a.ocurridoEn);
    if (diferenciaFecha !== 0) return diferenciaFecha;
    return (b.id || 0) - (a.id || 0);
  });
}
