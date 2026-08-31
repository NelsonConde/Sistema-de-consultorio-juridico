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
 * State handling.
 * 
 * @param {string} estado - Status value.
 * @returns {string} Result value.
 */
