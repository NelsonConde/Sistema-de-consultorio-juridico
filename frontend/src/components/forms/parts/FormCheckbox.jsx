/**
 * Form handling.
 *
 * Integrates `register` from react-hook-form.
 *
 * @module components/forms/parts/FormCheckbox
 */
import React from "react";

/**
 * Form handling.
 * @param {Object} props - Parameter description.
 * @param {string} props.name - Parameter description.
 * @param {string|React.ReactNode} props.label - Implementation detail.
 * @param {function} props.register - Parameter description.
 * @param {Object} props.errors - Parameter description.
 * @param {Object} [props.rules] - Parameter description.
 * @returns {JSX.Element} Result value.
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