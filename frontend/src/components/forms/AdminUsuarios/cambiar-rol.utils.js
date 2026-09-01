import { normalizar } from "@/lib/authz";
import { TIPOS_PERFIL } from "./cambiar-rol.constants";

export function usuarioActivo(usuario) {
  return (
    usuario?.activo !== false &&
    String(usuario?.estado || "").toUpperCase() !== "INACTIVO"
  );
}

export function filtrarActivos(lista) {
  return Array.isArray(lista) ? lista.filter(usuarioActivo) : [];
}

export function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.rows)) return data.rows;
  return [];
}

export function mapOption(item) {
  return {
    value: item.id,
    label:
      item.displayName ||
      item.nombre ||
      item.descripcion ||
      item.codigo ||
      String(item.id),
  };
}

export function normalizarTexto(value) {
  const text = String(value || "").trim();
  return text === "" ? null : text;
}

export function toNumberOrNull(value) {
  if (value === null || value === undefined || value === "") return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

export function buscarPerfil(value) {
  return TIPOS_PERFIL.find((perfil) => perfil.value === value);
}

export function rolCoincideConPerfil(rol, perfil) {
  if (!rol?.tipoPerfil || !perfil?.value) return false;
  return normalizar(rol.tipoPerfil) === normalizar(perfil.value);
}

/**
 * Role handling.
 * @returns {JSX.Element} Result value.
 */
