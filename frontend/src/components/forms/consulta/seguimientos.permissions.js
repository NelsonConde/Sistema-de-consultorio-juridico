/**
 * Permission rules for the follow-up module.
 *
 * This file contains only UI authorization decisions.
 * Actual authorization remains the backend's responsibility.
 */

import { tieneAlgunPermiso, tienePerfil as tienePerfilAuthz, tienePermiso } from "@/lib/authz"
import { PERMISOS } from "@/lib/permission"

const PERMISOS_LEGACY = {
  GESTIONAR_CONSULTAS: "Gestionar consultas",
  GESTIONAR_SEGUIMIENTOS: "Gestionar seguimientos",
}

export function tienePerfil(user, perfil) {
  return tienePerfilAuthz(user, perfil)
}

export function esEstudiante(user) {
  return tienePerfil(user, "ESTUDIANTE")
}

export function puedeAccederTareasUsuario(user) {
  return (
    tienePermiso(user, PERMISOS.ACCEDER_TAREAS) &&
    tieneAlgunPermiso(user, [
      PERMISOS.VER_SEGUIMIENTOS,
      PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
    ])
  )
}

export function puedeVerConsultasUsuario(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.VER_CONSULTAS,
    PERMISOS_LEGACY.GESTIONAR_CONSULTAS,
  ])
}

export function puedeCargarCategoriasUsuario(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.VER_SEGUIMIENTOS,
    PERMISOS.CREAR_SEGUIMIENTOS,
    PERMISOS.EDITAR_SEGUIMIENTOS,
    PERMISOS.GESTIONAR_CATEGORIAS_SEGUIMIENTO,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

export function puedeCrearTarea(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.CREAR_SEGUIMIENTOS,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

export function puedeEditarTarea(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.EDITAR_SEGUIMIENTOS,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

export function puedeEliminarTarea(user) {
  return tieneAlgunPermiso(user, [
    PERMISOS.ELIMINAR_SEGUIMIENTOS,
    PERMISOS_LEGACY.GESTIONAR_SEGUIMIENTOS,
  ])
}

export function puedeResponderTarea(user) {
  return tienePermiso(user, PERMISOS.RESPONDER_SEGUIMIENTOS)
}

export function puedeRevisarRespuestas(user) {
  return tienePermiso(user, PERMISOS.APROBAR_RESPUESTAS_SEGUIMIENTO)
}

export function puedeVerAlertasDisciplinarias(user) {
  return tienePermiso(user, PERMISOS.VER_ALERTAS_DISCIPLINARIAS)
}

export function accionPermitidaPorRegistro(item, accion, permisoGlobal) {
  const acciones = item?.accionesPermitidas

  if (acciones && typeof acciones[accion] === "boolean") {
    return acciones[accion]
  }

  return permisoGlobal
}
