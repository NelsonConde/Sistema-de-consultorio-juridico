/** Constantes y opciones del formulario de persona. */
export const REQUIRED_MESSAGE = "El campo es obligatorio";
export const MAX_ESTRATO = 7;
export const MAX_PERSONAS_A_CARGO = 10;

export const OPCIONES_VACIAS = [];

export const FORM_DEFAULTS = {
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
  sabeLeerEscribir: true,
  discapacidad: "",
  caracterizacionPcd: "",
  necesitaAjustePcd: false,
  departamentoId: "",
  municipioId: "",
  barrioId: "",
  direccion: "",
  comuna: "",
  localidad: "",
  estrato: "",
  tipoVivienda: "",
  zona: "",
  tenencia: "",
  numeroPersonasACargo: "",
  ingresosAdicionales: false,
  energiaElectrica: false,
  acueducto: false,
  alcantarillado: false,
  ocupacionId: "",
  empresaId: "",
  salario: "",
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

export const fallbackTipoDocumentoOptions = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
];

export const pronombreOptions = [
  { value: "Él", label: "Él" },
  { value: "Ella", label: "Ella" },
  { value: "Elle", label: "Elle" },
  { value: "Otro", label: "Otro" },
];

export const sexoOptions = [
  { value: "Hombre", label: "Hombre" },
  { value: "Mujer", label: "Mujer" },
  { value: "Intersexual", label: "Intersexual" },
];

export const generoOptions = [
  { value: "Masculino", label: "Masculino" },
  { value: "Femenino", label: "Femenino" },
  { value: "No binario", label: "No binario" },
  { value: "Transgénero", label: "Transgénero" },
  { value: "Otro", label: "Otro" },
];

export const orientacionSexualOptions = [
  { value: "Heterosexual", label: "Heterosexual" },
  { value: "Homosexual", label: "Homosexual" },
  { value: "Bisexual", label: "Bisexual" },
  { value: "Pansexual", label: "Pansexual" },
  { value: "Asexual", label: "Asexual" },
  { value: "Otro", label: "Otro" },
];

export const estadoCivilOptions = [
  { value: "Soltero/a", label: "Soltero/a" },
  { value: "Casado/a", label: "Casado/a" },
  { value: "Unión libre", label: "Unión libre" },
  { value: "Divorciado/a", label: "Divorciado/a" },
  { value: "Viudo/a", label: "Viudo/a" },
];

export const escolaridadOptions = [
  { value: "Ninguna", label: "Ninguna" },
  { value: "Primaria", label: "Primaria" },
  { value: "Secundaria", label: "Secundaria" },
  { value: "Técnico", label: "Técnico" },
  { value: "Tecnólogo", label: "Tecnólogo" },
  { value: "Universitario", label: "Universitario" },
  { value: "Postgrado", label: "Postgrado" },
];

export const zonaOptions = [
  { value: "Urbana", label: "Urbana" },
  { value: "Rural", label: "Rural" },
];
