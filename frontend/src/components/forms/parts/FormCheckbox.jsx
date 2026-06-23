/**
 * Campo de checkbox reutilizable para formularios.
 *
 * Integra `register` de react-hook-form.
 *
 * @module components/forms/parts/FormCheckbox
 */
import React from "react";

/**
 * Checkbox para formularios que integra react-hook-form.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.name - Nombre del campo.
 * @param {string|React.ReactNode} props.label - Etiqueta mostrada junto al checkbox.
 * @param {function} props.register - Función de registro de react-hook-form.
 * @param {Object} props.errors - Errores de validación del formulario.
 * @param {Object} [props.rules] - Reglas de validación.
 * @returns {JSX.Element} Campo checkbox con validación.
 */
export function FormCheckbox({
  name,
  label,
  register,
  errors,
  rules,
  ...props
}) {
  return (
    <div className="flex flex-col gap-1 w-full">
      <div className="flex items-center gap-2">
        <input
          id={name}
          type="checkbox"
          {...register(name, rules)}
          {...props}
          className={`h-4 w-4 rounded border-input ${
            errors?.[name] ? "border-red-500" : ""
          }`}
        />

        {label && (
          <label
            htmlFor={name}
            className="text-sm font-medium leading-none cursor-pointer"
          >
            {label}
          </label>
        )}
      </div>

      {errors?.[name] && (
        <p className="text-xs text-red-500">
          {errors[name]?.message}
        </p>
      )}
    </div>
  );
}