"use client"

import { useEffect, useRef } from "react"
import { useForm } from "react-hook-form"

import { Button } from "@/components/ui/button"
import { FormInput } from "@/components/forms/parts/FormInput"
import { useApiForm } from "@/hooks/useApiForm"
import { API_URL_BASE } from "@/lib/config"
import { requiredEmailRule } from "@/lib/form-validation"

/**
 * Formulario de contacto (correo/teléfono) de Mi Perfil.
 *
 * Solo expone los dos campos editables — nunca nombre, rol, sede o código,
 * que son de solo lectura y viven en MiPerfilView.
 *
 * @param {{ perfil: object, onUpdated: (perfil: object) => void }} props
 */
export function ActualizarContactoForm({ perfil, onUpdated }) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      email: perfil?.email || "",
      telefono: perfil?.telefono || "",
    },
  })

  // Si el perfil llega/cambia después del primer render (por ejemplo, tras
  // el fetch inicial en MiPerfilView), sincroniza los valores por defecto.
  useEffect(() => {
    reset({
      email: perfil?.email || "",
      telefono: perfil?.telefono || "",
    })
  }, [perfil, reset])

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/mi-perfil/contacto`,
    method: "PATCH",
    successMessage: "Datos de contacto actualizados",
  })

  const submittingRef = useRef(false)

  const onSubmit = async (data) => {
    if (submittingRef.current) return
    submittingRef.current = true

    try {
      const result = await submit({
        email: data.email,
        telefono: data.telefono,
      })

      if (result.success && result.data) {
        onUpdated?.(result.data)
      }
    } finally {
      submittingRef.current = false
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" aria-busy={isSubmitting}>
      <FormInput
        name="email"
        label="Correo electrónico"
        type="email"
        register={register}
        errors={errors}
        rules={requiredEmailRule()}
        autoComplete="email"
      />

      <FormInput
        name="telefono"
        label="Teléfono"
        type="tel"
        register={register}
        errors={errors}
        rules={{
          required: "El teléfono es obligatorio",
          pattern: {
            value: /^[0-9+\-\s]{7,30}$/,
            message: "Ingresa un teléfono válido",
          },
        }}
        autoComplete="tel"
      />

      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Guardando..." : "Guardar cambios"}
      </Button>
    </form>
  )
}