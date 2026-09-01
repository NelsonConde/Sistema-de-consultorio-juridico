/** List and table handling.*/
export const FORM_INICIAL = {
  tipoPersonaId: "",
  tipoDocumento: "",
  numeroDocumento: "",
  fechaExpedicion: "",
  ciudadExpedicion: "",
  nombres: "",
  apellidos: "",
  nombreIdentitario: "",
  pronombre: "",
  sexo: "",
  genero: "",
  orientacionSexual: "",
  fechaNacimiento: "",
  telefono: "",
  correo: "",
  nacionalidadId: "",
  estadoCivil: "",
  escolaridad: "",
  grupoEtnico: "",
  condicionActualId: "",
  sabeLeerEscribir: false,
  discapacidad: "",
  caracterizacionPcd: "",
  necesitaAjustePcd: false,
  departamentoId: "",
  municipioId: "",
  barrioId: "",
  direccion: "",
  comuna: "",
  localidad: "",
  estrato: 0,
  tipoVivienda: "",
  zona: "",
  tenencia: "",
  numeroPersonasACargo: 0,
  ingresosAdicionales: false,
  energiaElectrica: false,
  acueducto: false,
  alcantarillado: false,
  ocupacionId: "",
  empresaId: "",
  salario: 0,
  cargo: "",
  direccionEmpresa: "",
  telefonoEmpresa: "",
  nombreCompletoAcudiente: "",
  relacionAcudiente: "",
  telefonoAcudiente: "",
  correoAcudiente: "",
  direccionAcudiente: "",
  comoSeEntero: "",
  relacionConUniversidad: "",
};

export const FALLBACK_TIPO_DOCUMENTO_OPTIONS = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
];

export const PRONOMBRE_OPTIONS = [
  { value: "Él", label: "Él" },
  { value: "Ella", label: "Ella" },
  { value: "Elle", label: "Elle" },
  { value: "Otro", label: "Otro" },
];

export const SEXO_OPTIONS = [
  { value: "Hombre", label: "Hombre" },
  { value: "Mujer", label: "Mujer" },
  { value: "Intersexual", label: "Intersexual" },
];

export const GENERO_OPTIONS = [
  { value: "Masculino", label: "Masculino" },
  { value: "Femenino", label: "Femenino" },
  { value: "No binario", label: "No binario" },
  { value: "Transgénero", label: "Transgénero" },
  { value: "Otro", label: "Otro" },
];

export const ORIENTACION_OPTIONS = [
  { value: "Heterosexual", label: "Heterosexual" },
  { value: "Homosexual", label: "Homosexual" },
  { value: "Bisexual", label: "Bisexual" },
  { value: "Pansexual", label: "Pansexual" },
  { value: "Asexual", label: "Asexual" },
  { value: "Otro", label: "Otro" },
];

export const ESTADO_CIVIL_OPTIONS = [
  { value: "Soltero/a", label: "Soltero/a" },
  { value: "Casado/a", label: "Casado/a" },
  { value: "Unión libre", label: "Unión libre" },
  { value: "Divorciado/a", label: "Divorciado/a" },
  { value: "Viudo/a", label: "Viudo/a" },
];

export const ESCOLARIDAD_OPTIONS = [
  { value: "Ninguna", label: "Ninguna" },
  { value: "Primaria", label: "Primaria" },
  { value: "Secundaria", label: "Secundaria" },
  { value: "Técnico", label: "Técnico" },
  { value: "Tecnólogo", label: "Tecnólogo" },
  { value: "Universitario", label: "Universitario" },
  { value: "Postgrado", label: "Postgrado" },
];

export const ZONA_OPTIONS = [
  { value: "Urbana", label: "Urbana" },
  { value: "Rural", label: "Rural" },
];

export const REGISTROS_POR_PAGINA_OPTIONS = [5, 10, 20, 50];
