import { PERMISOS } from "@/lib/permission";
import { tieneAlgunPermiso } from "@/lib/authz";
import { PERMISOS_PROCESOS } from "./procesos.constants";

export function puedeAccederProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.ACCEDER_PROCESOS, PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * List and table handling.
 * @param {Object} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function puedeVerProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Process workflow detail.
 * @param {Object} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function puedeGestionarProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Data loading behavior.
 * @param {Object} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function puedeCargarCatalogos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CATALOGOS, PERMISOS.GESTIONAR_CATALOGOS]);
}

/**
 * Data loading behavior.
 * @param {Object} user - Authenticated user object.
 * @returns {boolean} Result value.
 */
export function puedeCargarConsultas(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CONSULTAS, PERMISOS_PROCESOS.GESTIONAR_CONSULTAS_LEGACY]);
}

/**
 * List and table handling.
 * Implementation detail.
 * 
 * @param {Array<Object>} lista - Parameter description.
 * @returns {Array<Object>} Result value.
 */
