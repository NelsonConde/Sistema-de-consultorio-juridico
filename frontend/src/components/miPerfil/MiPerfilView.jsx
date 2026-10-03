"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { toast } from "sonner"

import { apiResponse } from "@/lib/api"
import { API_URL_BASE } from "@/lib/config"
import { Skeleton } from "@/components/ui/skeleton"
import { ActualizarContactoForm } from "./ActualizarContactoForm"
import { CambiarPasswordForm } from "@/components/forms/perfil/CambiarPasswordForm"

// Campos de solo lectura mostrados tal cual llegan de GET /api/mi-perfil.
// email y telefono NO aparecen aquí: son editables y viven en
// ActualizarContactoForm, no en este bloque de identidad.
const READONLY_FIELDS = [
  { key: "nombre", label: "Nombre" },
  { key: "username", label: "Usuario" },
  { key: "rolNombre", label: "Rol" },
  { key: "tipoPerfil", label: "Perfil" },
  { key: "sede", label: "Sede" },
  { key: "codigo", label: "Código" },
]

export function MiPerfilView() {
  const router = useRouter()
  const [perfil, setPerfil] = useState(null)
  const [status, setStatus] = useState("loading") // loading | ready | error

  const cargarPerfil = useCallback(async () => {
    setStatus("loading")

    try {
      const { response, data } = await apiResponse(`${API_URL_BASE}/mi-perfil`, {
        method: "GET",
      })

      if (response.status === 401) {
        toast.error("Sesión expirada", {
          description: "Debe iniciar sesión nuevamente",
        })
        router.replace("/")
        return
      }

      if (!response.ok) {
        setStatus("error")
        return
      }

      setPerfil(data)
      setStatus("ready")
    } catch {
      setStatus("error")
    }
  }, [router])

  useEffect(() => {
    cargarPerfil()
  }, [cargarPerfil])

  if (status === "loading") {
    return (
      <div className="max-w-2xl space-y-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-56 w-full" />
      </div>
    )
  }

  if (status === "error" || !perfil) {
    return (
      <div className="max-w-2xl rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-sm">
        <p className="font-medium text-destructive">
          No fue posible cargar tu perfil
        </p>
        <p className="mt-1 text-muted-foreground">
          Verifica tu conexión o que tu perfil siga activo, e intenta de nuevo.
        </p>
        <button
          type="button"
          onClick={cargarPerfil}
          className="mt-3 text-sm font-medium text-primary underline underline-offset-4"
        >
          Reintentar
        </button>
      </div>
    )
  }

  return (
    <div className="max-w-2xl space-y-8">
      <section className="rounded-xl border bg-card p-6">
        <h2 className="mb-4 text-lg font-semibold">Identidad</h2>
        <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {READONLY_FIELDS.map(({ key, label }) => (
            <div key={key}>
              <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                {label}
              </dt>
              <dd className="text-sm font-medium">{perfil[key] || "—"}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="rounded-xl border bg-card p-6">
        <h2 className="mb-4 text-lg font-semibold">Datos de contacto</h2>
        <ActualizarContactoForm perfil={perfil} onUpdated={setPerfil} />
      </section>

      <section className="rounded-xl border bg-card p-6">
        <h2 className="mb-4 text-lg font-semibold">Cambiar contraseña</h2>
        <CambiarPasswordForm />
      </section>
    </div>
  )
}