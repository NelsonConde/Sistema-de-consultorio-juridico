/**
 * Utilidades para ordenamiento y paginación de listas en el frontend.
 *
 * Usadas por tablas y listados de consultas, seguimientos, personas y otros recursos
 * para mantener consistencia en la presentación de datos paginados.
 *
 * @module lib/list-utils
 */

/** Opciones de tamaño de página disponibles en los selectores de paginación. */
export const DEFAULT_PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

/**
 * Obtiene el ID numérico de un item, buscando en los campos estándar del backend.
 * Devuelve `fallback` si no se encuentra un ID numérico válido.
 *
 * @param {object} item - El item del que obtener el ID.
 * @param {number} [fallback=Number.MAX_SAFE_INTEGER] - Valor por defecto si no hay ID.
 * @returns {number} El ID numérico del item.
 */
export function getNumericId(item, fallback = Number.MAX_SAFE_INTEGER) {
  const rawId = item?.id ?? item?.consultaId ?? item?.seguimientoId ?? item?.idSeguimiento;
  const numericId = Number(rawId);

  return Number.isFinite(numericId) ? numericId : fallback;
}

/**
 * Ordena un array de items por ID de forma ascendente.
 * En caso de empate por ID, ordena alfabéticamente por nombre/username/descripción.
 *
 * @param {object[]} items - Array de items a ordenar.
 * @param {function(object): number} [getId=getNumericId] - Función para obtener el ID de un item.
 * @returns {object[]} Nuevo array ordenado (el original no se modifica).
 */
export function sortByIdAsc(items, getId = getNumericId) {
  if (!Array.isArray(items)) return [];

  return [...items].sort((a, b) => {
    const idA = getId(a);
    const idB = getId(b);

    if (idA !== idB) return idA - idB;

    return String(a?.nombre || a?.username || a?.descripcion || "").localeCompare(
      String(b?.nombre || b?.username || b?.descripcion || ""),
      "es",
      { sensitivity: "base" }
    );
  });
}

/**
 * Calcula el número total de páginas para una lista paginada.
 *
 * @param {number} totalItems - Cantidad total de items en la lista completa.
 * @param {number} pageSize - Cantidad de items por página.
 * @returns {number} Total de páginas, mínimo 1.
 */
export function getTotalPages(totalItems, pageSize) {
  const size = Number(pageSize) > 0 ? Number(pageSize) : DEFAULT_PAGE_SIZE_OPTIONS[1];

  return Math.max(1, Math.ceil(Number(totalItems || 0) / size));
}

/**
 * Extrae el subconjunto de items correspondiente a una página específica.
 *
 * @param {object[]} items - Array completo de items.
 * @param {number} currentPage - Número de página actual (base 1).
 * @param {number} pageSize - Cantidad de items por página.
 * @returns {object[]} Subarray con los items de la página solicitada.
 */
export function paginateItems(items, currentPage, pageSize) {
  const page = Math.max(1, Number(currentPage) || 1);
  const size = Number(pageSize) > 0 ? Number(pageSize) : DEFAULT_PAGE_SIZE_OPTIONS[1];
  const start = (page - 1) * size;

  return Array.isArray(items) ? items.slice(start, start + size) : [];
}
