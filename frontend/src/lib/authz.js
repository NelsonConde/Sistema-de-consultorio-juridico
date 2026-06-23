/**
 * Utilidades de autorización para el sistema de casos jurídicos.
 *
 * Provee funciones para verificar permisos y roles del usuario autenticado
 * a partir del objeto devuelto por `/api/auth/me`.
 *
 * @module lib/authz
 */

/**
 * Normaliza un string para comparaciones insensibles a mayúsculas y tildes.
 * Convierte a mayúsculas y elimina diacríticos.
 *
 * @param {unknown} value - Valor a normalizar.
 * @returns {string} String normalizado en mayúsculas sin tildes.
 */
export function normalizar(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

/**
 * Extrae el nombre legible de un permiso, ya sea un string directo
 * o un objeto con alguno de los campos estándar del backend.
 *
 * @param {string|object} permiso - El permiso como string o como objeto DTO.
 * @returns {string} El nombre del permiso, o string vacío si no se puede extraer.
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
 * Obtiene el array de permisos del usuario.
 *
 * @param {object|null} user - Objeto de usuario devuelto por `/api/auth/me`.
 * @returns {Array} Array de permisos del usuario, o vacío si no hay ninguno.
 */
export function obtenerPermisos(user) {
  return Array.isArray(user?.permisos) ? user.permisos : [];
}

/**
 * Obtiene los nombres de todos los permisos del usuario como strings.
 *
 * @param {object|null} user - Objeto de usuario.
 * @returns {string[]} Array de nombres de permisos.
 */
export function obtenerNombresPermisos(user) {
  return obtenerPermisos(user)
    .map(nombrePermiso)
    .filter(Boolean);
}

/**
 * Verifica si el usuario tiene un permiso específico.
 * La comparación es insensible a mayúsculas y tildes.
 *
 * @param {object|null} user - Objeto de usuario.
 * @param {string} permiso - Nombre del permiso a verificar (ej. `"Ver consultas"`).
 * @returns {boolean} `true` si el usuario tiene el permiso.
 */
export function tienePermiso(user, permiso) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);
  return permisos.includes(normalizar(permiso));
}

/**
 * Verifica si el usuario tiene al menos uno de los permisos indicados.
 *
 * @param {object|null} user - Objeto de usuario.
 * @param {string[]} [permisosRequeridos=[]] - Lista de permisos a verificar.
 * @returns {boolean} `true` si el usuario tiene alguno de los permisos.
 */
export function tieneAlgunPermiso(user, permisosRequeridos = []) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);

  return permisosRequeridos
    .filter(Boolean)
    .some((permiso) => permisos.includes(normalizar(permiso)));
}

/**
 * Verifica si el usuario tiene todos los permisos indicados.
 *
 * @param {object|null} user - Objeto de usuario.
 * @param {string[]} [permisosRequeridos=[]] - Lista de permisos a verificar.
 * @returns {boolean} `true` si el usuario tiene todos los permisos.
 */
export function tieneTodosLosPermisos(user, permisosRequeridos = []) {
  const permisos = obtenerNombresPermisos(user).map(normalizar);

  return permisosRequeridos
    .filter(Boolean)
    .every((permiso) => permisos.includes(normalizar(permiso)));
}

/**
 * Verifica si el tipo de perfil del usuario coincide con el indicado.
 * La comparación es insensible a mayúsculas y tildes.
 *
 * @param {object|null} user - Objeto de usuario.
 * @param {string} perfil - Nombre del perfil a verificar (ej. `"ESTUDIANTE"`).
 * @returns {boolean} `true` si el perfil del usuario coincide.
 */
export function tienePerfil(user, perfil) {
  return normalizar(user?.tipoPerfil) === normalizar(perfil);
}

/**
 * Verifica si el nombre de rol del usuario coincide con el indicado.
 *
 * @param {object|null} user - Objeto de usuario.
 * @param {string} rol - Nombre del rol a verificar.
 * @returns {boolean} `true` si el rol coincide.
 */
export function tieneRol(user, rol) {
  return normalizar(user?.rolNombre || user?.rol?.nombre) === normalizar(rol);
}

/**
 * @param {object|null} user - Objeto de usuario.
 * @returns {boolean} `true` si el usuario es administrativo.
 */
export function esAdministrativo(user) {
  return tienePerfil(user, "ADMINISTRATIVO");
}

/**
 * @param {object|null} user - Objeto de usuario.
 * @returns {boolean} `true` si el usuario es asesor.
 */
export function esAsesor(user) {
  return tienePerfil(user, "ASESOR");
}

/**
 * @param {object|null} user - Objeto de usuario.
 * @returns {boolean} `true` si el usuario es estudiante.
 */
export function esEstudiante(user) {
  return tienePerfil(user, "ESTUDIANTE");
}

/**
 * @param {object|null} user - Objeto de usuario.
 * @returns {boolean} `true` si el usuario es monitor.
 */
export function esMonitor(user) {
  return tienePerfil(user, "MONITOR");
}

/**
 * @param {object|null} user - Objeto de usuario.
 * @returns {boolean} `true` si el usuario es conciliador.
 */
export function esConciliador(user) {
  return tienePerfil(user, "CONCILIADOR");
}
