"use client";

import { Eye, EyeOff, ShieldCheck } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useApiForm } from "@/hooks/useApiForm";
import { getApiErrorDescription, withErrorReference } from "@/lib/api";
import { API_URL_BASE } from "@/lib/config";
import { passwordPolicyRule, passwordsMatchRule } from "@/lib/form-validation";

const PASSWORD_POLICY_HINTS = [
  "Mínimo 8 caracteres",
  "Al menos una letra mayúscula",
  "Al menos un número",
];

const EMPTY_VALUES = {
  passwordActual: "",
  passwordNueva: "",
  confirmarPassword: "",
};

/**
 * Formulario de cambio de contraseña para un usuario ya autenticado.
 *
 * - No persiste valores fuera del estado en memoria de react-hook-form.
 * - Bloquea doble envío con un ref adicional al estado de isSubmitting
 *   (el estado tarda un render en reflejarse en el botón).
 * - Limpia los campos al desmontar o tras un envío exitoso.
 * - Los errores llegan tal cual del backend (BusinessException), que ya
 *   evita revelar detalles internos (ver CambioPasswordValidator).
 */
export function CambiarPasswordForm({ isActive = true }) {
  const [feedback, setFeedback] = useState(null);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    mode: "onBlur",
    defaultValues: EMPTY_VALUES,
  });

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/auth/cambiar-password`,
    method: "PATCH",
    successMessage: "Contraseña actualizada correctamente",
  });

  const [visibility, setVisibility] = useState({
    passwordActual: false,
    passwordNueva: false,
    confirmarPassword: false,
  });

  // Refuerza el bloqueo de doble envío: el estado isSubmitting de
  // useApiForm no se refleja en el botón hasta el siguiente render.
  const submittingRef = useRef(false);

  const clearSecrets = useCallback(() => {
    reset(EMPTY_VALUES);
  }, [reset]);

  const toggleVisibility = (field) =>
    setVisibility((prev) => ({ ...prev, [field]: !prev[field] }));

  const onSubmit = async (data) => {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setFeedback(null);

    try {
      const result = await submit({
        passwordActual: data.passwordActual,
        passwordNueva: data.passwordNueva,
      });

      if (result.success) {
        clearSecrets();
        setVisibility({
          passwordActual: false,
          passwordNueva: false,
          confirmarPassword: false,
        });
        setFeedback({
          type: "success",
          message: "La contraseña se cambió correctamente.",
        });
      } else {
        setFeedback({
          type: "error",
          message: withErrorReference(
            getApiErrorDescription(
              result.error,
              "No fue posible cambiar la contraseña.",
            ),
            result.correlationId,
          ),
        });
      }
    } finally {
      submittingRef.current = false;
    }
  };

  // El formulario permanece montado para que Contacto conserve su borrador.
  // Por eso los secretos se limpian explícitamente al abandonar Seguridad.
  useEffect(() => {
    if (isActive) return;

    clearSecrets();
    setVisibility({
      passwordActual: false,
      passwordNueva: false,
      confirmarPassword: false,
    });
    setFeedback(null);
  }, [clearSecrets, isActive]);

  useEffect(() => {
    return () => {
      clearSecrets();
    };
  }, [clearSecrets]);

  const nuevaPassword = watch("passwordNueva");

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="min-w-0 space-y-5"
      aria-busy={isSubmitting}
      noValidate
    >
      <h2 className="text-base font-semibold">Cambiar contraseña</h2>

      <PasswordField
        name="passwordActual"
        label="Contraseña actual"
        autoComplete="current-password"
        register={register}
        errors={errors}
        rules={{ required: "La contraseña actual es obligatoria" }}
        visible={visibility.passwordActual}
        onToggleVisibility={() => toggleVisibility("passwordActual")}
        disabled={isSubmitting}
        onValueChange={() => setFeedback(null)}
      />

      <div>
        <PasswordField
          name="passwordNueva"
          label="Nueva contraseña"
          autoComplete="new-password"
          register={register}
          errors={errors}
          rules={passwordPolicyRule()}
          visible={visibility.passwordNueva}
          onToggleVisibility={() => toggleVisibility("passwordNueva")}
          describedBy="password-policy-hints"
          disabled={isSubmitting}
          onValueChange={() => setFeedback(null)}
        />

        <ul
          id="password-policy-hints"
          className="mt-1.5 space-y-0.5 pl-1 text-xs text-muted-foreground"
        >
          {PASSWORD_POLICY_HINTS.map((hint) => (
            <li key={hint} className="flex items-center gap-1.5">
              <ShieldCheck className="h-3 w-3" aria-hidden="true" />
              {hint}
            </li>
          ))}
        </ul>
      </div>

      <PasswordField
        name="confirmarPassword"
        label="Confirmar nueva contraseña"
        autoComplete="new-password"
        register={register}
        errors={errors}
        rules={passwordsMatchRule(() => nuevaPassword)}
        visible={visibility.confirmarPassword}
        onToggleVisibility={() => toggleVisibility("confirmarPassword")}
        disabled={isSubmitting}
        onValueChange={() => setFeedback(null)}
      />

      {feedback && (
        <p
          role={feedback.type === "error" ? "alert" : "status"}
          className={`whitespace-pre-wrap break-words rounded-lg border px-3 py-2 text-sm ${
            feedback.type === "error"
              ? "border-destructive/30 bg-destructive/5 text-destructive"
              : "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
          }`}
        >
          {feedback.message}
        </p>
      )}

      <Button type="submit" disabled={isSubmitting} className="w-full sm:w-48">
        {isSubmitting ? "Guardando..." : "Cambiar contraseña"}
      </Button>
    </form>
  );
}

function PasswordField({
  name,
  label,
  register,
  errors,
  rules,
  visible,
  onToggleVisibility,
  describedBy,
  autoComplete,
  disabled,
  onValueChange,
}) {
  const error = errors?.[name];
  const errorId = error ? `${name}-error` : undefined;
  const ariaDescribedBy =
    [describedBy, errorId].filter(Boolean).join(" ") || undefined;
  const registration = register(name, rules);

  return (
    <div className="flex min-w-0 flex-col gap-1.5">
      <label htmlFor={name} className="text-sm font-medium leading-none">
        {label}
        <span className="ml-1 text-red-500" aria-hidden="true">
          *
        </span>
      </label>

      <div className="relative">
        <Input
          id={name}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          aria-invalid={error ? "true" : "false"}
          aria-required="true"
          aria-describedby={ariaDescribedBy}
          disabled={disabled}
          className={`pr-10 ${error ? "border-red-500" : ""}`.trim()}
          {...registration}
          onChange={(event) => {
            registration.onChange(event);
            onValueChange?.();
          }}
        />

        <button
          type="button"
          onClick={onToggleVisibility}
          disabled={disabled}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-md text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
          aria-label={
            visible
              ? `Ocultar ${label.toLowerCase()}`
              : `Mostrar ${label.toLowerCase()}`
          }
        >
          {visible ? (
            <EyeOff className="h-4 w-4" aria-hidden="true" />
          ) : (
            <Eye className="h-4 w-4" aria-hidden="true" />
          )}
        </button>
      </div>

      {error && (
        <p id={errorId} className="break-words text-xs text-red-500">
          {error.message}
        </p>
      )}
    </div>
  );
}
