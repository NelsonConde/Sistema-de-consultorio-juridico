/** Configuration for the user role/profile change form. */
export const PERMISO_GESTIONAR_USUARIOS = "Gestionar usuarios";

export const TIPOS_PERFIL = [
  {
    value: "ADMINISTRATIVO",
    label: "Administrativo",
    endpoint: "administrativo",
    endpointActual: "administrativos",
  },
  {
    value: "ASESOR",
    label: "Asesor",
    endpoint: "asesor",
    endpointActual: "asesores",
  },
  {
    value: "ESTUDIANTE",
    label: "Estudiante",
    endpoint: "estudiante",
    endpointActual: "estudiantes",
  },
  {
    value: "MONITOR",
    label: "Monitor",
    endpoint: "monitor",
    endpointActual: "monitores",
  },
  {
    value: "CONCILIADOR",
    label: "Conciliador",
    endpoint: "conciliador",
    endpointActual: "conciliadores",
  },
];

export const VALORES_INICIALES = {
  usuarioSistemaId: "",
  destino: "",
  rolIdDestino: "",
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
