const CAMPOS_PERSONA = {
  tipoPersonaId: ["Tipo de persona", "Identificación"],
  tipoDocumento: ["Tipo de documento", "Identificación"],
  numeroDocumento: ["Número de documento", "Identificación"],
  fechaExpedicion: ["Fecha de expedición", "Identificación"],
  ciudadExpedicion: ["Ciudad de expedición", "Identificación"],
  nombres: ["Nombres", "Identificación"],
  apellidos: ["Apellidos", "Identificación"],
  fechaNacimiento: ["Fecha de nacimiento", "Identificación"],
  nombreIdentitario: ["Nombre identitario", "Identidad"],
  pronombre: ["Pronombre", "Identidad"],
  sexo: ["Sexo", "Identidad"],
  genero: ["Género", "Identidad"],
  orientacionSexual: ["Orientación sexual", "Identidad"],
  nacionalidadId: ["Nacionalidad", "Identidad"],
  estadoCivil: ["Estado civil", "Identidad"],
  escolaridad: ["Escolaridad", "Identidad"],
  grupoEtnico: ["Grupo étnico", "Identidad"],
  condicionActualId: ["Condición actual", "Identidad"],
  discapacidad: ["Discapacidad", "Identidad"],
  caracterizacionPcd: ["Caracterización PCD", "Identidad"],
  sabeLeerEscribir: ["Sabe leer y escribir", "Identidad"],
  necesitaAjustePcd: ["Necesita ajuste razonable", "Identidad"],
  telefono: ["Teléfono", "Contacto"],
  correo: ["Correo electrónico", "Contacto"],
  departamentoId: ["Departamento", "Vivienda"],
  municipioId: ["Municipio", "Vivienda"],
  barrioId: ["Barrio", "Vivienda"],
  direccion: ["Dirección", "Vivienda"],
  comuna: ["Comuna", "Vivienda"],
  localidad: ["Localidad", "Vivienda"],
  estrato: ["Estrato", "Vivienda"],
  tipoVivienda: ["Tipo de vivienda", "Vivienda"],
  zona: ["Zona", "Vivienda"],
  tenencia: ["Tenencia de la vivienda", "Vivienda"],
  numeroPersonasACargo: ["Número de personas a cargo", "Vivienda"],
  ingresosAdicionales: ["Ingresos adicionales", "Vivienda"],
  energiaElectrica: ["Energía eléctrica", "Vivienda"],
  acueducto: ["Acueducto", "Vivienda"],
  alcantarillado: ["Alcantarillado", "Vivienda"],
  ocupacionId: ["Ocupación", "Economía"],
  empresaId: ["Empresa donde labora", "Economía"],
  salario: ["Salario", "Economía"],
  cargo: ["Cargo", "Economía"],
  direccionEmpresa: ["Dirección de la empresa", "Economía"],
  telefonoEmpresa: ["Teléfono de la empresa", "Economía"],
  nombreCompletoAcudiente: ["Nombre completo del acudiente", "Acudiente"],
  relacionAcudiente: ["Relación o parentesco del acudiente", "Acudiente"],
  telefonoAcudiente: ["Teléfono del acudiente", "Acudiente"],
  correoAcudiente: ["Correo electrónico del acudiente", "Acudiente"],
  direccionAcudiente: ["Dirección del acudiente", "Acudiente"],
  comoSeEntero: ["Cómo se enteró de nuestros servicios", "Servicio"],
  relacionConUniversidad: ["Relación con la Universidad", "Servicio"],
};

const ORDEN_CAMPOS = new Map(
  Object.keys(CAMPOS_PERSONA).map((campo, indice) => [campo, indice]),
);

function normalizarNombreCampo(nombre) {
  const ultimoSegmento = String(nombre ?? "")
    .split(".")
    .at(-1)
    ?.replace(/\[\d+\]$/u, "");

  return ultimoSegmento || "formulario";
}

function humanizarNombreCampo(nombre) {
  const texto = normalizarNombreCampo(nombre)
    .replace(/Id$/u, "")
    .replace(/([a-záéíóúñ])([A-ZÁÉÍÓÚÑ])/gu, "$1 $2")
    .trim();

  if (!texto || texto === "formulario") return "Formulario";
  return texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase();
}

function crearDetalle(campoOriginal, mensaje) {
  const campo = normalizarNombreCampo(campoOriginal);
  const [etiqueta, seccion] = CAMPOS_PERSONA[campo] ?? [
    humanizarNombreCampo(campo),
    "Formulario",
  ];

  return {
    campo,
    etiqueta,
    seccion,
    mensaje: String(mensaje || "El valor ingresado no es válido"),
  };
}

function ordenarYEliminarDuplicados(detalles) {
  const vistos = new Set();

  return detalles
    .filter((detalle) => {
      const clave = `${detalle.campo}:${detalle.mensaje}`;
      if (vistos.has(clave)) return false;
      vistos.add(clave);
      return true;
    })
    .sort(
      (a, b) =>
        (ORDEN_CAMPOS.get(a.campo) ?? Number.MAX_SAFE_INTEGER) -
        (ORDEN_CAMPOS.get(b.campo) ?? Number.MAX_SAFE_INTEGER),
    );
}

export function crearErrorPersona(campo, mensaje) {
  return crearDetalle(campo, mensaje);
}

export function extraerErroresLocales(errores = {}) {
  const detalles = Object.entries(errores).flatMap(([campo, error]) => {
    if (!error) return [];
    return [crearDetalle(campo, error.message)];
  });

  return ordenarYEliminarDuplicados(detalles);
}

export function extraerErroresServidor(payload) {
  const origen =
    payload?.detalles ??
    payload?.details ??
    payload?.fieldErrors ??
    payload?.validaciones ??
    payload?.errors;

  if (!origen) return [];

  const detalles = [];

  if (Array.isArray(origen)) {
    for (const error of origen) {
      if (typeof error === "string") {
        detalles.push(crearDetalle("formulario", error));
      } else if (error && typeof error === "object") {
        const campo =
          error.campo ?? error.field ?? error.property ?? "formulario";
        const mensaje = error.mensaje ?? error.message ?? error.defaultMessage;
        if (mensaje) detalles.push(crearDetalle(campo, mensaje));
      }
    }
  } else if (typeof origen === "object") {
    for (const [campo, valor] of Object.entries(origen)) {
      const mensajes = Array.isArray(valor) ? valor : [valor];
      for (const mensaje of mensajes) {
        if (typeof mensaje === "string" || typeof mensaje === "number") {
          detalles.push(crearDetalle(campo, mensaje));
        } else if (mensaje && typeof mensaje === "object") {
          detalles.push(
            crearDetalle(
              mensaje.campo ?? mensaje.field ?? campo,
              mensaje.mensaje ?? mensaje.message ?? mensaje.defaultMessage,
            ),
          );
        }
      }
    }
  }

  return ordenarYEliminarDuplicados(detalles);
}
