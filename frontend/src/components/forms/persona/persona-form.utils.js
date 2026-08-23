import { FORM_DEFAULTS } from "./persona-form.constants";

export function toOption(item) {
  return {
    value: String(item.id),
    label: item.nombre || item.descripcion || `Registro ${item.id}`,
  };
}

/**
 * Convierte datos de documento en una opción amigable para el select.
 * @param {Object} item - Datos del documento.
 * @returns {{value:string, label:string}} Opción de documento.
 */
export function toDocumentoOption(item) {
  const value = item.codigo || item.abreviatura || item.nombre || item.descripcion || "";
  const label = item.nombre || item.descripcion || item.codigo || item.abreviatura || value;

  return { value, label };
}

/**
 * Normaliza los valores iniciales antes de usarlos en el formulario.
 * @param {Object} values - Valores iniciales recibidos.
 * @returns {Object} Valores preparados para el formulario.
 */
export function toFormValues(values = {}) {
  return {
    ...FORM_DEFAULTS,
    ...values,
    tipoPersonaId: values.tipoPersonaId != null ? String(values.tipoPersonaId) : "",
    nacionalidadId: values.nacionalidadId != null ? String(values.nacionalidadId) : "",
    condicionActualId:
      values.condicionActualId != null ? String(values.condicionActualId) : "",
    departamentoId: values.departamentoId != null ? String(values.departamentoId) : "",
    municipioId: values.municipioId != null ? String(values.municipioId) : "",
    barrioId: values.barrioId != null ? String(values.barrioId) : "",
    ocupacionId: values.ocupacionId != null ? String(values.ocupacionId) : "",
    empresaId: values.empresaId != null ? String(values.empresaId) : "",
    sabeLeerEscribir: values.sabeLeerEscribir ?? true,
    necesitaAjustePcd: Boolean(values.necesitaAjustePcd),
    ingresosAdicionales: Boolean(values.ingresosAdicionales),
    energiaElectrica: Boolean(values.energiaElectrica),
    acueducto: Boolean(values.acueducto),
    alcantarillado: Boolean(values.alcantarillado),
    estrato: values.estrato ?? "",
    numeroPersonasACargo: values.numeroPersonasACargo ?? "",
    salario: values.salario ?? "",
  };
}

/**
 * Convierte un valor numérico vacío en null.
 * @param {unknown} value - Valor de entrada.
 * @returns {number|null} Número o null.
 */
export function numberOrNull(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isNaN(number) ? null : number;
}

/**
 * Convierte texto vacío en null y elimina espacios innecesarios.
 * @param {unknown} value - Valor de texto recibido.
 * @returns {string|null} Texto limpio o null.
 */
export function textOrNull(value) {
  const text = String(value ?? "").trim();
  return text === "" ? null : text;
}

/**
 * Construye un payload con tipos normalizados para el backend.
 * @param {Object} data - Datos del formulario.
 * @returns {Object} Payload normalizado.
 */
export function construirPayload(data) {
  return {
    ...data,
    id: data.id ?? undefined,
    tipoPersonaId: numberOrNull(data.tipoPersonaId),
    nacionalidadId: numberOrNull(data.nacionalidadId),
    condicionActualId: numberOrNull(data.condicionActualId),
    departamentoId: numberOrNull(data.departamentoId),
    municipioId: numberOrNull(data.municipioId),
    barrioId: numberOrNull(data.barrioId),
    ocupacionId: numberOrNull(data.ocupacionId),
    empresaId: numberOrNull(data.empresaId),
    estrato: Number(data.estrato || 0),
    numeroPersonasACargo: Number(data.numeroPersonasACargo || 0),
    salario: Number(data.salario || 0),
    correo: textOrNull(data.correo),
    correoAcudiente: textOrNull(data.correoAcudiente),
    nombreCompletoAcudiente: textOrNull(data.nombreCompletoAcudiente),
    relacionAcudiente: textOrNull(data.relacionAcudiente),
    telefonoAcudiente: textOrNull(data.telefonoAcudiente),
    direccionAcudiente: textOrNull(data.direccionAcudiente),
  };
}

/**
 * Carga un catálogo desde la API.
 * @param {string} path - Ruta del catálogo relativa.
 * @returns {Promise<Array<any>>} Lista de registros.
 */
