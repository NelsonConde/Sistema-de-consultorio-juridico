"use client"

import React, { useState } from "react"
import { useForm } from "react-hook-form"
import { useRouter } from "next/navigation"
import { FormInput } from "../forms/parts/FormInput"
import { Button } from "@/components/ui/button"
import { Scale } from "lucide-react"
import { API_URL_BASE } from "@/lib/config"
import { getApiErrorTitle, readResponseBody } from "@/lib/api"
import { requiredEmailRule } from "@/lib/form-validation"

/**
 * Formulario para solicitar recuperación de contraseña.
 * @returns {JSX.Element} Formulario de recuperación.
 */
export function RecuperarPasswordForm() {
  const router = useRouter()
  const [message, setMessage] = useState("")
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm()

  const REQUIRED = "Campo obligatorio"

  /**
   * Envía la solicitud de recuperación al backend.
   * @param {Object} data - Datos del formulario.
   * @param {string} data.correo - Correo electrónico del usuario.
   * @returns {Promise<void>} Promesa de envío.
   */
  const onSubmit = async (data) => {
    setError("")
    setMessage("")
    setLoading(true)

    try {
      const res = await fetch(`${API_URL_BASE}/auth/solicitar-recuperacion`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username: data.correo
        })
      })

      const result = await readResponseBody(res)

      if (!res.ok) {
        throw new Error(getApiErrorTitle(result, "Error al enviar solicitud"))
      }

      setMessage(result?.mensaje || "Revisa tu correo")

      // redirige al login después de unos segundos
      setTimeout(() => {
        router.push("/")
      }, 2500)

    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="w-full max-w-md">
      <div className="backdrop-blur-xl bg-card/80 border border-border rounded-2xl shadow-2xl p-8">

        <div className="mb-6 text-center space-y-2">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
            <Scale className="h-7 w-7 text-primary" />
          </div>

          <h1 className="text-2xl font-semibold">
            Recuperar contraseña
          </h1>

          <p className="text-sm text-muted-foreground">
            Ingresa tu correo para recibir instrucciones
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">

          <FormInput
            name="correo"
            label="Correo electrónico"
            register={register}
            errors={errors}
            rules={requiredEmailRule()}
          />

          {error && (
            <div className="text-sm text-destructive">
              {error}
            </div>
          )}

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? "Enviando..." : "Enviar instrucciones"}
          </Button>

          <Button
            type="button"
            variant="outline"
            className="w-full"
            onClick={() => router.push("/")}
          >
            Volver al login
          </Button>

        </form>
      </div>
    </div>
  )
}