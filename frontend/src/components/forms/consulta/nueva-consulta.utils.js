import { readResponseBody } from "@/lib/api";

export async function leerRespuesta(res) {
  const data = await readResponseBody(res);
  return typeof data === "string" ? { mensaje: data } : data;
}

export function textOrNull(value) {
  const text = String(value ?? "").trim();
  return text === "" ? null : text;
}

export function numberOrNull(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isNaN(number) ? null : number;
}

export function numberArray(values) {
  if (!Array.isArray(values)) {
    return [];
  }

  return values
    .map((value) => Number(value))
    .filter((value) => !Number.isNaN(value));
}

export function idNormalizado(value) {
  if (value === null || value === undefined || value === "") {
    return "";
  }

  return String(value);
}

export function obtenerAreaIdAsesor(asesor) {
  return idNormalizado(asesor?.areaId ?? asesor?.area?.id);
}

export function obtenerAsesorIdEstudiante(estudiante) {
  return idNormalizado(estudiante?.asesorId ?? estudiante?.asesor?.id);
}

function tieneDuplicados(values = []) {
  const normalizados = values.map(Number).filter((value) => !Number.isNaN(value));
  return new Set(normalizados).size !== normalizados.length;
}

/**
 * Replica en la UI las invariantes de ConsultaValidator que pueden comprobarse
 * con los catálogos ya cargados. El backend sigue siendo la autoridad final.
 */
export function validarCoherenciaConsultaFrontend({
  form,
  temas = [],
  tipos = [],
  asesores = [],
  monitores = [],
  estudiantes = [],
}) {
  const areaId = idNormalizado(form?.areaId);
  const temaId = idNormalizado(form?.temaId);
  const tipoId = idNormalizado(form?.tipoId);
  const asesorId = idNormalizado(form?.asesorId);
  const monitorId = idNormalizado(form?.monitorId);
  const estudianteId = idNormalizado(form?.estudianteId);

  if (temaId && !temas.some((tema) => idNormalizado(tema?.id) === temaId)) {
    return "El tema seleccionado no pertenece al área de la consulta";
  }

  if (tipoId && !tipos.some((tipo) => idNormalizado(tipo?.id) === tipoId)) {
    return "El tipo seleccionado no pertenece al tema de la consulta";
  }

  if (asesorId) {
    const asesor = asesores.find((item) => idNormalizado(item?.id) === asesorId);

    if (!asesor) {
      return "Asesor no encontrado o inactivo";
    }

    if (!areaId || obtenerAreaIdAsesor(asesor) !== areaId) {
      return "El asesor asignado no pertenece al área de la consulta";
    }
  }

  if (monitorId) {
    const monitor = monitores.find((item) => idNormalizado(item?.id) === monitorId);

    if (!monitor) {
      return "Monitor no encontrado o inactivo";
    }
  }

  if (estudianteId) {
    const estudiante = estudiantes.find(
      (item) => idNormalizado(item?.id) === estudianteId
    );

    if (!estudiante) {
      return "Estudiante no encontrado o inactivo";
    }

    const asesorEstudianteId = obtenerAsesorIdEstudiante(estudiante);

    if (!asesorEstudianteId) {
      return "El estudiante seleccionado no tiene asesor asignado";
    }

    if (asesorId && asesorEstudianteId !== asesorId) {
      return "El estudiante asignado no pertenece al asesor seleccionado";
    }

    const asesorDelEstudiante = asesores.find(
      (item) => idNormalizado(item?.id) === asesorEstudianteId
    );

    if (!asesorDelEstudiante) {
      return "El asesor asignado al estudiante no existe o está inactivo";
    }

    if (!areaId || obtenerAreaIdAsesor(asesorDelEstudiante) !== areaId) {
      return "El asesor del estudiante no pertenece al área de la consulta";
    }
  }

  const personaId = Number(form?.personaId);
  const partesIds = numberArray(form?.partesIds);
  const contrapartesIds = numberArray(form?.contrapartesIds);

  if (!Number.isNaN(personaId) && partesIds.includes(personaId)) {
    return "La persona principal no puede repetirse como parte adicional";
  }

  if (!Number.isNaN(personaId) && contrapartesIds.includes(personaId)) {
    return "La persona principal no puede repetirse como contraparte";
  }

  if (partesIds.some((id) => contrapartesIds.includes(id))) {
    return "Una misma persona no puede estar como parte y contraparte";
  }

  if (tieneDuplicados(form?.partesIds || [])) {
    return "Existen personas repetidas en partes";
  }

  if (tieneDuplicados(form?.contrapartesIds || [])) {
    return "Existen personas repetidas en contrapartes";
  }

  return null;
}
