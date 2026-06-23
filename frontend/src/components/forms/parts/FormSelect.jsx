/**
 * Campo de selección reutilizable para formularios.
 *
 * Integra `register` de react-hook-form con lista de opciones `{ value, label }`.
 *
 * @module components/forms/parts/FormSelect
 */
function hasRequiredRule(rules) {
  return Boolean(rules?.required);
}

/**
 * Renderiza la etiqueta del campo y agrega el marcador obligatorio.
 * @param {string|React.ReactNode} label - Etiqueta a renderizar.
 * @param {boolean} required - Indica si el campo es obligatorio.
 * @returns {React.ReactNode|null} Elemento de etiqueta.
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
 * Campo de selección compatible con react-hook-form.
 * @param {Object} props - Propiedades del componente.
 * @param {string} props.name - Nombre del campo.
 * @param {string|React.ReactNode} props.label - Texto de la etiqueta.
 * @param {Array<{value:string,label:string}>} props.options - Opciones del select.
 * @param {function} props.register - Registro de react-hook-form.
 * @param {Object} props.errors - Errores de validación.
 * @param {Object} [props.rules] - Reglas de validación.
 * @param {string} [props.placeholder] - Texto de ayuda para el select.
 * @returns {JSX.Element} Campo select con validación.
 */
export function FormSelect({
  name,
  label,
  options,
  register,
  errors,
  rules,
  placeholder = "Seleccione una opción",
  ...props
}) {
  const error = errors?.[name];
  const required = hasRequiredRule(rules);

  return (
    <div className="flex flex-col gap-1.5 w-full">
      {label && (
        <label htmlFor={name} className="text-sm font-medium leading-none">
          {renderLabel(label, required)}
        </label>
      )}

      <select
        id={name}
        defaultValue=""
        aria-invalid={error ? "true" : "false"}
        aria-required={required ? "true" : "false"}
        {...register(name, rules)}
        {...props}
        className={`flex h-8 w-full rounded-lg border px-2.5 py-1 ${
          error ? "border-red-500" : ""
        }`}
      >
        <option value="">{placeholder}</option>
        {(options || []).map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>

      {error && (
        <p className="text-xs text-red-500">{error?.message}</p>
      )}
    </div>
  );
}
