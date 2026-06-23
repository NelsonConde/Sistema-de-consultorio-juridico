/**
 * Campo de entrada de texto reutilizable para formularios.
 *
 * Integra `register` de react-hook-form y muestra el mensaje de error
 * del campo automáticamente si existe.
 *
 * @module components/forms/parts/FormInput
 */
import React from "react";
import { Input } from "@/components/ui/input";

/**
 * Comprueba si las reglas de validación incluyen un campo obligatorio.
 * @param {Object} rules - Reglas de validación de react-hook-form.
 * @returns {boolean} True si el campo es requerido.
 */
function hasRequiredRule(rules) {
  return Boolean(rules?.required);
}

/**
 * Renderiza la etiqueta del campo con un asterisco si es requerido.
 * @param {string|React.ReactNode} label - Texto o elemento de etiqueta.
 * @param {boolean} required - Indica si el campo es obligatorio.
 * @returns {React.ReactNode} Etiqueta renderizada.
 */
function renderLabel(label, required) {
  if (!label) return null;

  if (required && typeof label === "string") {
    return (
      <span className="inline-flex items-center gap-1">
        <span>{label}</span>
        <span className="text-red-500" aria-hidden="true">*</span>
      </span>
    );
  }

  return label;
}

/**
 * Input de formulario que integra react-hook-form y muestra errores.
 * @param {{name:string, label:string, type?:string, register:function, errors:Object, rules?:Object}} props - Propiedades del componente.
 * @returns {JSX.Element} Campo de formulario.
 */
export function FormInput({ name, label, type = "text", register, errors, rules, ...props }) {
  const error = errors?.[name];
  const required = hasRequiredRule(rules);

  return (
    <div className="flex flex-col gap-1.5 w-full">
      {label && (
        <label htmlFor={name} className="text-sm font-medium leading-none">
          {renderLabel(label, required)}
        </label>
      )}

      <Input
        id={name}
        type={type}
        aria-invalid={error ? "true" : "false"}
        aria-required={required ? "true" : "false"}
        {...register(name, rules)}
        {...props}
        className={`${props.className || ""} ${error ? "border-red-500" : ""}`.trim()}
      />

      {error && (
        <p className="text-xs text-red-500">
          {error?.message}
        </p>
      )}
    </div>
  );
}
