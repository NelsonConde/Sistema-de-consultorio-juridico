import { PERMISOS } from "@/lib/permission";
import { tieneAlgunPermiso } from "@/lib/authz";
import { PERMISOS_PROCESOS } from "./procesos.constants";

export function puedeAccederProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.ACCEDER_PROCESOS, PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Verifica si el usuario puede ver (listar) procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso VER_PROCESOS o GESTIONAR_PROCESOS
 */
export function puedeVerProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Verifica si el usuario puede crear/editar/eliminar procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso GESTIONAR_PROCESOS
 */
export function puedeGestionarProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Verifica si el usuario puede cargar catálogos (departamentos, órganos, etc).
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso VER_CATALOGOS o GESTIONAR_CATALOGOS
 */
export function puedeCargarCatalogos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CATALOGOS, PERMISOS.GESTIONAR_CATALOGOS]);
}

/**
 * Verifica si el usuario puede cargar consultas para asociar a procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso VER_CONSULTAS o GESTIONAR_CONSULTAS_LEGACY
 */
export function puedeCargarConsultas(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CONSULTAS, PERMISOS_PROCESOS.GESTIONAR_CONSULTAS_LEGACY]);
}

/**
 * Ordena una lista de items, colocando los activos primero y luego los inactivos.
 * Dentro de cada grupo, ordena alfabéticamente por nombre o descripción.
 * 
 * @param {Array<Object>} lista - Lista a ordenar
 * @returns {Array<Object>} Nueva lista ordenada
 */
