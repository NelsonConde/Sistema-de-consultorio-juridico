/**
 * Validation rule.
 *
 * Validation rule.
 * Validation rule.
 * Permission and authorization handling.
 * Implementation detail.
 *
 * @module lib/form-validation
 */

export const REQUIRED_MESSAGE = "El campo es obligatorio";
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;
export const DIGITS_PATTERN = /^\d+$/;

export function isBlank(value) {
  return String(value ?? "").trim() === "";
}

export function sanitizeDigits(value, maxLength = null) {
  const digits = String(value ?? "").replace(/\D/g, "");
  return Number.isInteger(maxLength) && maxLength >= 0
    ? digits.slice(0, maxLength)
    : digits;
}

export function optionalEmailRule(message = "Ingrese un correo electrónico válido") {
  return {
    validate: (value) => isBlank(value) || EMAIL_PATTERN.test(String(value).trim()) || message,
  };
}

export function requiredEmailRule(message = "Ingrese un correo electrónico válido") {
  return {
    required: REQUIRED_MESSAGE,
    pattern: {
      value: EMAIL_PATTERN,
      message,
    },
  };
}

export function maxLengthRule(max, message = `El campo no puede superar ${max} caracteres`) {
  return {
    maxLength: {
      value: max,
      message,
    },
  };
}

export function minLengthRule(min, message = `El campo debe tener al menos ${min} caracteres`) {
  return {
    minLength: {
      value: min,
      message,
    },
  };
}

export function exactLengthRule(length, message = `El campo debe tener exactamente ${length} caracteres`) {
  return {
    validate: (value) =>
      isBlank(value) || String(value).trim().length === length || message,
  };
}

export function digitsOnlyRule({
  required = false,
  maxLength = 30,
  message = "Solo se permiten números",
} = {}) {
  return {
    ...(required ? { required: REQUIRED_MESSAGE } : {}),
    ...(Number.isInteger(maxLength) ? maxLengthRule(maxLength) : {}),
    validate: (value) =>
      (!required && isBlank(value)) ||
      (required && !isBlank(value) && DIGITS_PATTERN.test(String(value))) ||
      (!required && DIGITS_PATTERN.test(String(value))) ||
      message,
  };
}

export function nonNegativeNumberRule(message = "El valor no puede ser negativo") {
  return {
    min: {
      value: 0,
      message,
    },
  };
}

export function maxNumberRule(max, message = `El valor no puede ser mayor a ${max}`) {
  return {
    max: {
      value: max,
      message,
    },
  };
}

export function integerNumberRule(message = "El valor debe ser un número entero") {
  return {
    validate: (value) =>
      isBlank(value) || Number.isInteger(Number(value)) || message,
  };
}

export function requiredSelectRule(message = "Debe seleccionar una opción") {
  return {
    required: message,
  };
}

export function futureOrPresentDateRule(message = "La fecha no puede ser anterior a la fecha actual") {
  return {
    validate: (value) => {
      if (isBlank(value)) return true;
      const selected = new Date(`${String(value).slice(0, 10)}T00:00:00`);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      return (!Number.isNaN(selected.getTime()) && selected >= today) || message;
    },
  };
}

export function futureDateRule(message = "La fecha debe ser futura") {
  return {
    validate: (value) => {
      if (isBlank(value)) return true;
      const selected = new Date(value);
      return (!Number.isNaN(selected.getTime()) && selected.getTime() > Date.now()) || message;
    },
  };
}
