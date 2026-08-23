export function normalizarTexto(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

export function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];

  const claves = [
    "content",
    "data",
    "items",
    "rows",
    "consultas",
    "conciliaciones",
    "estudiantes",
    "conciliadores",
    "resultado",
    "payload",
  ];

  for (const clave of claves) {
    const valor = data[clave];
    if (Array.isArray(valor)) return valor;
    if (valor && typeof valor === "object") {
      const interno = extraerLista(valor);
      if (interno.length > 0) return interno;
    }
  }

  return [];
}

export async function leerRespuesta(response) {
  if (response.status === 204) return null;

  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
}

export function obtenerMensajeError(data, fallback = "Ocurrió un error") {
  if (!data) return fallback;
  if (typeof data === "string") return data || fallback;

  if (data.detalles && typeof data.detalles === "object") {
    const detalle = Object.values(data.detalles).filter(Boolean).join(". ");
    if (detalle) return detalle;
  }

  if (Array.isArray(data.detalles)) {
    const detalle = data.detalles.filter(Boolean).join(". ");
    if (detalle) return detalle;
  }

  return data.mensaje || data.message || data.error || fallback;
}

export function encodePath(path) {
  return String(path || "")
    .split("/")
    .map((segment) => encodeURIComponent(segment))
    .join("/");
}

export function archivoEsPdf(file) {
  if (!file) return false;
  const nombre = String(file.name || "").toLowerCase();
  return file.type === "application/pdf" || nombre.endsWith(".pdf");
}

export function valor(...opciones) {
  return opciones.find((item) => item !== undefined && item !== null && item !== "") ?? "";
}

export function nombrePersona(item) {
  const directo = valor(
    item?.nombre,
    item?.nombreCompleto,
    item?.nombre_completo,
    item?.personaNombre,
    item?.displayName
  );

  if (directo) return directo;

  const nombres = valor(item?.nombres, item?.primerNombre);
  const apellidos = valor(item?.apellidos, item?.apellido);
  const compuesto = `${nombres} ${apellidos}`.trim();

  return compuesto || "Sin nombre";
}

export function nombreConsulta(item) {
  const id = valor(item?.id, item?.consultaId, item?.idConsulta);
  return valor(
    item?.descripcion,
    item?.descripcionConsulta,
    item?.consulta,
    item?.hechos,
    item?.titulo,
    id ? `Consulta #${id}` : "Consulta"
  );
}

export function idConsulta(item) {
  return valor(item?.id, item?.consultaId, item?.idConsulta);
}

export function ordenarPorIdAsc(items) {
  return [...items].sort((a, b) => {
    const idA = Number(a?.id ?? Number.MAX_SAFE_INTEGER);
    const idB = Number(b?.id ?? Number.MAX_SAFE_INTEGER);
    return idA - idB;
  });
}

export function formatearFecha(value) {
  if (!value) return "No registra";
  const fecha = new Date(value);
  if (Number.isNaN(fecha.getTime())) return String(value);

  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(fecha);
}

export function etiquetaEstado(codigo, nombre) {
  if (nombre) return nombre;

  const estado = normalizarTexto(codigo).replace(/_/g, " ");
  if (!estado) return "Sin estado";

  return estado.charAt(0) + estado.slice(1).toLowerCase();
}

export function badgeEstadoClass(codigo) {
  const estado = normalizarTexto(codigo);

  if (estado.includes("COMPLETO")) {
    return "border-emerald-200 bg-emerald-50 text-emerald-700";
  }

  if (estado === "EN_ESPERA") {
    return "border-amber-200 bg-amber-50 text-amber-700";
  }

  if (estado === "REUNION_PROGRAMADA") {
    return "border-blue-200 bg-blue-50 text-blue-700";
  }

  return "border-slate-200 bg-slate-50 text-slate-700";
}

export function personaResumen(persona) {
  if (!persona) return "No registra";
  const nombre = nombrePersona(persona);
  const documento = valor(persona?.numeroDocumento, persona?.documento, persona?.cedula);
  return documento ? `${nombre} - ${documento}` : nombre;
}

export function obtenerDetalleConsulta(item) {
  const persona = item?.persona || item?.consultante || item?.partePrincipal || {};
  const nombreParte = valor(
    item?.personaNombre,
    item?.consultanteNombre,
    item?.nombrePersona,
    item?.nombre,
    persona?.nombre,
    persona?.nombreCompleto,
    persona?.nombre_completo
  );
  const apellidoParte = valor(item?.personaApellido, item?.apellido, persona?.apellido, persona?.apellidos);
  const documentoParte = valor(
    item?.cedula,
    item?.documento,
    item?.numeroDocumento,
    persona?.documento,
    persona?.numeroDocumento
  );
  const responsable = valor(item?.estudianteNombre, item?.asesorNombre, item?.monitorNombre);

  return {
    id: idConsulta(item),
    titulo: nombreConsulta(item),
    parte: [nombreParte, apellidoParte].filter(Boolean).join(" "),
    documentoParte,
    estado: valor(item?.estado, item?.estadoNombre, item?.estadoConsulta),
    area: valor(item?.areaNombre, item?.area, item?.nombreArea),
    tema: valor(item?.temaNombre, item?.tema, item?.nombreTema),
    tipo: valor(item?.tipoNombre, item?.tipo, item?.nombreTipo),
    responsable,
  };
}

export function labelConsultaBusqueda(item) {
  const detalle = obtenerDetalleConsulta(item);
  return [
    detalle.id,
    detalle.titulo,
    detalle.parte,
    detalle.documentoParte,
    detalle.estado,
    detalle.area,
    detalle.tema,
    detalle.tipo,
    detalle.responsable,
  ]
    .map(normalizarTexto)
    .join(" ");
}
