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
