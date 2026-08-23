import { PERMISOS } from "@/lib/permission";

export const PERMISOS_PROCESOS = {
  VER_PROCESOS: PERMISOS.VER_PROCESOS || "Ver procesos",
  GESTIONAR_PROCESOS: PERMISOS.GESTIONAR_PROCESOS || "Gestionar procesos",
  GESTIONAR_CONSULTAS_LEGACY: "Gestionar consultas",
};

export const FORM_INICIAL = {
  id: null,
  numeroRadicado: "",
  departamentoId: "",
  consultaId: "",
  organoControlId: "",
  especialidadId: "",
  estado: "",
};

export const ESTADOS_PROCESO = [
  { value: "PENDIENTE", label: "Pendiente" },
  { value: "SENTENCIA_FAVORABLE", label: "Sentencia favorable" },
  { value: "SENTENCIA_DESFAVORABLE", label: "Sentencia desfavorable" },
  { value: "DESISTIMIENTO", label: "Desistimiento" },
  { value: "RECHAZO", label: "Rechazo" },
  { value: "PRESCRIPCION", label: "Prescripción" },
];

/**
 * Convierte un estado de proceso a su etiqueta en español.
 * 
 * @param {string} estado - Código de estado (ej: "PENDIENTE", "SENTENCIA_FAVORABLE")
 * @returns {string} Etiqueta legible del estado
 */
