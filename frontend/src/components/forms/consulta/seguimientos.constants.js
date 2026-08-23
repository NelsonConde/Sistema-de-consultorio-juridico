/**
 * Constantes y valores iniciales del módulo de seguimientos.
 *
 * Mantener estos valores fuera del componente evita mezclar configuración
 * estática con estado y renderizado.
 */

export const FORM_TAREA_INICIAL = {
  categoriaId: "",
  descripcion: "",
  fechaEntrega: "",
  diasNotificacion: "",
  notificarPartes: false,
  alertaDisciplinaria: false,
  notificarEstudiante: true,
}

export const FORM_RESPUESTA_INICIAL = {
  contenido: "",
  archivos: [],
}

export const FORM_DECISION_INICIAL = {
  estado: "APROBADA",
  observacionRevision: "",
}

export const FORM_ESTADO_SEGUIMIENTO_INICIAL = {
  estado: "COMPLETADO",
}

export const ESTADOS_SEGUIMIENTO = [
  { value: "PENDIENTE", label: "Pendiente" },
  { value: "COMPLETADO", label: "Completado" },
  { value: "CANCELADO", label: "Cancelado" },
]
