/** Configuración del formulario de cambio de rol. */
export const PERMISO_GESTIONAR_USUARIOS = "Gestionar usuarios";

export const TIPOS_PERFIL = [
  {
    value: "ADMINISTRATIVO",
    label: "Administrativo",
    endpoint: "administrativo",
    endpointActual: "administrativos",
    rolIdFallback: 1,
    nombresRol: ["Administrador", "Administrativo"],
  },
  {
    value: "ASESOR",
    label: "Asesor",
    endpoint: "asesor",
    endpointActual: "asesores",
    rolIdFallback: 2,
    nombresRol: ["Asesor"],
  },
  {
    value: "ESTUDIANTE",
    label: "Estudiante",
    endpoint: "estudiante",
    endpointActual: "estudiantes",
    rolIdFallback: 3,
    nombresRol: ["Estudiante"],
  },
  {
    value: "MONITOR",
    label: "Monitor",
    endpoint: "monitor",
    endpointActual: "monitores",
    rolIdFallback: 4,
    nombresRol: ["Monitor"],
  },
  {
    value: "CONCILIADOR",
    label: "Conciliador",
    endpoint: "conciliador",
    endpointActual: "conciliadores",
    rolIdFallback: 5,
    nombresRol: ["Conciliador"],
  },
];

export const VALORES_INICIALES = {
  usuarioSistemaId: "",
  destino: "",
  motivo: "",
  nombre: "",
  tipoDocumentoId: "",
  documento: "",
  telefono: "",
  usuario: "",
  codigo: "",
  sedeId: "",
  asesorId: "",
  areaId: "",
  conciliacion: false,
  directora: false,
  tipoConciliador: "",
};
