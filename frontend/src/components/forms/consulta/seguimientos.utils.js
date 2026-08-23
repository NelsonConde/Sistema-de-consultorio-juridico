/**
 * Funciones puras y normalizadores del módulo de seguimientos.
 */

import { normalizar } from "@/lib/authz"
import { ESTADOS_SEGUIMIENTO } from "./seguimientos.constants"

export function extraerLista(data) {
  if (Array.isArray(data)) return data
  if (!data || typeof data !== "object") return []

  const claves = [
    "content",
    "data",
    "items",
    "rows",
    "consultas",
    "seguimientos",
    "tareas",
    "categorias",
    "respuestas",
    "pendientes",
    "resultado",
    "payload",
  ]

  for (const clave of claves) {
    const valor = data[clave]

    if (Array.isArray(valor)) return valor

    if (valor && typeof valor === "object") {
      const interno = extraerLista(valor)
      if (interno.length > 0) return interno
    }
  }

  return []
}

export function labelConsulta(consulta) {
  return [
    `#${consulta.id || consulta.consultaId}`,
    consulta.consulta || consulta.descripcion || consulta.hechos || consulta.asunto,
    consulta.nombre || consulta.apellido
      ? `${consulta.nombre || ""} ${consulta.apellido || ""}`.trim()
      : "",
    consulta.cedula || consulta.documento,
  ]
    .filter(Boolean)
    .join(" - ")
}

export function obtenerTextoTarea(item) {
  return (
    item.descripcion ||
    item.observacion ||
    item.detalle ||
    item.comentario ||
    "Sin descripción"
  )
}

export function obtenerCategoriaTarea(item) {
  return (
    item.categoriaNombre ||
    item.categoriaSeguimientoNombre ||
    item.categoria?.nombre ||
    item.categoriaSeguimiento?.nombre ||
    item.categoria ||
    "Sin categoría"
  )
}

export function obtenerCategoriaIdTarea(item) {
  return (
    item.categoriaSeguimientoId ||
    item.categoriaId ||
    item.categoria?.id ||
    item.categoriaSeguimiento?.id ||
    ""
  )
}

export function obtenerAutorTarea(item) {
  return (
    item.autorNombre ||
    item.autorUsername ||
    item.autor ||
    item.username ||
    "Sin autor"
  )
}

export function obtenerFechaTarea(item) {
  return (
    item.fechaCreacion ||
    item.fechaRegistro ||
    item.createdAt ||
    item.fecha ||
    ""
  )
}

export function obtenerIdTarea(item) {
  return item?.id || item?.seguimientoId
}

export function ordenarPorFechaDesc(lista) {
  return [...lista].sort((a, b) => {
    const fechaA = new Date(a.fechaActualizacion || a.fechaCreacion || a.fechaDecision || 0)
    const fechaB = new Date(b.fechaActualizacion || b.fechaCreacion || b.fechaDecision || 0)
    return fechaB.getTime() - fechaA.getTime()
  })
}

export function ultimaRespuesta(lista = []) {
  const respuestas = ordenarPorFechaDesc(lista)
  return respuestas[0] || null
}

export function getAccionRespuesta(ultima, puedeResponder) {
  if (!puedeResponder) return "NINGUNA"
  if (!ultima) return "RESPONDER"

  switch (normalizar(ultima.estado)) {
    case "PENDIENTE":
      return "EDITAR"
    case "RECHAZADA":
      return "RESPONDER_NUEVAMENTE"
    case "APROBADA":
      return "SOLO_LECTURA"
    default:
      return "NINGUNA"
  }
}

export function textoAccionRespuesta(accion) {
  switch (accion) {
    case "RESPONDER":
      return "Responder"
    case "EDITAR":
      return "Editar respuesta"
    case "RESPONDER_NUEVAMENTE":
      return "Responder nuevamente"
    default:
      return "Ver respuesta"
  }
}

export function estadoBadgeClass(estado) {
  switch (normalizar(estado)) {
    case "APROBADA":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700"
    case "RECHAZADA":
      return "border-destructive/30 bg-destructive/10 text-destructive"
    case "PENDIENTE":
      return "border-yellow-500/30 bg-yellow-500/10 text-yellow-700"
    default:
      return "border-muted bg-muted text-muted-foreground"
  }
}

export function estadoSeguimientoBadgeClass(estado) {
  switch (normalizar(estado)) {
    case "COMPLETADO":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700"
    case "CANCELADO":
      return "border-destructive/30 bg-destructive/10 text-destructive"
    case "PENDIENTE":
      return "border-yellow-500/30 bg-yellow-500/10 text-yellow-700"
    default:
      return "border-muted bg-muted text-muted-foreground"
  }
}

export function textoEstadoSeguimiento(estado) {
  const encontrado = ESTADOS_SEGUIMIENTO.find((item) => item.value === normalizar(estado))
  return encontrado?.label || estado || "Sin estado"
}

export function consultaPermiteOperaciones(consulta) {
  const estado = normalizar(consulta?.estado)
  return estado !== "CERRADO" && estado !== "ARCHIVADO"
}

export function seguimientoPermiteOperaciones(tarea) {
  return normalizar(tarea?.estado || "PENDIENTE") === "PENDIENTE"
}

export function seguimientoEstaVencido(tarea) {
  if (!tarea?.fechaEntrega || !seguimientoPermiteOperaciones(tarea)) return false

  const hoy = new Date()
  hoy.setHours(0, 0, 0, 0)

  const fechaEntrega = new Date(`${tarea.fechaEntrega}T00:00:00`)
  return fechaEntrega.getTime() < hoy.getTime()
}

export function pathRespuesta(seguimientoId, respuestaId) {
  return `tareas-${seguimientoId}-respuestas-${respuestaId}`
}

export async function leerRespuesta(response) {
  const text = await response.text()

  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return { mensaje: text }
  }
}

export function mensajeError(data, defecto) {
  return data?.mensaje || data?.message || data?.error || defecto
}
