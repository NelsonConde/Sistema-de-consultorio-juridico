"use client"

/**
 * Formulario de inicio de sesión del sistema jurídico.
 *
 * Al montarse verifica si ya existe sesión activa mediante `/api/auth/me`
 * y redirige a `/inicio` si el usuario ya está autenticado.
 *
 * Valida el formato del correo electrónico con `requiredEmailRule()` y
 * deshabilita el botón de envío mientras la petición está en curso.
 *
 * @module components/auth/LoginForm
 */

import React, { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { Scale } from "lucide-react"

import { FormInput } from "../forms/parts/FormInput"
import { Button } from "@/components/ui/button"
import { API_URL_BASE } from "@/lib/config"
import { apiClient } from "@/lib/apiClient"
import { getApiErrorTitle, readResponseBody } from "@/lib/api"
import { requiredEmailRule } from "@/lib/form-validation"

/**
 * Formulario de inicio de sesión.
 *
 * @returns {JSX.Element}
 */
export function LoginForm() {
  const router = useRouter()
  const [errorMessage, setErrorMessage] = useState("")
  const [checkingSession, setCheckingSession] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm()

  const REQUIRED = "Campo obligatorio"

  useEffect(() => {
    async function verificarSesion() {
      try {
        const res = await fetch(`${API_URL_BASE}/auth/me`, {
          method: "GET",
          credentials: "include",
        })

        if (res.ok) {
          router.push("/inicio")
          return
        }
      } catch {
        // Si no existe sesión o el backend no responde, se muestra el login.
      } finally {
        setCheckingSession(false)
      }
    }

    verificarSesion()
  }, [router])

  const handleSubmitForm = async (data) => {
    setErrorMessage("")
    setIsSubmitting(true)

    try {
      const response = await apiClient.post("/auth/login", {
        username: data.username,
        password: data.password,
      })

      const result = await readResponseBody(response)

      if (!response.ok) {
        throw new Error(
          getApiErrorTitle(result, "Usuario o contraseña incorrectos")
        )
      }

      router.push("/inicio")
    } catch (error) {
      setErrorMessage(error.message || "Error al iniciar sesión")
    } finally {
      setIsSubmitting(false)
    }
  }

  if (checkingSession) {
    return <div className="text-center mt-10">Cargando...</div>
  }

  return (
    <div className="w-full max-w-md">
      <div className="backdrop-blur-xl bg-card/80 border border-border rounded-2xl shadow-2xl p-8">
        <div className="mb-6 text-center space-y-2">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
            <Scale className="h-7 w-7 text-primary" />
          </div>

          <h1 className="text-2xl font-semibold">
            Consultorio Jurídico
          </h1>

          <p className="text-sm text-muted-foreground">
            Accede al sistema
          </p>
        </div>

        <form onSubmit={handleSubmit(handleSubmitForm)} className="space-y-4">
          <FormInput
            name="username"
            label="Correo electrónico"
            register={register}
            errors={errors}
            rules={requiredEmailRule()}
          />

          <FormInput
            name="password"
            label="Contraseña"
            type="password"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED }}
          />

          {errorMessage && (
            <div className="text-sm text-destructive">
              {errorMessage}
            </div>
          )}

          <Button
            type="submit"
            className="w-full"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Ingresando..." : "Acceder"}
          </Button>

          <div className="text-center mt-2">
            <button
              type="button"
              onClick={() => router.push("/recuperar-password")}
              className="text-sm text-primary hover:underline"
            >
              ¿Olvidaste tu contraseña?
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
