/**
 * Utilidades generales de UI para el sistema de casos jurídicos.
 *
 * @module lib/utils
 */

import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Combina clases de Tailwind CSS resolviendo conflictos automáticamente.
 *
 * Internamente usa `clsx` para aceptar strings, arrays y objetos condicionales,
 * y `tailwind-merge` para resolver conflictos entre clases del mismo grupo
 * (p. ej. `p-2` y `p-4` en el mismo elemento).
 *
 * Usar esta función en lugar de concatenar strings directamente evita que
 * clases duplicadas o contradictorias se apliquen al DOM.
 *
 * @param {...(string|string[]|Record<string,boolean>|undefined|null|false)} inputs
 *   Cualquier combinación de clases aceptada por `clsx`.
 * @returns {string} String de clases CSS unificado y sin conflictos.
 *
 * @example
 * cn("px-4 py-2", isActive && "bg-primary", "px-6")
 * // → "py-2 bg-primary px-6"  (px-4 resuelto a px-6 por tailwind-merge)
 */
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}
