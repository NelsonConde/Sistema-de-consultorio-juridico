/**
 * Reglas de validación reutilizables para `react-hook-form`.
 *
 * Cada función devuelve un objeto de reglas compatible con el campo `rules`
 * del componente `FormInput` y del método `register` de `react-hook-form`.
 *
 * @module lib/form-validation
 */

/** Mensaje de error por defecto para campos obligatorios. */
export const REQUIRED_MESSAGE = "El campo es obligatorio";

/** Expresión regular para validar emails. */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;

/**
 * Verifica si un valor está en blanco (vacío o solo espacios).
 *
 * @param {unknown} value - El valor a verificar.
 * @returns {boolean} `true` si el valor está en blanco.
 */
export function isBlank(value) {
  return String(value ?? "").trim() === "";
}

/**
 * Regla de validación de email opcional.
 * El campo puede estar vacío, pero si tiene valor debe ser un email válido.
 *
 * @param {string} [message="Ingrese un correo electrónico válido"] - Mensaje de error.
 * @returns {import("react-hook-form").RegisterOptions} Objeto de reglas para `register`.
 */
export function optionalEmailRule(message = "Ingrese un correo electrónico válido") {
  return {
    validate: (value) => isBlank(value) || EMAIL_PATTERN.test(String(value).trim()) || message,
  };
}

/**
 * Regla de validación de email obligatorio.
 * El campo es requerido y debe ser un email válido.
 *
 * @param {string} [message="Ingrese un correo electrónico válido"] - Mensaje de error de formato.
 * @returns {import("react-hook-form").RegisterOptions} Objeto de reglas para `register`.
 */
export function requiredEmailRule(message = "Ingrese un correo electrónico válido") {
  return {
    required: REQUIRED_MESSAGE,
    pattern: {
      value: EMAIL_PATTERN,
      message,
    },
  };
}

/**
 * Regla de validación para números no negativos (>= 0).
 *
 * @param {string} [message="El valor no puede ser negativo"] - Mensaje de error.
 * @returns {import("react-hook-form").RegisterOptions} Objeto de reglas para `register`.
 */
export function nonNegativeNumberRule(message = "El valor no puede ser negativo") {
  return {
    min: {
      value: 0,
      message,
    },
  };
}

/**
 * Regla de validación para números con valor máximo.
 *
 * @param {number} max - Valor máximo permitido.
 * @param {string} [message] - Mensaje de error. Por defecto incluye el valor máximo.
 * @returns {import("react-hook-form").RegisterOptions} Objeto de reglas para `register`.
 */
export function maxNumberRule(max, message = `El valor no puede ser mayor a ${max}`) {
  return {
    max: {
      value: max,
      message,
    },
  };
}

/**
 * Regla de validación para campos `select` obligatorios.
 *
 * @param {string} [message="Debe seleccionar una opción"] - Mensaje de error.
 * @returns {import("react-hook-form").RegisterOptions} Objeto de reglas para `register`.
 */
export function requiredSelectRule(message = "Debe seleccionar una opción") {
  return {
    required: message,
  };
}
