"use client"

import { useForm } from "react-hook-form"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { API_URL_BASE } from "@/lib/config"
import { getApiErrorTitle, readResponseBody } from "@/lib/api"
import { useRouter } from "next/navigation" 
import { useEffect } from "react"

/**
 * Formulario para restablecer la contraseña con token.
 * @param {{token:string}} props - Props del formulario.
 * @returns {JSX.Element} Formulario de restablecimiento.
 */
export function RestablecerPasswordForm({ token }) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm()

  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [loading, setLoading] = useState(false)
  const router = useRouter()

  /**
   * Envía la nueva contraseña al backend.
   * @param {Object} data - Datos del formulario.
   * @param {string} data.passwordNueva - Nueva contraseña.
   * @param {string} data.confirmarPassword - Confirmación de contraseña.
   * @returns {Promise<void>} Promesa de envío.
   */
  const onSubmit = async (data) => {
    setError("")
    setSuccess("")
    setLoading(true)

    try {
      const res = await fetch(`${API_URL_BASE}/auth/restablecer-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token,
          passwordNueva: data.passwordNueva,
          confirmarPassword: data.confirmarPassword,
        }),
      })

      const result = await readResponseBody(res)

      if (!res.ok) {
        throw new Error(getApiErrorTitle(result, "Error al restablecer contraseña"))
      }

      setSuccess("La contraseña se restableció correctamente")

    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => {
        router.push("/")
      }, 2000)

      return () => clearTimeout(timer)
    }
  }, [success, router])

  return (
    <div className="w-full max-w-md p-6 bg-card rounded-xl border shadow space-y-4">
      <h2 className="text-2xl font-bold text-center">
        Restablecer contraseña
      </h2>

      <input
        type="password"
        placeholder="Nueva contraseña"
        {...register("passwordNueva", {
          required: "La contraseña es obligatoria",
          minLength: {
            value: 8,
            message: "Mínimo 8 caracteres",
          },
        })}
        className="w-full border rounded-lg p-2"
      />
      {errors.passwordNueva && (
        <p className="text-sm text-red-500">
          {errors.passwordNueva.message}
        </p>
      )}

      <input
        type="password"
        placeholder="Confirmar contraseña"
        {...register("confirmarPassword", {
          required: "Debe confirmar la contraseña",
          validate: (value) =>
            value === watch("passwordNueva") || "Las contraseñas no coinciden",
        })}
        className="w-full border rounded-lg p-2"
      />
      {errors.confirmarPassword && (
        <p className="text-sm text-red-500">
          {errors.confirmarPassword.message}
        </p>
      )}

      {error && (
        <p className="text-sm text-red-500 text-center">
          {error}
        </p>
      )}

      {success && (
        <p className="text-sm text-green-500 text-center">
          {success}
        </p>
      )}


      <Button
        className="w-full"
        onClick={handleSubmit(onSubmit)}
        disabled={loading}
      >
        {loading ? "Guardando..." : "Cambiar contraseña"}
      </Button>
    </div>
  )
}