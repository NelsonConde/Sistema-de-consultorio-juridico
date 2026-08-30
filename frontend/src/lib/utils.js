/**
 * Implementation detail.
 *
 * @module lib/utils
 */

import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Implementation detail.
 *
 * Implementation detail.
 * Implementation detail.
 * Implementation detail.
 *
 * Implementation detail.
 * Implementation detail.
 *
 * @param {...(string|string[]|Record<string,boolean>|undefined|null|false)} inputs
 * Implementation detail.
 * @returns {string} Result value.
 *
 * @example
 * cn("px-4 py-2", isActive && "bg-primary", "px-6")
 * Implementation detail.
 */
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}
