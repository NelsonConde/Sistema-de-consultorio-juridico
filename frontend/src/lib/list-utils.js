/**
 * Handles numeric identifiers consistently for list ordering.
 *
 * List and table handling.
 * Pagination handling.
 *
 * @module lib/list-utils
 */

/** Pagination handling.*/
export const DEFAULT_PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

/**
 * Data loading behavior.
 * Implementation detail.
 *
 * @param {object} item - Item to process.
 * @param {number} [fallback=Number.MAX_SAFE_INTEGER] - Fallback value.
 * @returns {number} Result value.
 */
export function getNumericId(item, fallback = Number.MAX_SAFE_INTEGER) {
  const rawId = item?.id ?? item?.consultaId ?? item?.seguimientoId ?? item?.idSeguimiento;
  const numericId = Number(rawId);

  return Number.isFinite(numericId) ? numericId : fallback;
}

/**
 * Handles numeric identifiers consistently for list ordering.
 * Handles numeric identifiers consistently for list ordering.
 *
 * @param {object[]} items - Item to process.
 * @param {function(object): number} [getId=getNumericId] - Parameter description.
 * @returns {object[]} Result value.
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
 * Handles list pagination consistently.
 *
 * @param {number} totalItems - Item to process.
 * @param {number} pageSize - Parameter description.
 * @returns {number} Result value.
 */
export function getTotalPages(totalItems, pageSize) {
  const size = Number(pageSize) > 0 ? Number(pageSize) : DEFAULT_PAGE_SIZE_OPTIONS[1];

  return Math.max(1, Math.ceil(Number(totalItems || 0) / size));
}

/**
 * Implementation detail.
 *
 * @param {object[]} items - Item to process.
 * @param {number} currentPage - Parameter description.
 * @param {number} pageSize - Parameter description.
 * @returns {object[]} Result value.
 */
export function paginateItems(items, currentPage, pageSize) {
  const page = Math.max(1, Number(currentPage) || 1);
  const size = Number(pageSize) > 0 ? Number(pageSize) : DEFAULT_PAGE_SIZE_OPTIONS[1];
  const start = (page - 1) * size;

  return Array.isArray(items) ? items.slice(start, start + size) : [];
}
