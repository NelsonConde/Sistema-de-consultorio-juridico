import { ESTADOS_PROCESO } from "./procesos.constants";

export function labelEstadoProceso(estado) {
  return ESTADOS_PROCESO.find((item) => item.value === estado)?.label || estado || "Sin estado";
}

/**
 * Determina si un estado de proceso es final (no puede cambiar).
 * 
 * @param {string} estado - Estado a verificar
 * @returns {boolean} True si el estado es final, false si es PENDIENTE
 */
export function estadoProcesoEsFinal(estado) {
  return estado && estado !== "PENDIENTE";
}

/**
 * Extrae un array de una estructura de datos anidada, buscando en claves comunes.
 * Útil para normalizar diferentes formatos de respuesta del backend.
 * 
 * @param {*} data - Datos a procesar (array u objeto)
 * @returns {Array} Array encontrado o array vacío
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
 * Crea un Map indexado por id numérico desde una lista de items.
 * Facilita búsquedas rápidas por id.
 * 
 * @param {Array<Object>} lista - Lista de items con propiedad 'id'
 * @returns {Map<number, Object>} Mapa id => item
 */
export function crearMapa(lista) {
  return new Map(lista.map((item) => [Number(item.id), item]));
}

/**
 * Obtiene el nombre de un item del catálogo usando un Map.
 * 
 * @param {Map<number, Object>} mapa - Mapa de items indexado por id
 * @param {number} id - ID a buscar
 * @param {string} [fallback] - Valor por defecto si no se encuentra
 * @returns {string} Nombre/descripción del item o fallback o "#id" si no existe
 */
export function nombreCatalogo(mapa, id, fallback) {
  const item = mapa.get(Number(id));
  return item?.nombre || item?.descripcion || fallback || (id ? `#${id}` : "Sin asignar");
}

/**
 * Genera una etiqueta legible para un item de catálogo.
 * Agrega "(Inactivo)" al nombre si el item no está activo.
 * 
 * @param {Object} item - Item de catálogo con propiedades {nombre, descripcion, codigo, activo, id}
 * @returns {string} Etiqueta formateada
 */
export function labelCatalogo(item) {
  const nombre = item.nombre || item.descripcion || item.codigo || `#${item.id}`;
  return item.activo === false ? `${nombre} (Inactivo)` : nombre;
}

/**
 * Genera una etiqueta detallada para una consulta.
 * Incluye ID, descripción, nombre de la persona y documento.
 * 
 * @param {Object} consulta - Objeto de consulta
 * @returns {string} Etiqueta en formato: "#id - descripción - persona - documento"
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
 * Normaliza un formulario de proceso antes de enviarlo al backend.
 * Convierte IDs a números y valores vacíos a null.
 * 
 * @param {Object} form - Objeto de formulario
 * @returns {Object} Objeto normalizado listo para enviar al API
 */
export function normalizarPayload(form) {
  const numeroRadicado = String(form.numeroRadicado || "").trim();
  return {
    numeroRadicado: numeroRadicado || null,
    departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
    consultaId: form.consultaId ? Number(form.consultaId) : null,
    organoControlId: form.organoControlId ? Number(form.organoControlId) : null,
    especialidadId: form.especialidadId ? Number(form.especialidadId) : null,
  };
}

/**
 * Convierte un objeto de proceso a formato de formulario.
 * Asegura que todos los campos tengan valor por defecto (strings vacíos).
 * 
 * @param {Object} proceso - Objeto de proceso del backend
 * @returns {Object} Objeto en formato de formulario
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
  };
}

// ─── Modal de búsqueda de consulta ───────────────────────────────────────────
