/**
 * Selection behavior.
 *
 * List and table handling.
 *
 * @module components/forms/parts/FormSelect
 */
function hasRequiredRule(rules) {
  return Boolean(rules?.required);
}

/**
 * Implementation detail.
 * @param {string|React.ReactNode} label - Label to render.
 * @param {boolean} required - Parameter description.
 * @returns {React.ReactNode|null} Result value.
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
 * Selection behavior.
 * @param {Object} props - Parameter description.
 * @param {string} props.name - Parameter description.
 * @param {string|React.ReactNode} props.label - Parameter description.
 * Implementation detail.
 * @param {function} props.register - react-hook-form register function.
 * @param {Object} props.errors - Parameter description.
 * @param {Object} [props.rules] - Parameter description.
 * @param {string} [props.placeholder] - Parameter description.
 * @returns {JSX.Element} Result value.
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
