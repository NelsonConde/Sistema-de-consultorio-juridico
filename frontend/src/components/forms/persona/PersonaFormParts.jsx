"use client";

export function requiredLabel(label) {
  return (
    <span className="inline-flex flex-wrap items-center gap-1">
      <span>{label}</span>
      <span className="text-red-500">*</span>
    </span>
  );
}

export function optionalLabel(label) {
  return <span>{label}</span>;
}

/**
 * Implementation detail.
 * Implementation detail.
 * @returns {{value:string, label:string} Result value.
 */

export function Seccion({ titulo, descripcion, children }) {
  return (
    <section className="space-y-4">
      <div>
        <h3 className="text-lg font-semibold border-b pb-2">{titulo}</h3>
        {descripcion && (
          <p className="mt-2 text-sm text-muted-foreground">{descripcion}</p>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {children}
      </div>
    </section>
  );
}

export function FormSelectField({
  name,
  label,
  options,
  register,
  errors,
  rules,
  disabled = false,
}) {
  return (
    <div className="flex flex-col gap-1.5 w-full">
      <label htmlFor={name} className="text-sm font-medium leading-none">
        {label}
      </label>

      <select
        id={name}
        defaultValue=""
        disabled={disabled}
        {...register(name, rules)}
        className={`flex h-9 w-full rounded-lg border bg-background px-2.5 py-1 text-sm disabled:cursor-not-allowed disabled:opacity-60 ${
          errors?.[name] ? "border-red-500" : ""
        }`}
      >
        <option value="">Seleccione una opción</option>

        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      {errors?.[name] && (
        <p className="text-xs text-red-500">{errors[name]?.message}</p>
      )}
    </div>
  );
}
