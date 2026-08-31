/**
 * Authorization utilities for the legal case management system.
 *
 * Provides helpers to verify permissions and profile information for the authenticated user
 * from the object returned by `/api/auth/me`.
 *
 * @module lib/authz
 */

/**
 * Normalizes a string for case-insensitive and accent-insensitive comparisons.
 * Converts to uppercase and removes diacritics.
 *
 * @param {unknown} value - Value to normalize.
 * @returns {string} Normalized uppercase string without diacritics.
 */
export function normalizar(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

/**
 * Extracts the readable permission name from either a direct string
 * or an object containing one of the backend's standard fields.
 *
 * @param {string|object} permiso - Permission as a string or DTO object.
 * @returns {string} Permission name, or an empty string when it cannot be extracted.
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
 * Returns the user's permission array.
 *
 * @param {object|null} user - User object returned by `/api/auth/me`.
 * @returns {Array} User permission array, or an empty array when none are available.
 */
export function obtenerPermisos(user) {
  return Array.isArray(user?.permisos) ? user.permisos : [];
}

/**
 * Returns all user permission names as strings.
 *
 * @param {object|null} user - User object.
 * @returns {string[]} Array of permission names.
 */
export function obtenerNombresPermisos(user) {
  return obtenerPermisos(user)
    .map(nombrePermiso)
    .filter(Boolean);
}

/**
 * Checks whether the user has a specific permission.
 * Comparison is case-insensitive and accent-insensitive.
 *
 * @param {object|null} user - User object.
 * @param {string} permiso - Permission name to verify (for example, `"Ver consultas"`).
 * @returns {boolean} `true` when the user has the permission.
 */
export function tienePermiso(user, permiso) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);
  return permisos.includes(normalizar(permiso));
}

/**
 * Checks whether the user has at least one of the specified permissions.
 *
 * @param {object|null} user - User object.
 * @param {string[]} [permisosRequeridos=[]] - Permission names to verify.
 * @returns {boolean} `true` when the user has at least one of the permissions.
 */
export function tieneAlgunPermiso(user, permisosRequeridos = []) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);

  return permisosRequeridos
    .filter(Boolean)
    .some((permiso) => permisos.includes(normalizar(permiso)));
}

/**
 * Checks whether the user has all specified permissions.
 *
 * @param {object|null} user - User object.
 * @param {string[]} [permisosRequeridos=[]] - Permission names to verify.
 * @returns {boolean} `true` when the user has all permissions.
 */
export function tieneTodosLosPermisos(user, permisosRequeridos = []) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);

  return permisosRequeridos
    .filter(Boolean)
    .every((permiso) => permisos.includes(normalizar(permiso)));
}

/**
 * Checks whether the user's profile type matches the specified profile.
 * Comparison is case-insensitive and accent-insensitive.
 *
 * @param {object|null} user - User object.
 * @param {string} perfil - Profile type to verify (for example, `"ESTUDIANTE"`).
 * @returns {boolean} `true` when the user's profile type matches.
 */
export function tienePerfil(user, perfil) {
  return normalizar(user?.tipoPerfil) === normalizar(perfil);
}

/**
 * @param {object|null} user - User object.
 * @returns {boolean} `true` when the user has an administrative profile.
 */
export function esAdministrativo(user) {
  return tienePerfil(user, "ADMINISTRATIVO");
}

/**
 * @param {object|null} user - User object.
 * @returns {boolean} `true` when the user has an advisor profile.
 */
export function esAsesor(user) {
  return tienePerfil(user, "ASESOR");
}

/**
 * @param {object|null} user - User object.
 * @returns {boolean} `true` when the user has a student profile.
 */
export function esEstudiante(user) {
  return tienePerfil(user, "ESTUDIANTE");
}

/**
 * @param {object|null} user - User object.
 * @returns {boolean} `true` when the user has a monitor profile.
 */
export function esMonitor(user) {
  return tienePerfil(user, "MONITOR");
}

/**
 * @param {object|null} user - User object.
 * @returns {boolean} `true` when the user has a conciliator profile.
 */
export function esConciliador(user) {
  return tienePerfil(user, "CONCILIADOR");
}
