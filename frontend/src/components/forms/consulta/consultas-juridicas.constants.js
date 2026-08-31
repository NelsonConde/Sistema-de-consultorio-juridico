/** Consultation flow detail.*/
export const ESTADOS_CONSULTA = [
  { value: "ACTIVO", label: "Activo" },
  { value: "EN_PROCESO", label: "En proceso" },
  { value: "PENDIENTE", label: "Pendiente" },
  { value: "URGENTE", label: "Urgente" },
  { value: "CERRADO", label: "Cerrado" },
  { value: "ARCHIVADO", label: "Archivado" },
];

export const VACIOS = {
  fecha: "", descripcion: "", hechos: "", pretensiones: "",
  conceptoJuridico: "", tramite: "", observaciones: "",
  tipoViolencia: "", estado: "", resultado: "",
  personaId: "", sedeId: "", areaId: "", temaId: "",
  tipoId: "", asesorId: "", monitorId: "", estudianteId: "",
  partesIds: [], contrapartesIds: [], version: null,
};
