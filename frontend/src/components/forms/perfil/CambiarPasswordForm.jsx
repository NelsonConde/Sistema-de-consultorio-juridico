"use client"

import { useEffect, useRef, useState } from "react"
import { useForm } from "react-hook-form"
import { Eye, EyeOff, ShieldCheck } from "lucide-react"

import { Button } from "@/components/ui/button"
import { FormInput } from "@/components/forms/parts/FormInput"
import { useApiForm } from "@/hooks/useApiForm"
import { API_URL_BASE } from "@/lib/config"
import { passwordPolicyRule, passwordsMatchRule } from "@/lib/form-validation"

const PASSWORD_POLICY_HINTS = [
  "Mínimo 8 caracteres",
  "Al menos una letra mayúscula",
  "Al menos un número",
]

const EMPTY_VALUES = {
  passwordActual: "",
  passwordNueva: "",
  confirmarPassword: "",
}

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
export function CambiarPasswordForm() {
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    mode: "onBlur",
    defaultValues: EMPTY_VALUES,
  })

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/auth/cambiar-password`,
    method: "PATCH",
    successMessage: "Contraseña actualizada correctamente",
  })

  const [visibility, setVisibility] = useState({
    passwordActual: false,
    passwordNueva: false,
    confirmarPassword: false,
  })

  // Refuerza el bloqueo de doble envío: el estado isSubmitting de
  // useApiForm no se refleja en el botón hasta el siguiente render.
  const submittingRef = useRef(false)

  const toggleVisibility = (field) =>
    setVisibility((prev) => ({ ...prev, [field]: !prev[field] }))

  const onSubmit = async (data) => {
    if (submittingRef.current) return
    submittingRef.current = true

    try {
      const result = await submit({
        passwordActual: data.passwordActual,
        passwordNueva: data.passwordNueva,
      })

      if (result.success) {
        reset(EMPTY_VALUES)
      }
    } finally {
      submittingRef.current = false
    }
  }

  // Limpia cualquier secreto en memoria si el componente se desmonta
  // (navegación fuera de Mi Perfil) sin haber enviado el formulario.
  useEffect(() => {
    return () => {
      reset(EMPTY_VALUES)
    }
  }, [reset])

  const nuevaPassword = watch("passwordNueva")

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-4"
      aria-busy={isSubmitting}
      noValidate
    >
      <PasswordField
        name="passwordActual"
        label="Contraseña actual"
        autoComplete="current-password"
        register={register}
        errors={errors}
        rules={{ required: "La contraseña actual es obligatoria" }}
        visible={visibility.passwordActual}
        onToggleVisibility={() => toggleVisibility("passwordActual")}
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
      />

      <Button type="submit" disabled={isSubmitting} className="w-full">
        {isSubmitting ? "Guardando..." : "Cambiar contraseña"}
      </Button>
    </form>
  )
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
}) {
  return (
    <div className="relative">
      <FormInput
        name={name}
        label={label}
        type={visible ? "text" : "password"}
        register={register}
        errors={errors}
        rules={rules}
        autoComplete={autoComplete}
        aria-describedby={describedBy}
        className="pr-10"
      />

      <button
        type="button"
        onClick={onToggleVisibility}
        className="absolute right-2 top-8 text-muted-foreground hover:text-foreground"
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
  )
}