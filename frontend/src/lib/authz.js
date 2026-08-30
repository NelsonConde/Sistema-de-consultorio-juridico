/**
 * Permission and authorization handling.
 *
 * Permission and authorization handling.
 * Implementation detail.
 *
 * @module lib/authz
 */

/**
 * Implementation detail.
 * Implementation detail.
 *
 * @param {unknown} value - Implementation detail.
 * @returns {string} Result value.
 */
export function normalizar(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

/**
 * Permission and authorization handling.
 * Implementation detail.
 *
 * @param {string|object} permiso - Parameter description.
 * @returns {string} Result value.
 */
export function nombrePermiso(permiso) {
  if (typeof permiso === "string") return permiso;

  return (
    permiso?.nombre ||
    permiso?.nombrePermiso ||
    permiso?.descripcion ||
    permiso?.permiso ||
    ""
  );
}

/**
 * Permission and authorization handling.
 *
 * @param {object|null} user - Authenticated user object.
 * @returns {Array} Result value.
 */
export function obtenerPermisos(user) {
  return Array.isArray(user?.permisos) ? user.permisos : [];
}

/**
 * Permission and authorization handling.
 *
 * @param {object|null} user - Authenticated user object.
 * @returns {string[]} Result value.
 */
export function obtenerNombresPermisos(user) {
  return obtenerPermisos(user)
    .map(nombrePermiso)
    .filter(Boolean);
}

/**
 * Permission and authorization handling.
 * Implementation detail.
 *
 * @param {object|null} user - Authenticated user object.
 * @param {string} permiso - Parameter description.
 * @returns {boolean} Result value.
 */
export function tienePermiso(user, permiso) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);
  return permisos.includes(normalizar(permiso));
}

/**
 * Permission and authorization handling.
 *
 * @param {object|null} user - Authenticated user object.
 * @param {string[]} [permisosRequeridos=[]] - Required permission names.
 * @returns {boolean} Result value.
 */
export function tieneAlgunPermiso(user, permisosRequeridos = []) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);

  return permisosRequeridos
    .filter(Boolean)
    .some((permiso) => permisos.includes(normalizar(permiso)));
}

/**
 * Permission and authorization handling.
 *
 * @param {object|null} user - Authenticated user object.
 * @param {string[]} [permisosRequeridos=[]] - Required permission names.
 * @returns {boolean} Result value.
 */
export function tieneTodosLosPermisos(user, permisosRequeridos = []) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);

  return permisosRequeridos
    .filter(Boolean)
    .every((permiso) => permisos.includes(normalizar(permiso)));
}

/**
 * User flow detail.
 * Implementation detail.
 *
 * @param {object|null} user - Authenticated user object.
 * @param {string} perfil - Parameter description.
 * @returns {boolean} Result value.
 */
export function tienePerfil(user, perfil) {
  return normalizar(user?.tipoPerfil) === normalizar(perfil);
}

/**
 * Role handling.
 *
 * @param {object|null} user - Authenticated user object.
 * @param {string} rol - Role name to compare.
 * @returns {boolean} Result value.
 */
export function tieneRol(user, rol) {
  return normalizar(user?.rolNombre || user?.rol?.nombre) === normalizar(rol);
}

/**
 * @param {object|null} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function esAdministrativo(user) {
  return tienePerfil(user, "ADMINISTRATIVO");
}

/**
 * @param {object|null} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function esAsesor(user) {
  return tienePerfil(user, "ASESOR");
}

/**
 * @param {object|null} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function esEstudiante(user) {
  return tienePerfil(user, "ESTUDIANTE");
}

/**
 * @param {object|null} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function esMonitor(user) {
  return tienePerfil(user, "MONITOR");
}

/**
 * @param {object|null} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function esConciliador(user) {
  return tienePerfil(user, "CONCILIADOR");
}
