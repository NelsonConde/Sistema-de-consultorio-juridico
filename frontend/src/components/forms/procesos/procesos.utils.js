import { ESTADOS_PROCESO } from "./procesos.constants";

export function labelEstadoProceso(estado) {
  return ESTADOS_PROCESO.find((item) => item.value === estado)?.label || estado || "Sin estado";
}

/**
 * State handling.
 * 
 * State handling.
 * @returns {boolean} Result value.
 */
export function estadoProcesoEsFinal(estado) {
  return estado && estado !== "PENDIENTE";
}

/**
 * Implementation detail.
 * Implementation detail.
 * 
 * @param {*} data - Parameter description.
 * @returns {Array} Result value.
 */
export function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];

  const claves = [
    "content", "data", "items", "rows", "departamentos", "organos",
    "organosControl", "especialidades", "consultas", "procesos", "resultado", "payload",
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

export function ordenarActivosPrimero(lista) {
  return [...lista].sort((a, b) => {
    const activoA = a.activo === false ? 1 : 0;
    const activoB = b.activo === false ? 1 : 0;
    if (activoA !== activoB) return activoA - activoB;
    return String(a.nombre || a.descripcion || "").localeCompare(String(b.nombre || b.descripcion || ""), "es");
  });
}

/**
 * List and table handling.
 * Search behavior.
 * 
 * @param {Array<Object>} lista - Parameter description.
 * @returns {Map<number, Object>} Result value.
 */
export function crearMapa(lista) {
  return new Map(lista.map((item) => [Number(item.id), item]));
}

/**
 * Data loading behavior.
 * 
 * @param {Map<number, Object>} mapa - Items indexed by ID.
 * @param {number} id - ID to look up.
 * @param {string} [fallback] - Fallback value.
 * @returns {string} Result value.
 */
export function nombreCatalogo(mapa, id, fallback) {
  const item = mapa.get(Number(id));
  return item?.nombre || item?.descripcion || fallback || (id ? `#${id}` : "Sin asignar");
}

/**
 * Implementation detail.
 * Implementation detail.
 * 
 * @param {Object} item - Item to process.
 * @returns {string} Result value.
 */
export function labelCatalogo(item) {
  const nombre = item.nombre || item.descripcion || item.codigo || `#${item.id}`;
  return item.activo === false ? `${nombre} (Inactivo)` : nombre;
}

/**
 * Consultation flow detail.
 * People workflow detail.
 * 
 * @param {Object} query - Parameter description.
 * @returns {string} Result value.
 */
export function labelConsulta(consulta) {
  const persona = consulta.persona || consulta.consultante || {};
  const nombrePersona = [
    consulta.nombre || consulta.personaNombre || persona.nombre,
    consulta.apellido || consulta.personaApellido || persona.apellido,
  ].filter(Boolean).join(" ");

  return [
    `#${consulta.id || consulta.consultaId}`,
    consulta.consulta || consulta.descripcion || consulta.hechos || consulta.asunto,
    nombrePersona,
    consulta.cedula || consulta.documento || persona.documento,
  ].filter(Boolean).join(" - ");
}

/**
 * Form handling.
 * Implementation detail.
 * 
 * @param {Object} form - Parameter description.
 * @returns {Object} Result value.
 */
export function normalizarPayload(form, { includeVersion = false } = {}) {
  const numeroRadicado = String(form.numeroRadicado || "").trim();
  return {
    numeroRadicado: numeroRadicado || null,
    ...(includeVersion ? { version: form.version } : {}),
    departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
    consultaId: form.consultaId ? Number(form.consultaId) : null,
    organoControlId: form.organoControlId ? Number(form.organoControlId) : null,
    especialidadId: form.especialidadId ? Number(form.especialidadId) : null,
  };
}

/**
 * Form handling.
 * Implementation detail.
 * 
 * @param {Object} proceso - Process data.
 * @returns {Object} Result value.
 */
export function procesoAForm(proceso) {
  return {
    id: proceso.id,
    numeroRadicado: proceso.numeroRadicado || "",
    departamentoId: proceso.departamentoId || "",
    consultaId: proceso.consultaId || "",
    organoControlId: proceso.organoControlId || "",
    especialidadId: proceso.especialidadId || "",
    estado: proceso.estado || "PENDIENTE",
    version: proceso.version ?? null,
  };
}

// Search behavior.
