import { FORM_INICIAL } from "./personas.constants";

export function toOption(item) {
  return {
    value: String(item.id),
    label: item.nombre || item.descripcion || `Registro ${item.id}`,
  };
}

export function toDocumentoOption(item) {
  const value = item.codigo || item.abreviatura || item.nombre || item.descripcion || "";
  const label = item.nombre || item.descripcion || item.codigo || item.abreviatura || value;

  return { value, label };
}

export function optionsMap(options) {
  return new Map(options.map((option) => [String(option.value), option.label]));
}

export function labelFromMap(map, value, fallback = "N/A") {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  return map.get(String(value)) || fallback;
}

export function nombreCompleto(persona) {
  return [persona?.nombres, persona?.apellidos].filter(Boolean).join(" ");
}

export function valorTexto(value) {
  return value || "N/A";
}

export function numberOrNull(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isNaN(number) ? null : number;
}

export function textOrNull(value) {
  const text = String(value ?? "").trim();
  return text === "" ? null : text;
}

export function calcularEsMenorEdad(fechaNacimiento) {
  if (!fechaNacimiento) return false;

  const nacimiento = new Date(fechaNacimiento);
  if (Number.isNaN(nacimiento.getTime())) return false;

  const hoy = new Date();
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  const mes = hoy.getMonth() - nacimiento.getMonth();

  if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
    edad -= 1;
  }

  return edad < 18;
}

export function ordenarPorIdAscendente(items) {
  return [...items].sort((a, b) => {
    const idA = Number(a?.id ?? Number.MAX_SAFE_INTEGER);
    const idB = Number(b?.id ?? Number.MAX_SAFE_INTEGER);

    return idA - idB;
  });
}

export function obtenerPaginasVisibles(paginaActual, totalPaginas) {
  const paginas = new Set([
    1,
    totalPaginas,
    paginaActual - 1,
    paginaActual,
    paginaActual + 1,
  ]);

  return Array.from(paginas)
    .filter((pagina) => pagina >= 1 && pagina <= totalPaginas)
    .sort((a, b) => a - b);
}

export function convertirPersonaAForm(persona) {
  return {
    ...FORM_INICIAL,
    ...persona,
    tipoPersonaId: persona.tipoPersonaId != null ? String(persona.tipoPersonaId) : "",
    nacionalidadId: persona.nacionalidadId != null ? String(persona.nacionalidadId) : "",
    condicionActualId:
      persona.condicionActualId != null ? String(persona.condicionActualId) : "",
    departamentoId: persona.departamentoId != null ? String(persona.departamentoId) : "",
    municipioId: persona.municipioId != null ? String(persona.municipioId) : "",
    barrioId: persona.barrioId != null ? String(persona.barrioId) : "",
    ocupacionId: persona.ocupacionId != null ? String(persona.ocupacionId) : "",
    empresaId: persona.empresaId != null ? String(persona.empresaId) : "",
    sabeLeerEscribir: Boolean(persona.sabeLeerEscribir),
    necesitaAjustePcd: Boolean(persona.necesitaAjustePcd),
    ingresosAdicionales: Boolean(persona.ingresosAdicionales),
    energiaElectrica: Boolean(persona.energiaElectrica),
    acueducto: Boolean(persona.acueducto),
    alcantarillado: Boolean(persona.alcantarillado),
    estrato: persona.estrato ?? 0,
    numeroPersonasACargo: persona.numeroPersonasACargo ?? 0,
    salario: persona.salario ?? 0,
  };
}

export function construirPayload(form, id, version) {
  const esMenorEdad = calcularEsMenorEdad(form.fechaNacimiento);

  return {
    ...form,
    id,
    version,
    tipoPersonaId: numberOrNull(form.tipoPersonaId),
    nacionalidadId: numberOrNull(form.nacionalidadId),
    condicionActualId: numberOrNull(form.condicionActualId),
    departamentoId: numberOrNull(form.departamentoId),
    municipioId: numberOrNull(form.municipioId),
    barrioId: numberOrNull(form.barrioId),
    ocupacionId: numberOrNull(form.ocupacionId),
    empresaId: numberOrNull(form.empresaId),
    estrato: Number(form.estrato || 0),
    numeroPersonasACargo: Number(form.numeroPersonasACargo || 0),
    salario: Number(form.salario || 0),
    correo: textOrNull(form.correo),
    correoAcudiente: esMenorEdad ? textOrNull(form.correoAcudiente) : null,
    nombreCompletoAcudiente: esMenorEdad ? textOrNull(form.nombreCompletoAcudiente) : null,
    relacionAcudiente: esMenorEdad ? textOrNull(form.relacionAcudiente) : null,
    telefonoAcudiente: esMenorEdad ? textOrNull(form.telefonoAcudiente) : null,
    direccionAcudiente: esMenorEdad ? textOrNull(form.direccionAcudiente) : null,
  };
}
