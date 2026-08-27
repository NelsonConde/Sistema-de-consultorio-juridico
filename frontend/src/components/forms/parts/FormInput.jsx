/**
 * Campo de entrada de texto reutilizable para formularios.
 *
 * Integra `register` de react-hook-form y muestra el mensaje de error
 * del campo automáticamente si existe. También puede restringir la entrada
 * a dígitos sin cambiar el diseño del componente.
 *
 * @module components/forms/parts/FormInput
 */
import React from "react";
import { Input } from "@/components/ui/input";
import { sanitizeDigits } from "@/lib/form-validation";

function hasRequiredRule(rules) {
  return Boolean(rules?.required);
}

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
 *
 * `digitsOnly` sanea el valor en el evento de cambio para impedir que letras,
 * espacios o símbolos lleguen al estado del formulario. Es apropiado para
 * teléfonos, no para documentos que puedan ser alfanuméricos.
 */
export function FormInput({
  name,
  label,
  type = "text",
  register,
  errors,
  rules,
  digitsOnly = false,
  ...props
}) {
  const error = errors?.[name];
  const required = hasRequiredRule(rules);
  const registration = register(name, rules);
  const externalOnChange = props.onChange;
  const maxLength = props.maxLength;

  const handleChange = (event) => {
    if (digitsOnly) {
      event.target.value = sanitizeDigits(event.target.value, maxLength);
    }

    registration.onChange(event);

    if (typeof externalOnChange === "function") {
      externalOnChange(event);
    }
  };

  const inputProps = { ...props };
  delete inputProps.onChange;

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
        {...registration}
        {...inputProps}
        inputMode={digitsOnly ? "numeric" : inputProps.inputMode}
        pattern={digitsOnly ? "[0-9]*" : inputProps.pattern}
        onChange={handleChange}
        className={`${inputProps.className || ""} ${error ? "border-red-500" : ""}`.trim()}
      />

      {error && (
        <p className="text-xs text-red-500">
          {error?.message}
        </p>
      )}
    </div>
  );
}
